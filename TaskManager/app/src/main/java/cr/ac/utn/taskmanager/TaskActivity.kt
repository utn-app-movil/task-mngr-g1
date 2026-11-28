package cr.ac.utn.taskmanager

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AdapterView
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.util.Calendar

class TaskActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener {

    private lateinit var txtId: EditText
    private lateinit var txtTitle: EditText
    private lateinit var txtDescription: EditText
    private lateinit var spStatus: Spinner
    private lateinit var spPriority: Spinner
    private lateinit var spAssignTo: Spinner
    private lateinit var spParentTask: Spinner
    private lateinit var txtObservation: EditText
    private lateinit var tvDueDate: TextView
    private var dueDay: Int = 0
    private var dueMonth: Int = 0
    private var dueYear: Int = 0
    private var isEditMode: Boolean = false
    private var eligibleParents: List<Task> = emptyList()
    private var currentTask: Task? = null

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

        txtId = findViewById(R.id.txtId_Task)
        txtTitle = findViewById(R.id.txtTitle_Task)
        txtDescription = findViewById(R.id.txtDescription_Task)
        spStatus = findViewById(R.id.spStatus_Task)
        spPriority = findViewById(R.id.spPriority_Task)
        txtObservation = findViewById(R.id.txtObservation_Task)
        spAssignTo = findViewById(R.id.spAssignTo_Task)
        spParentTask = findViewById(R.id.spParentTask_Task)
        tvDueDate = findViewById(R.id.tvDueDate_Task)

        setupSpinners()
        observeEligibleParents()

        val btnSearch = findViewById<ImageButton>(R.id.btnSearch_Task)
        val btnDueDate = findViewById<ImageButton>(R.id.btnDueDate_Task)

        resetDate()
        val taskId = intent.getStringExtra(EXTRA_MESSAGE_ID)
        if (!taskId.isNullOrBlank()) {
            txtId.setText(taskId)
            searchTask(taskId)
        }

        btnSearch.setOnClickListener {
            searchTask(txtId.text.toString().trim())
        }

        btnDueDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    override fun onDateSet(
        view: android.widget.DatePicker?,
        year: Int,
        month: Int,
        dayOfMonth: Int
    ) {
        dueDay = dayOfMonth
        dueMonth = month + 1
        dueYear = year

        tvDueDate.text = getDateFormatString(
            dueDay, dueMonth, dueYear
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.crud_actions, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val editable = currentTask?.status?.isTerminal() != true
        menu?.findItem(R.id.mnuSave)?.isVisible = editable
        menu?.findItem(R.id.mnuDelete)?.isVisible = isEditMode && editable
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mnuSave -> {
                if (isEditMode) {
                    util.showDialogCondition(
                        this,
                        getString(R.string.TextSaveQuestion)
                    ) { saveTask() }
                } else {
                    saveTask()
                }
                true
            }
            R.id.mnuDelete -> {
                util.showDialogCondition(
                    this,
                    getString(R.string.TextDeleteQuestion)
                ) { deleteTask() }
                true
            }
            R.id.mnuCancel -> {
                cleanScreen()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupStatusSpinner() {
        val status = currentTask?.status
        val options = status?.allowedStatuses() ?: listOf(TaskStatus.PENDING)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spStatus.adapter = adapter
        spStatus.setSelection(0)
        spStatus.isEnabled = options.size > 1
        spStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateCancellationField()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                updateCancellationField()
            }
        }
        updateCancellationField()
    }

    private fun updateCancellationField() {
        val cancelled = spStatus.selectedItem == TaskStatus.CANCELLED
        val field = findViewById<EditText>(R.id.txtCancellationReason_Task)
        field.visibility = if (cancelled) View.VISIBLE else View.GONE
        findViewById<View>(R.id.lblCancellationReason_Task).visibility = field.visibility
        field.isEnabled = cancelled && currentTask?.status?.isTerminal() != true
    }

    private fun updateEditability() {
        val editable = currentTask?.status?.isTerminal() != true
        txtTitle.isEnabled = editable
        txtDescription.isEnabled = editable
        spPriority.isEnabled = editable
        spAssignTo.isEnabled = editable
        txtObservation.isEnabled = editable
        findViewById<ImageButton>(R.id.btnDueDate_Task).isEnabled = editable
        findViewById<EditText>(R.id.txtCancellationReason_Task).isEnabled = editable
        setupStatusSpinner()
    }
    private fun setupSpinners() {
        setupStatusSpinner()

        val priorityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TaskPriority.entries.toTypedArray()
        )

        priorityAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spPriority.adapter = priorityAdapter

        val peopleAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            util.loadPeopleFromAssets(this)
        )

        peopleAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spAssignTo.adapter = peopleAdapter
        updateParentSpinnerSelection()
    }

    private fun observeEligibleParents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.eligibleParentTasks.collect { parents ->
                    val selectedParentId = eligibleParents
                        .getOrNull(spParentTask.selectedItemPosition - 1)
                        ?.id
                    eligibleParents = parents
                    updateParentSpinnerSelection(selectedParentId)
                }
            }
        }
    }

    private fun updateParentSpinnerSelection(
        selectedParentId: Long? = null
    ) {
        val parentOptions = mutableListOf(
            "Ninguna (Tarea Principal)"
        )

        if (isEditMode) {
            val parentId = currentTask?.parentTaskId

            if (parentId != null) {
                val parent = eligibleParents.find {
                    it.id == parentId
                }
                parentOptions.add(
                    parent?.let { "${it.id} - ${it.title}" }
                        ?: "Tarea padre: $parentId"
                )
            }
        } else {
            eligibleParents.forEach { parent ->
                parentOptions.add("${parent.id} - ${parent.title}")
            }
        }

        val parentAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            parentOptions
        )

        parentAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spParentTask.adapter = parentAdapter
        spParentTask.isEnabled = !isEditMode

        if (isEditMode) {
            spParentTask.setSelection(
                if (currentTask?.parentTaskId != null) 1 else 0
            )
        } else {
            val position = eligibleParents.indexOfFirst {
                it.id == selectedParentId
            }
            spParentTask.setSelection(position + 1)
        }
    }

    private fun resetDate() {
        val calendar = Calendar.getInstance()
        dueYear = calendar.get(Calendar.YEAR)
        dueMonth = calendar.get(Calendar.MONTH) + 1
        dueDay = calendar.get(Calendar.DAY_OF_MONTH)
    }

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            this,
            this,
            dueYear,
            dueMonth - 1,
            dueDay
        )

        datePickerDialog.show()
    }

    private fun searchTask(id: String) {
        val taskId = id.trim().toLongOrNull() ?: return

        val progressDialog = util.showProgressDialog(
            this,
            getString(R.string.TextLoading)
        )

        lifecycleScope.launch {
            try {
                val task = taskViewModel.getTaskById(taskId)

                if (task != null) {
                    currentTask = task
                    isEditMode = true

                    txtId.setText(task.id.toString())
                    txtId.isEnabled = false
                    txtTitle.setText(task.title)
                    txtDescription.setText(task.description)
                    updateEditability()
                    spPriority.setSelection(task.priority.ordinal)

                    val personPosition = util
                        .loadPeopleFromAssets(this@TaskActivity)
                        .indexOf(task.assignedTo)

                    spAssignTo.setSelection(personPosition)

                    txtObservation.setText(task.notes)
                    findViewById<EditText>(R.id.txtCancellationReason_Task).setText(task.cancellationReason.orEmpty())

                    dueDay = task.dueDate.dayOfMonth
                    dueMonth = task.dueDate.monthValue
                    dueYear = task.dueDate.year
                    tvDueDate.text = getDateFormatString(
                        dueDay,
                        dueMonth,
                        dueYear
                    )
                    updateParentSpinnerSelection()
                    invalidateOptionsMenu()
                } else {
                    Toast.makeText(
                        this@TaskActivity,
                        getString(R.string.TextDataNoFound),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: CancellationException) {
                progressDialog.dismiss()
                throw e
            } catch (e: Exception) {
                cleanScreen()
                Toast.makeText(
                    this@TaskActivity,
                    e.message.toString(),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressDialog.dismiss()
            }
        }
    }

    private fun getDateFormatString(
        dayOfMonth: Int,
        monthValue: Int,
        yearValue: Int
    ): String {
        return "${if (dayOfMonth < 10) "0" else ""}$dayOfMonth/" +
                "${if (monthValue < 10) "0" else ""}$monthValue/$yearValue"
    }

    private fun cleanScreen() {
        resetDate()
        currentTask = null
        isEditMode = false
        txtId.isEnabled = true
        txtId.setText("")
        txtTitle.setText("")
        txtDescription.setText("")

        updateEditability()
        spPriority.setSelection(0)
        spAssignTo.setSelection(0)
        txtObservation.setText("")
        findViewById<EditText>(R.id.txtCancellationReason_Task).setText("")
        tvDueDate.text = ""
        updateParentSpinnerSelection()
        invalidateOptionsMenu()
    }

    private fun isValidationData(): Boolean {
        return txtTitle.text.toString().trim().isNotBlank()
                && txtDescription.text.toString().trim().isNotBlank()
                && spStatus.selectedItem != null
                && spPriority.selectedItem != null
                && tvDueDate.text.toString().trim().isNotBlank()
                && txtObservation.text.toString().trim().isNotBlank()
                && spAssignTo.selectedItem != null
    }

    private fun saveTask() {
        if (!isValidationData()) {
            Toast.makeText(
                this@TaskActivity,
                "Datos incompletos",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val progressDialog = util.showProgressDialog(
            this,
            getString(R.string.TextSaving)
        )

        lifecycleScope.launch {
            try {
                val existingTask = if (isEditMode) {
                    val id = currentTask?.id
                        ?: error("Tarea no encontrada")

                    taskViewModel.getTaskById(id)
                        ?: error("Tarea no encontrada")
                } else {
                    null
                }

                val taskId = existingTask?.id ?: 0L

                val parentTaskId = if (isEditMode) {
                    existingTask?.parentTaskId
                } else {
                    eligibleParents.getOrNull(
                        spParentTask.selectedItemPosition - 1
                    )?.id
                }

                if (!isEditMode && parentTaskId != null) {
                    val parent = taskViewModel.getTaskById(parentTaskId)

                    if (parent == null ||
                        (parent.status != TaskStatus.PENDING &&
                                parent.status != TaskStatus.IN_PROGRESS)
                    ) {
                        error("La tarea padre no esta disponible")
                    }
                }

                val selectedStatus =
                    spStatus.selectedItem as TaskStatus

                val selectedPriority =
                    spPriority.selectedItem as TaskPriority

                val task = Task(
                    id = taskId,
                    title = txtTitle.text.toString().trim(),
                    description = txtDescription.text.toString().trim(),
                    dueDate = LocalDate.of(dueYear, dueMonth, dueDay),
                    creationDate = existingTask?.creationDate
                        ?: LocalDate.now(),
                    status = selectedStatus,
                    assignedTo = spAssignTo.selectedItem.toString(),
                    priority = selectedPriority,
                    notes = txtObservation.text.toString().trim(),
                    parentTaskId = parentTaskId,
                    cancellationReason = findViewById<EditText>(R.id.txtCancellationReason_Task).text.toString().trim()
                )

                val onCompleteCallback = {
                    progressDialog.dismiss()
                    cleanScreen()
                    Toast.makeText(
                        this@TaskActivity,
                        getString(R.string.TextSaveSuccess),
                        Toast.LENGTH_LONG
                    ).show()
                }

                if (!isEditMode) {
                    taskViewModel.saveTask(task)
                } else {
                    taskViewModel.updateTask(task)
                }
                onCompleteCallback()

            } catch (e: CancellationException) {
                progressDialog.dismiss()
                throw e
            } catch (e: Exception) {
                progressDialog.dismiss()

                Toast.makeText(
                    this@TaskActivity,
                    e.message.toString(),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun deleteTask() {
        val id = currentTask?.id ?: return

        val progressDialog = util.showProgressDialog(
            this,
            getString(R.string.TextDeleting)
        )

        lifecycleScope.launch {
            try {
                taskViewModel.removeTask(id)
                cleanScreen()
                Toast.makeText(this@TaskActivity, getString(R.string.TextDeleteSuccess), Toast.LENGTH_LONG).show()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(this@TaskActivity, e.message ?: "Error al eliminar", Toast.LENGTH_LONG).show()
            } finally {
                progressDialog.dismiss()
            }
        }
    }
}