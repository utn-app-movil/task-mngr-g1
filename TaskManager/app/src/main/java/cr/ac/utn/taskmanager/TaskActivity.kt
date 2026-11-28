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
import cr.ac.utn.taskmanager.domain.rules.TaskRuleViolation
import cr.ac.utn.taskmanager.util.EXTRA_MESSAGE_ID
import cr.ac.utn.taskmanager.util.util
import cr.ac.utn.taskmanager.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
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

    private var loadedTask: Task? = null
    private var lockedParent: Task? = null
    private var eligibleParents: List<Task> = emptyList()
    private var parentOptions: List<Task?> = listOf(null)
    private var people: List<String> = emptyList()

    private val isEditMode: Boolean get() = loadedTask != null
    private val isLocked: Boolean get() = loadedTask?.status?.isFinal == true

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

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
        btnDueDate = findViewById(R.id.btnDueDate_Task)
        txtCancellationReason = findViewById(R.id.txtCancellationReason_Task)
        lblCancellationReason = findViewById(R.id.lblCancellationReason_Task)
        tvStatusChangeDate = findViewById(R.id.tvStatusChangeDate_Task)

        people = util.loadPeopleFromAssets(this)

        setupSpinners()
        observeEligibleParents()
        cleanScreen()

        val btnSearch = findViewById<ImageButton>(R.id.btnSearch_Task)
        btnSearch.setOnClickListener { searchTask(txtId.text.trim().toString()) }
        btnDueDate.setOnClickListener { showDatePickerDialog() }

        val taskId = intent.getStringExtra(EXTRA_MESSAGE_ID)
        if (!taskId.isNullOrBlank()) {
            txtId.setText(taskId)
            searchTask(taskId)
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
        menu?.findItem(R.id.mnuSave)?.isVisible = !isLocked
        menu?.findItem(R.id.mnuDelete)?.isVisible = isEditMode && !isLocked
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mnuSave -> {
                if (isEditMode) {
                    util.showDialogCondition(this, getString(R.string.TextSaveQuestion)) { saveTask() }
                } else {
                    saveTask()
                }
                true
            }
            R.id.mnuDelete -> {
                util.showDialogCondition(this, getString(R.string.TextDeleteQuestion)) { deleteTask() }
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
        spPriority.adapter = buildAdapter(TaskPriority.entries.toList())
        spAssignTo.adapter = buildAdapter(people)

        spStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                updateCancellationVisibility()
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun <T> buildAdapter(items: List<T>): ArrayAdapter<T> =
        ArrayAdapter(this, android.R.layout.simple_spinner_item, items).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

    private fun setStatusOptions(options: List<TaskStatus>, selected: TaskStatus) {
        spStatus.adapter = buildAdapter(options)
        spStatus.setSelection(options.indexOf(selected).coerceAtLeast(0))
    }

    private fun updateCancellationVisibility() {
        val cancelled = spStatus.selectedItem == TaskStatus.CANCELLED
        val visibility = if (cancelled) View.VISIBLE else View.GONE
        lblCancellationReason.visibility = visibility
        txtCancellationReason.visibility = visibility
    }

    private fun observeEligibleParents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.eligibleParentTasks.collect { parents ->
                    eligibleParents = parents
                    renderParentSpinner()
                }
            }
        }
    }

    private fun renderParentSpinner() {
        val previousId = parentOptions.getOrNull(spParentTask.selectedItemPosition)?.id

        parentOptions = if (isEditMode) listOf(lockedParent)
        else listOf<Task?>(null) + eligibleParents

        val labels = parentOptions.map { parent ->
            parent?.let { "#${it.id} - ${it.title}" } ?: getString(R.string.TextNoParent)
        }
        spParentTask.adapter = buildAdapter(labels)

        if (!isEditMode && previousId != null) {
            val index = parentOptions.indexOfFirst { it?.id == previousId }
            if (index >= 0) spParentTask.setSelection(index)
        }
        spParentTask.isEnabled = !isEditMode
    }

    private fun applyEditability() {
        val editable = !isLocked
        listOf<View>(
            txtTitle, txtDescription, txtObservation, txtCancellationReason,
            spPriority, spAssignTo, btnDueDate
        ).forEach { it.isEnabled = editable }
        spStatus.isEnabled = isEditMode && editable
        spParentTask.isEnabled = !isEditMode
    }

    private fun resetDate() {
        val calendar = Calendar.getInstance()
        dueYear = calendar.get(Calendar.YEAR)
        dueMonth = calendar.get(Calendar.MONTH) + 1
        dueDay = calendar.get(Calendar.DAY_OF_MONTH)
    }

    private fun showDatePickerDialog() {
        DatePickerDialog(this, this, dueYear, dueMonth - 1, dueDay).show()
    }

    private fun getDateFormatString(dayOfMonth: Int, monthValue: Int, yearValue: Int): String {
        return "${if (dayOfMonth < 10) "0" else ""}$dayOfMonth/${if (monthValue < 10) "0" else ""}$monthValue/$yearValue"
    }

    private fun searchTask(idText: String) {
        if (idText.isBlank()) return
        val id = idText.toLongOrNull()
        if (id == null) {
            toast(getString(R.string.TextDataNoFound))
            return
        }
        val progressDialog = util.showProgressDialog(this, getString(R.string.TextLoading))
        lifecycleScope.launch {
            try {
                val task = taskViewModel.getTaskById(id)
                if (task != null) {
                    showTask(task)
                } else {
                    toast(getString(R.string.TextDataNoFound))
                }
            } catch (e: Exception) {
                cleanScreen()
                toast(e.message.toString())
            } finally {
                progressDialog.dismiss()
            }
        }
    }

    private suspend fun showTask(task: Task) {
        loadedTask = task
        lockedParent = task.parentTaskId?.let { taskViewModel.getTaskById(it) }

        txtId.setText(task.id.toString())
        txtId.isEnabled = false
        txtTitle.setText(task.title)
        txtDescription.setText(task.description)
        txtObservation.setText(task.notes)
        txtCancellationReason.setText(task.cancellationReason.orEmpty())
        spPriority.setSelection(task.priority.ordinal)
        spAssignTo.setSelection(people.indexOf(task.assignedTo).coerceAtLeast(0))
        setStatusOptions(task.status.allowedTransitions(), task.status)

        dueDay = task.dueDate.dayOfMonth
        dueMonth = task.dueDate.monthValue
        dueYear = task.dueDate.year
        tvDueDate.text = getDateFormatString(dueDay, dueMonth, dueYear)
        tvStatusChangeDate.text = getString(
            R.string.TextStatusChanged,
            task.statusChangedAt.format(dateTimeFormatter)
        )

        renderParentSpinner()
        applyEditability()
        invalidateOptionsMenu()

        if (task.status.isFinal) toast(getString(R.string.TextTaskLocked))
    }

    private fun cleanScreen() {
        resetDate()
        loadedTask = null
        lockedParent = null
        txtId.isEnabled = true
        txtId.setText("")
        txtTitle.setText("")
        txtDescription.setText("")
        txtObservation.setText("")
        txtCancellationReason.setText("")
        tvDueDate.text = ""
        tvStatusChangeDate.text = ""
        spPriority.setSelection(0)
        spAssignTo.setSelection(0)
        setStatusOptions(listOf(TaskStatus.PENDING), TaskStatus.PENDING)
        renderParentSpinner()
        applyEditability()
        invalidateOptionsMenu()
    }

    private fun isValidationData(): Boolean {
        return txtTitle.text.toString().trim().isNotBlank()
                && txtDescription.text.toString().trim().isNotBlank()
                && tvDueDate.text.toString().trim().isNotBlank()
                && txtObservation.text.toString().trim().isNotBlank()
                && spAssignTo.selectedItem != null
    }

    private fun buildTaskFromForm(): Task {
        val current = loadedTask
        return Task(
            id = current?.id ?: 0,
            title = txtTitle.text.toString().trim(),
            description = txtDescription.text.toString().trim(),
            dueDate = LocalDate.of(dueYear, dueMonth, dueDay),
            creationDate = current?.creationDate ?: LocalDate.now(),
            status = spStatus.selectedItem as TaskStatus,
            assignedTo = spAssignTo.selectedItem as String,
            priority = spPriority.selectedItem as TaskPriority,
            notes = txtObservation.text.toString().trim(),
            parentTaskId = parentOptions.getOrNull(spParentTask.selectedItemPosition)?.id,
            cancellationReason = txtCancellationReason.text.toString().trim().ifBlank { null },
            statusChangedAt = current?.statusChangedAt ?: LocalDateTime.now()
        )
    }

    private fun saveTask() {
        if (!isValidationData()) {
            toast(getString(R.string.TextIncompleteData))
            return
        }
        val task = buildTaskFromForm()
        val progressDialog = util.showProgressDialog(this, getString(R.string.TextSaving))

        val onSuccess = {
            progressDialog.dismiss()
            cleanScreen()
            toast(getString(R.string.TextSaveSuccess))
        }
        val onError = { violation: TaskRuleViolation ->
            progressDialog.dismiss()
            toast(violationMessage(violation))
        }

        if (isEditMode) taskViewModel.updateTask(task, onSuccess, onError)
        else taskViewModel.createTask(task, onSuccess, onError)
    }

    private fun deleteTask() {
        val id = loadedTask?.id ?: return
        val progressDialog = util.showProgressDialog(this, getString(R.string.TextDeleting))
        taskViewModel.removeTask(
            id,
            onSuccess = {
                progressDialog.dismiss()
                cleanScreen()
                toast(getString(R.string.TextDeleteSuccess))
            },
            onError = { violation ->
                progressDialog.dismiss()
                toast(violationMessage(violation))
            }
        )
    }

    private fun violationMessage(violation: TaskRuleViolation): String = getString(
        when (violation) {
            TaskRuleViolation.TASK_IS_FINAL -> R.string.TextErrTaskIsFinal
            TaskRuleViolation.INVALID_TRANSITION -> R.string.TextErrInvalidTransition
            TaskRuleViolation.CANCELLATION_REASON_REQUIRED -> R.string.TextErrCancellationReason
            TaskRuleViolation.OVERDUE_CANNOT_START -> R.string.TextErrOverdue
            TaskRuleViolation.ASSIGNEE_REQUIRED -> R.string.TextErrAssignee
            TaskRuleViolation.PARENT_NOT_FOUND -> R.string.TextErrParentNotFound
            TaskRuleViolation.PARENT_NOT_ELIGIBLE -> R.string.TextErrParentNotEligible
            TaskRuleViolation.TASK_NOT_FOUND -> R.string.TextDataNoFound
        }
    )

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}