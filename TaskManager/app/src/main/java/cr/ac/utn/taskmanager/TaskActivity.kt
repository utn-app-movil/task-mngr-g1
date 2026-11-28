
package cr.ac.utn.taskmanager

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskPriority
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import cr.ac.utn.taskmanager.util.EXTRA_MESSAGE_ID
import cr.ac.utn.taskmanager.util.util
import cr.ac.utn.taskmanager.viewmodel.TaskViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TaskActivity : AppCompatActivity(),
    DatePickerDialog.OnDateSetListener {

    private lateinit var txtId: EditText
    private lateinit var txtTitle: EditText
    private lateinit var txtDescription: EditText
    private lateinit var spStatus: Spinner
    private lateinit var spPriority: Spinner
    private lateinit var spAssignTo: Spinner
    private lateinit var spParentTask: Spinner
    private lateinit var txtObservation: EditText
    private lateinit var tvDueDate: TextView
    private lateinit var txtCancellationReason: EditText
    private lateinit var lblCancellationReason: TextView
    private lateinit var tvStatusChangeDate: TextView
    private lateinit var btnDueDate: ImageButton
    private lateinit var btnAddPerson: ImageButton

    private var selectedDate = LocalDate.now()
    private var isEditMode = false
    private var currentTask: Task? = null

    private var eligibleParents: List<Task> = emptyList()
    private var parentOptions: List<Task> = emptyList()
    private val people = mutableListOf<String>()

    private val dateFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private val dateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    private val taskViewModel: TaskViewModel by viewModels {
        TaskViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_task)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->

            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        initializeViews()
        loadPeople()
        setupSpinners()
        cleanScreen()
        observeEligibleParents()

        val taskId = intent.getStringExtra(EXTRA_MESSAGE_ID)

        if (!taskId.isNullOrBlank()) {
            searchTask(taskId)
        }

        findViewById<ImageButton>(
            R.id.btnSearch_Task
        ).setOnClickListener {
            searchTask(txtId.text.toString().trim())
        }

        btnDueDate.setOnClickListener {
            showDatePickerDialog()
        }

        btnAddPerson.setOnClickListener {
            showAddPersonDialog()
        }
    }

    private fun initializeViews() {

        txtId = findViewById(R.id.txtId_Task)
        txtTitle = findViewById(R.id.txtTitle_Task)
        txtDescription = findViewById(R.id.txtDescription_Task)
        spStatus = findViewById(R.id.spStatus_Task)
        spPriority = findViewById(R.id.spPriority_Task)
        spAssignTo = findViewById(R.id.spAssignTo_Task)
        spParentTask = findViewById(R.id.spParentTask_Task)
        txtObservation = findViewById(R.id.txtObservation_Task)
        tvDueDate = findViewById(R.id.tvDueDate_Task)

        txtCancellationReason =
            findViewById(R.id.txtCancellationReason_Task)

        lblCancellationReason =
            findViewById(R.id.lblCancellationReason_Task)

        tvStatusChangeDate =
            findViewById(R.id.tvStatusChangeDate_Task)

        btnDueDate = findViewById(R.id.btnDueDate_Task)

        btnAddPerson = findViewById(R.id.btnAddPerson_Task)
    }

    private fun setupSpinners() {

        val priorityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TaskPriority.entries.toTypedArray()
        )

        priorityAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spPriority.adapter = priorityAdapter

        spStatus.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    updateCancellationVisibility()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) = Unit
            }
    }

    // Assigned people

    private fun loadPeople() {

        val preferences = getSharedPreferences(
            PEOPLE_PREFERENCES,
            MODE_PRIVATE
        )

        val savedPeople = preferences.getStringSet(
            PEOPLE_KEY,
            emptySet()
        ).orEmpty()

        people.clear()
        people.addAll(savedPeople.sorted())
    }

    private fun savePeople() {

        getSharedPreferences(
            PEOPLE_PREFERENCES,
            MODE_PRIVATE
        ).edit()
            .putStringSet(PEOPLE_KEY, people.toSet())
            .apply()
    }

    private fun setupPeopleSpinner(
        selectedPerson: String? = null
    ) {

        if (
            !selectedPerson.isNullOrBlank() &&
            selectedPerson !in people
        ) {
            people.add(selectedPerson)
            savePeople()
        }

        val options = mutableListOf(
            getString(R.string.TextSelectPerson)
        )

        options.addAll(people)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            options
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spAssignTo.adapter = adapter

        val position = options.indexOf(selectedPerson)

        spAssignTo.setSelection(
            position.coerceAtLeast(0)
        )
    }

    private fun showAddPersonDialog() {

        val input = EditText(this)

        input.hint = getString(R.string.TextPersonName)
        input.isSingleLine = true

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.TextAddPerson)
            .setView(input)
            .setPositiveButton(R.string.TextSave, null)
            .setNegativeButton(R.string.TextCancel, null)
            .create()

        dialog.setOnShowListener {

            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val name = input.text.toString().trim()

                if (name.isBlank()) {

                    input.error = getString(
                        R.string.TextIncompleteData
                    )

                } else {

                    val existingPerson = people.firstOrNull {
                        it.equals(name, ignoreCase = true)
                    }

                    val selectedPerson = existingPerson ?: name

                    if (existingPerson == null) {
                        people.add(name)
                        savePeople()
                    }

                    setupPeopleSpinner(selectedPerson)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    // Task status

    private fun setupStatusSpinner(status: TaskStatus) {

        val allowedStatuses = if (!isEditMode) {

            listOf(TaskStatus.PENDING)

        } else {

            when (status) {

                TaskStatus.PENDING -> listOf(
                    TaskStatus.PENDING,
                    TaskStatus.IN_PROGRESS,
                    TaskStatus.CANCELLED
                )

                TaskStatus.IN_PROGRESS -> listOf(
                    TaskStatus.IN_PROGRESS,
                    TaskStatus.COMPLETED,
                    TaskStatus.CANCELLED
                )

                TaskStatus.COMPLETED -> listOf(
                    TaskStatus.COMPLETED
                )

                TaskStatus.CANCELLED -> listOf(
                    TaskStatus.CANCELLED
                )
            }
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            allowedStatuses
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spStatus.adapter = adapter

        spStatus.setSelection(
            allowedStatuses.indexOf(status).coerceAtLeast(0)
        )

        spStatus.isEnabled =
            isEditMode && !isFinalStatus()

        updateCancellationVisibility()
    }

    private fun isFinalStatus(): Boolean {

        return currentTask?.status == TaskStatus.COMPLETED ||
                currentTask?.status == TaskStatus.CANCELLED
    }

    private fun updateCancellationVisibility() {

        val visibility =
            if (spStatus.selectedItem == TaskStatus.CANCELLED) {
                View.VISIBLE
            } else {
                View.GONE
            }

        lblCancellationReason.visibility = visibility
        txtCancellationReason.visibility = visibility
    }

    // Parent tasks

    private fun observeEligibleParents() {

        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {

                taskViewModel.eligibleParentTasks.collect { parents ->

                    val selectedParentId = getSelectedParentId()

                    eligibleParents = parents

                    updateParentSpinner(selectedParentId)
                }
            }
        }
    }

    private fun updateParentSpinner(parentId: String?) {

        parentOptions = eligibleParents.filter {
            it.id != currentTask?.id
        }

        val options = mutableListOf(
            getString(R.string.TextNoParentTask)
        )

        options.addAll(
            parentOptions.map {
                "${it.id} - ${it.title}"
            }
        )

        var selectedPosition = parentOptions.indexOfFirst {
            it.id == parentId
        } + 1

        if (
            isEditMode &&
            parentId != null &&
            selectedPosition == 0
        ) {
            options.add(parentId)
            selectedPosition = options.lastIndex
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            options
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spParentTask.adapter = adapter
        spParentTask.setSelection(selectedPosition)
        spParentTask.isEnabled = !isEditMode
    }

    private fun getSelectedParentId(): String? {

        if (isEditMode) {
            return currentTask?.parentTaskId
        }

        return parentOptions.getOrNull(
            spParentTask.selectedItemPosition - 1
        )?.id
    }

    // Due date

    override fun onDateSet(
        view: android.widget.DatePicker?,
        year: Int,
        month: Int,
        dayOfMonth: Int
    ) {

        selectedDate = LocalDate.of(
            year,
            month + 1,
            dayOfMonth
        )

        tvDueDate.text = selectedDate.format(dateFormatter)
    }

    private fun showDatePickerDialog() {

        DatePickerDialog(
            this,
            this,
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        ).show()
    }

    // Menu

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.crud_actions, menu)

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {

        menu?.findItem(R.id.mnuSave)?.isVisible =
            !isFinalStatus()

        menu?.findItem(R.id.mnuDelete)?.isVisible =
            isEditMode && !isFinalStatus()

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(
        item: MenuItem
    ): Boolean {

        return when (item.itemId) {

            R.id.mnuSave -> {

                if (isEditMode) {

                    util.showDialogCondition(
                        this,
                        getString(R.string.TextSaveQuestion)
                    ) {
                        saveTask()
                    }

                } else {
                    saveTask()
                }

                true
            }

            R.id.mnuDelete -> {

                util.showDialogCondition(
                    this,
                    getString(R.string.TextDeleteQuestion)
                ) {
                    deleteTask()
                }

                true
            }

            R.id.mnuCancel -> {

                cleanScreen()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // Search task

    private fun searchTask(id: String) {

        if (id.isBlank()) return

        val progressDialog = util.showProgressDialog(
            this,
            getString(R.string.TextLoading)
        )

        lifecycleScope.launch {

            try {

                val task = taskViewModel.getTaskById(id)

                if (task == null) {

                    showMessage(R.string.TextTaskNotFound)
                    return@launch
                }

                currentTask = task
                isEditMode = true

                txtId.setText(task.id)
                txtId.isEnabled = false

                txtTitle.setText(task.title)
                txtDescription.setText(task.description)
                txtObservation.setText(task.notes)

                spPriority.setSelection(
                    task.priority.ordinal
                )

                setupPeopleSpinner(task.assignedTo)
                setupStatusSpinner(task.status)
                updateParentSpinner(task.parentTaskId)

                txtCancellationReason.setText(
                    task.cancellationReason.orEmpty()
                )

                selectedDate = task.dueDate

                tvDueDate.text =
                    selectedDate.format(dateFormatter)

                tvStatusChangeDate.text =
                    task.statusChangeDate?.let {

                        getString(
                            R.string.TextStatusChangeDate,
                            it.format(dateTimeFormatter)
                        )

                    }.orEmpty()

                setFieldsEnabled(!isFinalStatus())

                updateCancellationVisibility()
                invalidateOptionsMenu()

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                showMessage(R.string.TextTaskOperationError)

            } finally {

                progressDialog.dismiss()
            }
        }
    }

    private fun setFieldsEnabled(enabled: Boolean) {

        txtTitle.isEnabled = enabled
        txtDescription.isEnabled = enabled
        txtObservation.isEnabled = enabled
        spPriority.isEnabled = enabled
        spAssignTo.isEnabled = enabled
        btnAddPerson.isEnabled = enabled
        btnDueDate.isEnabled = enabled
        txtCancellationReason.isEnabled = enabled

        spStatus.isEnabled = enabled && isEditMode

        spParentTask.isEnabled = enabled && !isEditMode
    }

    private fun cleanScreen() {

        currentTask = null
        isEditMode = false

        txtId.isEnabled = true
        txtId.setText("")
        txtId.hint = getString(R.string.TextAutomaticId)

        txtTitle.setText("")
        txtDescription.setText("")
        txtObservation.setText("")
        txtCancellationReason.setText("")
        tvStatusChangeDate.text = ""

        selectedDate = LocalDate.now()

        tvDueDate.text =
            selectedDate.format(dateFormatter)

        spPriority.setSelection(0)

        setupPeopleSpinner()
        setupStatusSpinner(TaskStatus.PENDING)
        updateParentSpinner(null)

        setFieldsEnabled(true)

        updateCancellationVisibility()
        invalidateOptionsMenu()
    }

    // Save task

    private fun isValidationData(): Boolean {

        return txtTitle.text.toString().trim().isNotBlank() &&
                txtDescription.text.toString().trim().isNotBlank() &&
                spStatus.selectedItem != null &&
                spPriority.selectedItem != null
    }

    private fun saveTask() {

        if (isFinalStatus()) {

            showMessage(R.string.TextTaskCannotBeModified)
            return
        }

        if (!isValidationData()) {

            showMessage(R.string.TextIncompleteData)
            return
        }

        if (spAssignTo.selectedItemPosition <= 0) {

            showMessage(R.string.TextAssignedPersonRequired)
            return
        }

        val selectedStatus =
            spStatus.selectedItem as TaskStatus

        val selectedPriority =
            spPriority.selectedItem as TaskPriority

        val cancellationReason =
            if (selectedStatus == TaskStatus.CANCELLED) {

                txtCancellationReason.text.toString().trim()

            } else {
                null
            }

        val task = Task(

            id = currentTask?.id.orEmpty(),

            title = txtTitle.text.toString().trim(),

            description =
                txtDescription.text.toString().trim(),

            dueDate = selectedDate,

            creationDate =
                currentTask?.creationDate ?: LocalDate.now(),

            status = selectedStatus,

            assignedTo =
                spAssignTo.selectedItem.toString(),

            priority = selectedPriority,

            notes = txtObservation.text.toString().trim(),

            parentTaskId = getSelectedParentId(),

            cancellationReason = cancellationReason,

            statusChangeDate = currentTask?.statusChangeDate
        )

        val progressDialog = util.showProgressDialog(
            this,
            getString(R.string.TextSaving)
        )

        val onComplete: () -> Unit = {

            progressDialog.dismiss()
            cleanScreen()
            showMessage(R.string.TextSaveSuccess)
        }

        val onError: (Int) -> Unit = { message ->

            progressDialog.dismiss()
            showMessage(message)
        }

        if (isEditMode) {

            taskViewModel.updateTask(
                task,
                onComplete,
                onError
            )

        } else {

            taskViewModel.saveTask(
                task,
                onComplete,
                onError
            )
        }
    }

    // Delete task

    private fun deleteTask() {

        val id = currentTask?.id ?: return

        if (isFinalStatus()) return

        val progressDialog = util.showProgressDialog(
            this,
            getString(R.string.TextDeleting)
        )

        taskViewModel.removeTask(id) {

            progressDialog.dismiss()
            cleanScreen()
            showMessage(R.string.TextDeleteSuccess)
        }
    }

    private fun showMessage(message: Int) {

        Toast.makeText(
            this,
            getString(message),
            Toast.LENGTH_LONG
        ).show()
    }

    companion object {

        private const val PEOPLE_PREFERENCES =
            "task_people_preferences"

        private const val PEOPLE_KEY =
            "task_people"
    }
}
