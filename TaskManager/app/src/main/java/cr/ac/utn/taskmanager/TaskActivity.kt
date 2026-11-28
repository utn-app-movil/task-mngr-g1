package cr.ac.utn.taskmanager

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
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
import java.util.Calendar
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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

    private var isReadOnly = false
    private var savedParentTaskId: String? = null

    private data class ParentOption(val id: String?, val label: String) {
        override fun toString(): String = label
    }
    private val taskViewModel: TaskViewModel by viewModels {
        TaskViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_task)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
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
            searchTask(txtId.text.trim().toString())
        }

        btnDueDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    override fun onDateSet(view: android.widget.DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        dueDay = dayOfMonth
        dueMonth = month + 1
        dueYear = year
        tvDueDate.text = getDateFormatString(dueDay, dueMonth, dueYear)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.crud_actions, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.mnuSave)?.isVisible = !isReadOnly
        menu?.findItem(R.id.mnuDelete)?.isVisible = isEditMode && !isReadOnly
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
    private fun allowedStatuses(current: TaskStatus): List<TaskStatus> {
        return when (current) {
            TaskStatus.PENDING ->
                listOf(TaskStatus.PENDING, TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED)
            TaskStatus.IN_PROGRESS ->
                listOf(TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED, TaskStatus.CANCELLED)
            TaskStatus.COMPLETED, TaskStatus.CANCELLED ->
                listOf(current)
        }
    }

    private fun showStatusOptions(current: TaskStatus? = null) {
        val options = current?.let { allowedStatuses(it) } ?: listOf(TaskStatus.PENDING)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spStatus.adapter = adapter
        spStatus.setSelection(0)
    }
    private fun setupSpinners() {
        showStatusOptions()
        spStatus.isEnabled = false

        val priorityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TaskPriority.entries.toTypedArray()
        )
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPriority.adapter = priorityAdapter

        val assigneeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.task_assignees).toMutableList()
        )
        assigneeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spAssignTo.adapter = assigneeAdapter
    }

    private fun observeEligibleParents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.eligibleParentTasks.collect {
                    val selectedId = if (isEditMode) {
                        savedParentTaskId
                    } else {
                        (spParentTask.selectedItem as? ParentOption)?.id
                    }
                    updateParentSpinnerSelection(selectedId)
                }
            }
        }
    }

    private fun updateParentSpinnerSelection(selectedId: String?) {
        val parents = taskViewModel.eligibleParentTasks.value
        val options = mutableListOf(ParentOption(null, "Ninguna (Tarea Principal)"))

        options.addAll(parents.map { ParentOption(it.id, "${it.id} - ${it.title}") })

        if (isEditMode && selectedId != null && options.none { it.id == selectedId }) {
            options.add(ParentOption(selectedId, "Tarea padre #$selectedId"))
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spParentTask.adapter = adapter
        spParentTask.setSelection(options.indexOfFirst { it.id == selectedId }.coerceAtLeast(0))
        spParentTask.isEnabled = !isEditMode
    }

    private fun resetDate() {
        val calendar = Calendar.getInstance()
        dueYear = calendar.get(Calendar.YEAR)
        dueMonth = calendar.get(Calendar.MONTH) + 1
        dueDay = calendar.get(Calendar.DAY_OF_MONTH)
    }

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(this, this, dueYear, dueMonth - 1, dueDay)
        datePickerDialog.show()
    }

    private fun searchTask(id: String) {
        if (id.isBlank()) return
        val progressDialog = util.showProgressDialog(this, getString(R.string.TextLoading))
        lifecycleScope.launch {
            try {
                val task = taskViewModel.getTaskById(id)
                if (task != null) {
                    isEditMode = true
                    isReadOnly = task.status == TaskStatus.COMPLETED ||
                            task.status == TaskStatus.CANCELLED
                    savedParentTaskId = task.parentTaskId
                    updateParentSpinnerSelection(savedParentTaskId)
                    txtId.isEnabled = false
                    txtTitle.setText(task.title)
                    txtDescription.setText(task.description)
                    showStatusOptions(task.status)
                    spPriority.setSelection(task.priority.ordinal)
                    val assigneeAdapter = spAssignTo.adapter as ArrayAdapter<String>
                    if (task.assignedTo.isNotBlank() && assigneeAdapter.getPosition(task.assignedTo) < 0) {
                        assigneeAdapter.add(task.assignedTo)
                    }
                    spAssignTo.setSelection(assigneeAdapter.getPosition(task.assignedTo).coerceAtLeast(0))
                    txtObservation.setText(task.notes)
                    findViewById<EditText>(R.id.txtCancellationReason_Task).setText("")
                    findViewById<TextView>(R.id.tvStatusChangeDate_Task).text = ""
                    findViewById<EditText>(R.id.txtCancellationReason_Task)
                        .setText(task.cancellationReason.orEmpty())

                    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                    val statusChanges = listOfNotNull(
                        task.inProgressAt?.let { "En progreso: ${it.format(formatter)}" },
                        task.closedAt?.let { "${task.status.name}: ${it.format(formatter)}" }
                    )
                    findViewById<TextView>(R.id.tvStatusChangeDate_Task).text =
                        statusChanges.joinToString("\n")
                    val dueDateStr = getDateFormatString(
                        task.dueDate.dayOfMonth,
                        task.dueDate.monthValue,
                        task.dueDate.year
                    )
                    tvDueDate.text = dueDateStr
                    dueDay = task.dueDate.dayOfMonth
                    dueMonth = task.dueDate.monthValue
                    dueYear = task.dueDate.year
                    updateFormAccess()
                } else {
                    Toast.makeText(
                        this@TaskActivity,
                        getString(R.string.TextDataNoFound),
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

    private fun getDateFormatString(dayOfMonth: Int, monthValue: Int, yearValue: Int): String {
        return "${if (dayOfMonth < 10) "0" else ""}$dayOfMonth/${if (monthValue < 10) "0" else ""}$monthValue/$yearValue"
    }
    private fun updateFormAccess() {
        val canEdit = !isReadOnly

        txtTitle.isEnabled = canEdit
        txtDescription.isEnabled = canEdit
        txtObservation.isEnabled = canEdit
        spPriority.isEnabled = canEdit
        spAssignTo.isEnabled = canEdit
        spStatus.isEnabled = isEditMode && canEdit

        findViewById<ImageButton>(R.id.btnDueDate_Task).isEnabled = canEdit
        findViewById<EditText>(R.id.txtCancellationReason_Task).isEnabled = canEdit

        invalidateOptionsMenu()
    }
    private fun cleanScreen() {
        resetDate()
        isEditMode = false
        savedParentTaskId = null
        updateParentSpinnerSelection(null)
        txtId.isEnabled = true
        txtId.setText("")
        txtTitle.setText("")
        txtDescription.setText("")
        showStatusOptions()
        spStatus.isEnabled = false
        isReadOnly = false
        spPriority.setSelection(0)
        spAssignTo.setSelection(0)
        txtObservation.setText("")
        tvDueDate.text = ""
        updateFormAccess()
    }

    private fun isValidationData(): Boolean {
        return txtTitle.text.toString().trim().isNotBlank()
                && txtDescription.text.toString().trim().isNotBlank()
                && spStatus.selectedItem != null
                && spPriority.selectedItem != null
                && tvDueDate.text.toString().trim().isNotBlank()
                && txtObservation.text.toString().trim().isNotBlank()
                && spAssignTo.selectedItemPosition > 0
    }

    private fun saveTask() {
        if (isReadOnly) return
        if (!isValidationData()) {
            Toast.makeText(
                this@TaskActivity,
                "Datos incompletos",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val progressDialog = util.showProgressDialog(this, getString(R.string.TextSaving))
        lifecycleScope.launch {
            try {
                val existingTask = if (isEditMode) {
                    taskViewModel.getTaskById(txtId.text.toString().trim())
                } else {
                    null
                }

                if (isEditMode && existingTask == null) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@TaskActivity,
                        "La tarea ya no está disponible",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val dueDate = util.parseStringToDateModern(
                        tvDueDate.text.toString(),
                        "dd/MM/yyyy"
                    ) ?: throw IllegalArgumentException("Selecciona una fecha de vencimiento válida")

                    val selectedStatus = if (isEditMode) {
                        spStatus.selectedItem as TaskStatus
                    } else {
                        TaskStatus.PENDING
                    }

                    if (isEditMode) {
                        val currentStatus = existingTask?.status
                            ?: throw IllegalStateException("La tarea ya no está disponible")

                        if (currentStatus == TaskStatus.COMPLETED || currentStatus == TaskStatus.CANCELLED) {
                            throw IllegalStateException("Esta tarea es de solo lectura")
                        }

                        if (selectedStatus !in allowedStatuses(currentStatus)) {
                            throw IllegalArgumentException("Ese cambio de estado no está permitido")
                        }

                        if (currentStatus == TaskStatus.PENDING &&
                            selectedStatus == TaskStatus.IN_PROGRESS &&
                            dueDate.isBefore(LocalDate.now())
                        ) {
                            throw IllegalArgumentException("Una tarea vencida no puede pasar a En progreso")
                        }
                    }
                    val selectedPriority = spPriority.selectedItem as TaskPriority
                    val parentTaskId = if (isEditMode) {
                        existingTask?.parentTaskId
                    } else {
                        (spParentTask.selectedItem as? ParentOption)?.id
                    }

                    if (!isEditMode && parentTaskId != null) {
                        val parent = taskViewModel.getTaskById(parentTaskId)
                        if (parent?.status != TaskStatus.PENDING && parent?.status != TaskStatus.IN_PROGRESS) {
                            progressDialog.dismiss()
                            Toast.makeText(
                                this@TaskActivity,
                                "Selecciona una tarea padre pendiente o en progreso",
                                Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }
                    }

                    val cancellationReason = if (selectedStatus == TaskStatus.CANCELLED) {
                        val reason = findViewById<EditText>(R.id.txtCancellationReason_Task)
                            .text.toString().trim()
                        require(reason.isNotBlank()) { "Indica la razón de la cancelación" }
                        reason
                    } else {
                        null
                    }

                    val changedAt = if (isEditMode && existingTask?.status != selectedStatus) {
                        LocalDateTime.now()
                    } else {
                        null
                    }

                    val task = Task(
                        existingTask?.id.orEmpty(),
                        txtTitle.text.toString().trim(),
                        txtDescription.text.toString().trim(),
                        dueDate,
                        LocalDate.now(),
                        selectedStatus,
                        spAssignTo.selectedItem?.toString() ?: "",
                        selectedPriority,
                        txtObservation.text.toString().trim(),
                        parentTaskId = parentTaskId,
                        cancellationReason = cancellationReason,
                        inProgressAt = changedAt?.takeIf { selectedStatus == TaskStatus.IN_PROGRESS }
                            ?: existingTask?.inProgressAt,
                        closedAt = changedAt?.takeIf {
                            selectedStatus == TaskStatus.COMPLETED || selectedStatus == TaskStatus.CANCELLED
                        } ?: existingTask?.closedAt
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
                        taskViewModel.saveTask(task, onCompleteCallback)
                    } else {
                        taskViewModel.updateTask(task, onCompleteCallback)
                    }
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
        if (isReadOnly) return
        val id = txtId.text.toString().trim()
        if (id.isBlank()) return
        val progressDialog = util.showProgressDialog(this, getString(R.string.TextDeleting))
        taskViewModel.removeTask(id) {
            progressDialog.dismiss()
            cleanScreen()
            Toast.makeText(
                this@TaskActivity,
                getString(R.string.TextDeleteSuccess),
                Toast.LENGTH_LONG
            ).show()
        }
    }
}