package cr.ac.utn.taskmanager

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.DatePicker
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
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cr.ac.utn.taskmanager.domain.model.StatusChange
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

class TaskActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener {

    private lateinit var txtId: EditText
    private lateinit var txtTitle: EditText
    private lateinit var txtDescription: EditText
    private lateinit var txtObservation: EditText
    private lateinit var txtCancellationReason: EditText
    private lateinit var lblCancellationReason: TextView
    private lateinit var tvDueDate: TextView
    private lateinit var tvStatusHistory: TextView
    private lateinit var tvSubtasks: TextView
    private lateinit var btnDueDate: ImageButton
    private lateinit var spStatus: Spinner
    private lateinit var spPriority: Spinner
    private lateinit var spAssignTo: Spinner
    private lateinit var spParentTask: Spinner
    private var dueDate: LocalDate? = null
    private var loadedTask: Task? = null
    private var eligibleParents = emptyList<Task>()
    private var parentIds = emptyList<String?>()
    private lateinit var people: List<String>

    private val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val changeFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    private val taskViewModel: TaskViewModel by viewModels {
        TaskViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_task)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        txtId = findViewById(R.id.txtId_Task)
        txtTitle = findViewById(R.id.txtTitle_Task)
        txtDescription = findViewById(R.id.txtDescription_Task)
        txtObservation = findViewById(R.id.txtObservation_Task)
        txtCancellationReason = findViewById(R.id.txtCancellationReason_Task)
        lblCancellationReason = findViewById(R.id.lblCancellationReason_Task)
        tvDueDate = findViewById(R.id.tvDueDate_Task)
        tvStatusHistory = findViewById(R.id.tvStatusChangeDate_Task)
        tvSubtasks = findViewById(R.id.tvSubtasks_Task)
        btnDueDate = findViewById(R.id.btnDueDate_Task)
        spStatus = findViewById(R.id.spStatus_Task)
        spPriority = findViewById(R.id.spPriority_Task)
        spAssignTo = findViewById(R.id.spAssignTo_Task)
        spParentTask = findViewById(R.id.spParentTask_Task)

        people = (application as TaskManagerApp).people
        spPriority.setOptions(TaskPriority.entries)
        spStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateCancellationVisibility()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = updateCancellationVisibility()
        }

        renderTask(null)
        observeTasks()
        findViewById<ImageButton>(R.id.btnSearch_Task).setOnClickListener {
            searchTask(txtId.text.toString().trim())
        }
        btnDueDate.setOnClickListener {
            val date = dueDate ?: LocalDate.now()
            DatePickerDialog(this, this, date.year, date.monthValue - 1, date.dayOfMonth).show()
        }

        val initialId = savedInstanceState?.getString("loadedTaskId") ?: intent.getStringExtra(EXTRA_MESSAGE_ID)
        if (!initialId.isNullOrBlank()) searchTask(initialId)
        else if (savedInstanceState != null) {
            setDueDate(savedInstanceState.getString("dueDate")?.let(LocalDate::parse))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("loadedTaskId", loadedTask?.id ?: "")
        outState.putString("dueDate", dueDate?.toString())
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        setDueDate(LocalDate.of(year, month + 1, dayOfMonth))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.crud_actions, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.mnuSave).isVisible = loadedTask?.status?.isActive != false
        menu.findItem(R.id.mnuDelete).isVisible = loadedTask?.status?.isActive == true
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.mnuSave -> {
                if (loadedTask == null) saveTask()
                else util.showDialogCondition(this, getString(R.string.TextSaveQuestion)) { saveTask() }
                true
            }
            R.id.mnuDelete -> {
                util.showDialogCondition(this, getString(R.string.TextDeleteQuestion)) { deleteTask() }
                true
            }
            R.id.mnuCancel -> {
                renderTask(null)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun observeTasks() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    taskViewModel.eligibleParentTasks.collect { parents ->
                        val selectedId = parentIds.getOrNull(spParentTask.selectedItemPosition)
                        eligibleParents = parents
                        updateParentOptions(selectedId)
                    }
                }
                launch {
                    taskViewModel.tasks.collect { tasks -> updateSubtasks(tasks) }
                }
            }
        }
    }

    private fun updateParentOptions(selectedId: String?) {
        val task = loadedTask
        if (task == null) {
            parentIds = listOf(null) + eligibleParents.map { it.id }
            spParentTask.setOptions(listOf(getString(R.string.TaskNoParent)) + eligibleParents.map {
                getString(R.string.TaskNumberAndTitle, it.id, it.title)
            })
            spParentTask.setSelection(parentIds.indexOf(selectedId).coerceAtLeast(0))
        } else {
            val parent = taskViewModel.tasks.value.firstOrNull { it.id == task.parentId }
            val parentLabel = when {
                parent != null -> getString(R.string.TaskNumberAndTitle, parent.id, parent.title)
                task.parentId != null -> "#${task.parentId}"
                else -> getString(R.string.TaskNoParent)
            }
            parentIds = listOf(task.parentId)
            spParentTask.setOptions(listOf(parentLabel))
        }
        spParentTask.isEnabled = task == null
    }

    private fun updateSubtasks(tasks: List<Task>) {
        val task = loadedTask
        val children = if (task == null) emptyList() else tasks.filter { it.parentId == task.id }
        tvSubtasks.isVisible = children.isNotEmpty()
        tvSubtasks.text = getString(R.string.TaskSubtasks, children.joinToString("\n") {
            getString(R.string.TaskNumberAndTitle, it.id, it.title)
        })
        if (task != null) updateParentOptions(task.parentId)
    }

    private fun <T> Spinner.setOptions(options: List<T>) {
        val spinnerAdapter = ArrayAdapter(this@TaskActivity, android.R.layout.simple_spinner_item, options)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        adapter = spinnerAdapter
    }

    private fun renderTask(task: Task?, changes: List<StatusChange> = emptyList()) {
        loadedTask = task
        val editable = task?.status?.isActive != false
        txtId.setText(task?.id.orEmpty())
        txtId.isEnabled = task == null
        txtTitle.setText(task?.title.orEmpty())
        txtTitle.isEnabled = editable
        txtDescription.setText(task?.description.orEmpty())
        txtDescription.isEnabled = editable
        txtObservation.setText(task?.notes.orEmpty())
        txtObservation.isEnabled = editable

        val editableStatuses = task?.status?.availableStatuses() ?: listOf(TaskStatus.PENDING)
        spStatus.setOptions(editableStatuses)
        spStatus.setSelection(editableStatuses.indexOf(task?.status ?: TaskStatus.PENDING))
        spStatus.isEnabled = editableStatuses.size > 1
        spPriority.setSelection(task?.priority?.ordinal ?: 0)
        spPriority.isEnabled = editable

        val displayedPeople = if (editable) people else listOf(task.assignedTo)
        spAssignTo.setOptions(listOf(getString(R.string.TaskSelectPerson)) + displayedPeople)
        spAssignTo.setSelection(displayedPeople.indexOf(task?.assignedTo.orEmpty()) + 1)
        spAssignTo.isEnabled = editable

        txtCancellationReason.error = null
        txtCancellationReason.setText(task?.cancellationReason.orEmpty())
        txtCancellationReason.isEnabled = editable
        updateCancellationVisibility()
        btnDueDate.isEnabled = editable
        setDueDate(task?.dueDate)
        if (task == null) updateParentOptions(null)
        updateSubtasks(taskViewModel.tasks.value)

        tvStatusHistory.isVisible = changes.isNotEmpty()
        tvStatusHistory.text = getString(R.string.TaskStatusHistory, changes.joinToString("\n") {
            getString(
                R.string.TaskStatusHistoryEntry,
                it.changedAt.format(changeFormat),
                it.previousStatus.name,
                it.newStatus.name
            )
        })
        invalidateOptionsMenu()
    }

    private fun setDueDate(date: LocalDate?) {
        dueDate = date
        tvDueDate.text = date?.format(dateFormat).orEmpty()
    }

    private fun updateCancellationVisibility() {
        val cancelled = spStatus.selectedItem == TaskStatus.CANCELLED
        lblCancellationReason.isVisible = cancelled
        txtCancellationReason.isVisible = cancelled
    }

    private fun searchTask(id: String) {
        if (id.isBlank()) return
        runWithProgress(R.string.TextLoading) {
            val task = taskViewModel.getTaskById(id)
            if (task == null) showMessage(getString(R.string.TextDataNoFound))
            else renderTask(task, taskViewModel.getStatusChanges(task.id))
        }
    }

    private fun saveTask() {
        val title = txtTitle.text.toString().trim()
        val description = txtDescription.text.toString().trim()
        val notes = txtObservation.text.toString().trim()
        val assignee = spAssignTo.selectedItem as? String
        val date = dueDate
        val previous = loadedTask
        val status = if (previous == null) TaskStatus.PENDING else spStatus.selectedItem as TaskStatus
        val reason = txtCancellationReason.text.toString().trim()

        val fieldError = when {
            title.isBlank() -> R.string.TaskTitleRequired
            description.isBlank() -> R.string.TaskDescriptionRequired
            else -> null
        }
        if (fieldError != null) {
            showMessage(getString(fieldError))
            return
        }
        if (date == null) {
            showMessage(getString(R.string.TaskDueDateRequired))
            return
        }
        if (assignee == null || spAssignTo.selectedItemPosition <= 0) {
            showMessage(getString(R.string.TaskSelectPerson))
            return
        }
        if (status == TaskStatus.CANCELLED && reason.isBlank()) {
            txtCancellationReason.error = getString(R.string.TaskCancellationRequired)
            txtCancellationReason.requestFocus()
            return
        }

        val task = Task(
            id = previous?.id.orEmpty(),
            title = title,
            description = description,
            dueDate = date,
            creationDate = previous?.creationDate ?: LocalDate.now(),
            status = status,
            assignedTo = assignee,
            priority = spPriority.selectedItem as TaskPriority,
            notes = notes,
            parentId = parentIds.getOrNull(spParentTask.selectedItemPosition),
            cancellationReason = if (status == TaskStatus.CANCELLED) reason else null
        )

        runWithProgress(R.string.TextSaving) {
            val id = if (previous == null) taskViewModel.saveTask(task) else {
                taskViewModel.updateTask(task)
                task.id
            }
            if (previous == null) renderTask(null)
            else {
                val savedTask = checkNotNull(taskViewModel.getTaskById(id))
                renderTask(savedTask, taskViewModel.getStatusChanges(id))
            }
            showMessage(getString(R.string.TaskSavedWithId, id))
        }
    }

    private fun deleteTask() {
        val task = loadedTask ?: return
        runWithProgress(R.string.TextDeleting) {
            taskViewModel.removeTask(task.id)
            renderTask(null)
            showMessage(getString(R.string.TextDeleteSuccess))
        }
    }

    private fun runWithProgress(message: Int, action: suspend () -> Unit) {
        val progress = util.showProgressDialog(this, getString(message))
        lifecycleScope.launch {
            try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showMessage(error.localizedMessage ?: getString(R.string.TaskOperationError))
            } finally {
                progress.dismiss()
            }
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
