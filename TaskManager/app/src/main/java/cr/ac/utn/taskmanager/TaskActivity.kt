package cr.ac.utn.taskmanager

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    private lateinit var txtCancellationReason: EditText
    private lateinit var lblCancellationReason: TextView
    private lateinit var tvStatusChangeDate: TextView
    private lateinit var tvDueDate: TextView
    private lateinit var btnDueDate: ImageButton

    private var dueDay: Int = 0
    private var dueMonth: Int = 0
    private var dueYear: Int = 0
    private var isEditMode: Boolean = false
    private var currentTask: Task? = null
    private var availableParents: List<Task> = emptyList()
    private var parentIds: List<String?> = listOf(null)

    private val taskViewModel: TaskViewModel by viewModels {
        TaskViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_task)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
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
        txtCancellationReason = findViewById(R.id.txtCancellationReason_Task)
        lblCancellationReason = findViewById(R.id.lblCancellationReason_Task)
        tvStatusChangeDate = findViewById(R.id.tvStatusChangeDate_Task)
        btnDueDate = findViewById(R.id.btnDueDate_Task)

        setupSpinners()
        observeEligibleParents()
        cleanScreen()

        val taskId = intent.getStringExtra(EXTRA_MESSAGE_ID)

        if (!taskId.isNullOrBlank()) {
            searchTask(taskId)
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
            dueDay,
            dueMonth,
            dueYear
        )
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.crud_actions, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val finished =
            currentTask?.status == TaskStatus.COMPLETED ||
                    currentTask?.status == TaskStatus.CANCELLED

        menu?.findItem(R.id.mnuSave)?.isVisible = !finished
        menu?.findItem(R.id.mnuDelete)?.isVisible = isEditMode && !finished

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
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

    private fun setupSpinners() {

        val statusAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TaskStatus.entries.toTypedArray()
        )

        statusAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spStatus.adapter = statusAdapter

        val priorityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TaskPriority.entries.toTypedArray()
        )

        priorityAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spPriority.adapter = priorityAdapter

        val people = util.loadPeopleFromAssets(this)

        val peopleAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            people
        )

        peopleAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spAssignTo.adapter = peopleAdapter

        spStatus.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    showCancellationReason()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }
            }
    }

    private fun observeEligibleParents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.eligibleParentTasks.collect { parents ->
                    availableParents = parents
                    updateParentSpinnerSelection()
                }
            }
        }
    }

    private fun updateParentSpinnerSelection() {

        val parentOptions = mutableListOf(
            getString(R.string.TextMainTask)
        )

        val ids = mutableListOf<String?>(null)

        for (parent in availableParents) {
            if (parent.id != currentTask?.id) {
                ids.add(parent.id)
                parentOptions.add("${parent.id} - ${parent.title}")
            }
        }

        val originalParentId = currentTask?.parentTaskId

        if (
            originalParentId != null &&
            !ids.contains(originalParentId)
        ) {
            ids.add(originalParentId)
            parentOptions.add(
                getString(
                    R.string.TextCurrentParent,
                    originalParentId
                )
            )
        }

        parentIds = ids

        val parentAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            parentOptions
        )

        parentAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spParentTask.adapter = parentAdapter

        val selectedPosition = parentIds.indexOf(originalParentId)

        if (selectedPosition >= 0) {
            spParentTask.setSelection(selectedPosition)
        }
    }

    private fun showCancellationReason() {

        val isCancelled =
            spStatus.selectedItem == TaskStatus.CANCELLED

        lblCancellationReason.visibility =
            if (isCancelled) View.VISIBLE else View.GONE

        txtCancellationReason.visibility =
            if (isCancelled) View.VISIBLE else View.GONE
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

        if (id.isBlank()) return

        val progressDialog = util.showProgressDialog(
            this,
            getString(R.string.TextLoading)
        )

        lifecycleScope.launch {
            try {
                val task = taskViewModel.getTaskById(id)

                if (task != null) {

                    currentTask = task
                    isEditMode = true

                    txtId.setText(task.id)
                    txtTitle.setText(task.title)
                    txtDescription.setText(task.description)

                    spStatus.setSelection(task.status.ordinal)
                    spPriority.setSelection(task.priority.ordinal)

                    txtObservation.setText(task.notes)
                    txtCancellationReason.setText(task.cancellationReason)

                    val peopleAdapter = spAssignTo.adapter

                    for (i in 0 until peopleAdapter.count) {
                        if (
                            peopleAdapter.getItem(i).toString() ==
                            task.assignedTo
                        ) {
                            spAssignTo.setSelection(i)
                            break
                        }
                    }

                    dueDay = task.dueDate.dayOfMonth
                    dueMonth = task.dueDate.monthValue
                    dueYear = task.dueDate.year

                    tvDueDate.text = getDateFormatString(
                        dueDay,
                        dueMonth,
                        dueYear
                    )

                    if (task.statusChangeDate != null) {

                        val dateText = task.statusChangeDate.format(
                            DateTimeFormatter.ofPattern(
                                "dd/MM/yyyy HH:mm"
                            )
                        )

                        tvStatusChangeDate.text = getString(
                            R.string.TextStatusChangedAt,
                            dateText
                        )

                        tvStatusChangeDate.visibility = View.VISIBLE

                    } else {

                        tvStatusChangeDate.text = ""
                        tvStatusChangeDate.visibility = View.GONE
                    }

                    updateParentSpinnerSelection()
                    updateEditMode()
                    showCancellationReason()
                    invalidateOptionsMenu()

                } else {

                    Toast.makeText(
                        this@TaskActivity,
                        R.string.TextDataNoFound,
                        Toast.LENGTH_LONG
                    ).show()
                }

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

    private fun updateEditMode() {

        val finished =
            currentTask?.status == TaskStatus.COMPLETED ||
                    currentTask?.status == TaskStatus.CANCELLED

        val editable = !finished

        txtId.isEnabled = false
        txtTitle.isEnabled = editable
        txtDescription.isEnabled = editable

        spStatus.isEnabled = isEditMode && editable
        spPriority.isEnabled = editable
        spAssignTo.isEnabled = editable
        spParentTask.isEnabled = !isEditMode && editable

        txtObservation.isEnabled = editable
        txtCancellationReason.isEnabled = editable
        btnDueDate.isEnabled = editable
    }

    private fun cleanScreen() {

        resetDate()

        isEditMode = false
        currentTask = null

        txtId.setText("")
        txtTitle.setText("")
        txtDescription.setText("")

        spStatus.setSelection(TaskStatus.PENDING.ordinal)
        spPriority.setSelection(0)

        if (spAssignTo.adapter.count > 0) {
            spAssignTo.setSelection(0)
        }

        txtObservation.setText("")
        txtCancellationReason.setText("")

        tvDueDate.text = getDateFormatString(
            dueDay,
            dueMonth,
            dueYear
        )

        tvStatusChangeDate.text = ""
        tvStatusChangeDate.visibility = View.GONE

        updateParentSpinnerSelection()
        showCancellationReason()
        updateEditMode()
        invalidateOptionsMenu()
    }

    private fun isValidationData(): Boolean {

        return txtTitle.text.toString().trim().isNotBlank()
                && txtDescription.text.toString().trim().isNotBlank()
                && tvDueDate.text.toString().trim().isNotBlank()
                && spPriority.selectedItem != null
                && spAssignTo.selectedItem != null
    }

    private fun saveTask() {

        if (!isValidationData()) {

            Toast.makeText(
                this,
                R.string.TextIncompleteData,
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val previousTask = currentTask

        if (
            previousTask?.status == TaskStatus.COMPLETED ||
            previousTask?.status == TaskStatus.CANCELLED
        ) {

            Toast.makeText(
                this,
                R.string.TextFinishedTask,
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val selectedStatus = if (isEditMode) {
            spStatus.selectedItem as TaskStatus
        } else {
            TaskStatus.PENDING
        }

        if (previousTask != null) {

            val oldStatus = previousTask.status

            val validChange = when (oldStatus) {

                TaskStatus.PENDING ->
                    selectedStatus == TaskStatus.PENDING ||
                            selectedStatus == TaskStatus.IN_PROGRESS ||
                            selectedStatus == TaskStatus.CANCELLED

                TaskStatus.IN_PROGRESS ->
                    selectedStatus == TaskStatus.IN_PROGRESS ||
                            selectedStatus == TaskStatus.CANCELLED ||
                            selectedStatus == TaskStatus.COMPLETED

                TaskStatus.COMPLETED,
                TaskStatus.CANCELLED -> false
            }

            if (!validChange) {

                Toast.makeText(
                    this,
                    R.string.TextInvalidStatusChange,
                    Toast.LENGTH_LONG
                ).show()

                return
            }
        }

        val dueDate = util.parseStringToDateModern(
            tvDueDate.text.toString(),
            "dd/MM/yyyy"
        )

        if (dueDate == null) {

            Toast.makeText(
                this,
                R.string.TextIncompleteData,
                Toast.LENGTH_LONG
            ).show()

            return
        }

        if (
            selectedStatus == TaskStatus.IN_PROGRESS &&
            previousTask?.status != TaskStatus.IN_PROGRESS &&
            dueDate.isBefore(LocalDate.now())
        ) {

            Toast.makeText(
                this,
                R.string.TextExpiredTask,
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val reason = txtCancellationReason.text.toString().trim()

        if (
            selectedStatus == TaskStatus.CANCELLED &&
            reason.isBlank()
        ) {

            Toast.makeText(
                this,
                R.string.TextCancellationRequired,
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val parentId = if (isEditMode) {
            previousTask?.parentTaskId
        } else {
            parentIds.getOrNull(spParentTask.selectedItemPosition)
        }

        val progressDialog = util.showProgressDialog(
            this,
            getString(R.string.TextSaving)
        )

        lifecycleScope.launch {
            try {

                if (parentId != null && !isEditMode) {

                    val parentTask = taskViewModel.getTaskById(parentId)

                    if (
                        parentTask == null ||
                        (
                                parentTask.status != TaskStatus.PENDING &&
                                        parentTask.status != TaskStatus.IN_PROGRESS
                                )
                    ) {

                        progressDialog.dismiss()

                        Toast.makeText(
                            this@TaskActivity,
                            R.string.TextInvalidParent,
                            Toast.LENGTH_LONG
                        ).show()

                        return@launch
                    }
                }

                // Guardar la hora local de Costa Rica
                val changeDate =
                    if (
                        previousTask != null &&
                        previousTask.status != selectedStatus
                    ) {
                        LocalDateTime.now(
                            ZoneId.of("America/Costa_Rica")
                        )
                    } else {
                        previousTask?.statusChangeDate
                    }

                val task = Task(
                    id = previousTask?.id ?: "",
                    title = txtTitle.text.toString().trim(),
                    description = txtDescription.text.toString().trim(),
                    dueDate = dueDate,
                    creationDate = previousTask?.creationDate
                        ?: LocalDate.now(),
                    status = selectedStatus,
                    assignedTo = spAssignTo.selectedItem.toString(),
                    priority = spPriority.selectedItem as TaskPriority,
                    notes = txtObservation.text.toString().trim(),
                    parentTaskId = parentId,
                    cancellationReason =
                        if (selectedStatus == TaskStatus.CANCELLED) {
                            reason
                        } else {
                            ""
                        },
                    statusChangeDate = changeDate
                )

                val onCompleteCallback = {

                    progressDialog.dismiss()
                    cleanScreen()

                    Toast.makeText(
                        this@TaskActivity,
                        R.string.TextSaveSuccess,
                        Toast.LENGTH_LONG
                    ).show()
                }

                if (isEditMode) {
                    taskViewModel.updateTask(task, onCompleteCallback)
                } else {
                    taskViewModel.saveTask(task, onCompleteCallback)
                }

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

        val task = currentTask ?: return

        if (
            task.status == TaskStatus.COMPLETED ||
            task.status == TaskStatus.CANCELLED
        ) {

            Toast.makeText(
                this,
                R.string.TextFinishedTask,
                Toast.LENGTH_LONG
            ).show()

            return
        }

        val progressDialog = util.showProgressDialog(
            this,
            getString(R.string.TextDeleting)
        )

        lifecycleScope.launch {
            try {

                if (taskViewModel.hasSubtasks(task.id)) {

                    progressDialog.dismiss()

                    Toast.makeText(
                        this@TaskActivity,
                        R.string.TextTaskHasSubtasks,
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }

                taskViewModel.removeTask(task.id) {

                    progressDialog.dismiss()
                    cleanScreen()

                    Toast.makeText(
                        this@TaskActivity,
                        R.string.TextDeleteSuccess,
                        Toast.LENGTH_LONG
                    ).show()
                }

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
}
