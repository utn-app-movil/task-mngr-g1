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
import org.json.JSONArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TaskActivity : AppCompatActivity(), DatePickerDialog.OnDateSetListener {
    private lateinit var txtId: EditText
    private lateinit var txtTitle: EditText
    private lateinit var txtDescription: EditText
    private lateinit var txtObservation: EditText
    private lateinit var txtCancellationReason: EditText
    private lateinit var spStatus: Spinner
    private lateinit var spPriority: Spinner
    private lateinit var spAssignTo: Spinner
    private lateinit var spParentTask: Spinner
    private lateinit var peopleAdapter: ArrayAdapter<String>
    private lateinit var tvDueDate: TextView
    private lateinit var tvStatusChangeDate: TextView
    private lateinit var tvSubtasks: TextView
    private lateinit var btnDueDate: ImageButton
    private lateinit var btnSearch: ImageButton

    private var currentTask: Task? = null
    private var dueDate: LocalDate = LocalDate.now()
    private var parentTasks: List<Task> = emptyList()
    private var parentOptions: List<Task?> = listOf(null)
    private var selectedParent: Task? = null
    private var selectedParentId: String? = null
    private var allTasks: List<Task> = emptyList()
    private var isBusy = false

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
        spStatus = findViewById(R.id.spStatus_Task)
        spPriority = findViewById(R.id.spPriority_Task)
        spAssignTo = findViewById(R.id.spAssignTo_Task)
        spParentTask = findViewById(R.id.spParentTask_Task)
        tvDueDate = findViewById(R.id.tvDueDate_Task)
        tvStatusChangeDate = findViewById(R.id.tvStatusChangeDate_Task)
        tvSubtasks = findViewById(R.id.tvSubtasks_Task)
        btnDueDate = findViewById(R.id.btnDueDate_Task)
        btnSearch = findViewById(R.id.btnSearch_Task)

        val priorityAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, TaskPriority.entries.toTypedArray()
        )
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPriority.adapter = priorityAdapter
        loadPeople()
        spStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateCancellationFields()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spParentTask.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedParent = parentOptions.getOrNull(position)
                selectedParentId = selectedParent?.id
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        cleanScreen()
        observeTasks()
        val taskId = if (savedInstanceState != null) savedInstanceState.getString("currentTaskId")
        else intent.getStringExtra(EXTRA_MESSAGE_ID)
        if (!taskId.isNullOrBlank()) {
            searchTask(taskId, savedInstanceState)
        } else {
            restoreForm(savedInstanceState)
        }

        btnSearch.setOnClickListener { searchTask(txtId.text.toString().trim()) }
        btnDueDate.setOnClickListener {
            DatePickerDialog(this, this, dueDate.year, dueDate.monthValue - 1, dueDate.dayOfMonth).show()
        }
    }

    private fun loadStatuses(task: Task?) {
        val statuses = taskViewModel.getAllowedStatuses(task)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statuses)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spStatus.adapter = adapter
        if (task != null) spStatus.setSelection(statuses.indexOf(task.status))
    }

    private fun loadPeople() {
        val names = mutableListOf(getString(R.string.TextSelectPerson))
        try {
            val json = assets.open("people.json").bufferedReader().use { it.readText() }
            val people = JSONArray(json)
            for (index in 0 until people.length()) {
                names.add(people.getJSONObject(index).getString("name"))
            }
        } catch (e: Exception) {
            showMessage(R.string.TextPeopleError)
        }
        peopleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        peopleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spAssignTo.adapter = peopleAdapter
    }

    private fun observeTasks() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    taskViewModel.eligibleParentTasks.collect { tasks ->
                        parentTasks = tasks
                        updateParentSpinner()
                    }
                }
                launch {
                    taskViewModel.tasks.collect { tasks ->
                        allTasks = tasks
                        showSubtasks()
                    }
                }
            }
        }
    }

    private fun updateParentSpinner() {
        val options = mutableListOf<Task?>(null)
        options.addAll(parentTasks.filter { it.id != currentTask?.id })
        // Conserva la relaci�n guardada aunque la tarea principal ya est� cerrada.
        val parent = selectedParent
        if (parent != null && options.none { it?.id == parent.id }) options.add(parent)
        parentOptions = options
        val labels = options.map {
            if (it == null) getString(R.string.TextNoParent)
            else getString(R.string.TextTaskOption, it.id, it.title)
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spParentTask.adapter = adapter
        val position = options.indexOfFirst { it?.id == selectedParentId }
        spParentTask.setSelection(if (position >= 0) position else 0)
    }

    private fun showSubtasks() {
        val task = currentTask
        tvSubtasks.visibility = if (task == null) View.GONE else View.VISIBLE
        if (task == null) return
        val subtasks = allTasks.filter { it.parentTaskId == task.id }
        val detail = if (subtasks.isEmpty()) getString(R.string.TextNoSubtasks)
        else subtasks.joinToString("\n") {
            getString(R.string.TextSubtaskItem, it.id, it.title, it.status.name)
        }
        tvSubtasks.text = getString(R.string.TextSubtasks, detail)
    }

    private fun searchTask(id: String, savedForm: Bundle? = null) {
        if (isBusy) return
        if (id.isBlank()) {
            showMessage(R.string.TextEnterId)
            return
        }
        setBusy(true)
        lifecycleScope.launch {
            try {
                val task = taskViewModel.getTaskById(id)
                if (task == null) {
                    showMessage(R.string.TextDataNoFound)
                } else {
                    loadTask(task, savedForm)
                }
            } catch (e: Exception) {
                showMessage(R.string.TextLoadingError)
            } finally {
                setBusy(false)
            }
        }
    }

    private suspend fun loadTask(task: Task, savedForm: Bundle?) {
        currentTask = task
        selectedParentId = task.parentTaskId
        selectedParent = task.parentTaskId?.let { taskViewModel.getTaskById(it) }
        txtId.setText(task.id)
        txtTitle.setText(task.title)
        txtDescription.setText(task.description)
        txtObservation.setText(task.notes)
        txtCancellationReason.setText(task.cancellationReason)
        dueDate = task.dueDate
        showDueDate()
        loadStatuses(task)
        spPriority.setSelection(task.priority.ordinal)
        var personPosition = peopleAdapter.getPosition(task.assignedTo)
        if (personPosition < 0 && taskViewModel.isClosed(task)) {
            peopleAdapter.add(task.assignedTo)
            personPosition = peopleAdapter.getPosition(task.assignedTo)
        }
        spAssignTo.setSelection(if (personPosition >= 0) personPosition else 0)
        updateParentSpinner()
        val changedAt = task.statusChangeDate
        tvStatusChangeDate.text = if (changedAt == null) {
            getString(R.string.TextNoStatusChange)
        } else {
            getString(R.string.TextStatusChangedAt,
                changedAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")))
        }
        tvStatusChangeDate.visibility = View.VISIBLE
        showSubtasks()
        if (!taskViewModel.isClosed(task)) restoreForm(savedForm)
    }

    override fun onDateSet(view: android.widget.DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
        dueDate = LocalDate.of(year, month + 1, dayOfMonth)
        showDueDate()
    }

    private fun showDueDate() {
        tvDueDate.text = dueDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }

    private fun cleanScreen() {
        currentTask = null
        selectedParent = null
        selectedParentId = null
        dueDate = LocalDate.now()
        txtId.setText("")
        txtTitle.setText("")
        txtDescription.setText("")
        txtObservation.setText("")
        txtCancellationReason.setText("")
        loadStatuses(null)
        spPriority.setSelection(0)
        spAssignTo.setSelection(0)
        tvStatusChangeDate.visibility = View.GONE
        showDueDate()
        updateParentSpinner()
        showSubtasks()
        updateEditableFields()
    }

    private fun setBusy(busy: Boolean) {
        isBusy = busy
        updateEditableFields()
    }

    private fun updateEditableFields() {
        val closed = currentTask?.let { taskViewModel.isClosed(it) } ?: false
        val editable = !isBusy && !closed
        txtId.isEnabled = !isBusy && currentTask == null
        btnSearch.isEnabled = !isBusy && currentTask == null
        txtTitle.isEnabled = editable
        txtDescription.isEnabled = editable
        txtObservation.isEnabled = editable
        spPriority.isEnabled = editable
        spAssignTo.isEnabled = editable
        spStatus.isEnabled = editable && currentTask != null
        spParentTask.isEnabled = editable && currentTask == null
        btnDueDate.isEnabled = editable
        txtCancellationReason.isEnabled = editable
        updateCancellationFields()
        invalidateOptionsMenu()
    }

    private fun updateCancellationFields() {
        val visible = spStatus.selectedItem == TaskStatus.CANCELLED
        val visibility = if (visible) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.lblCancellationReason_Task).visibility = visibility
        txtCancellationReason.visibility = visibility
    }

    private fun saveTask() {
        if (isBusy) return
        if (spAssignTo.selectedItemPosition <= 0
            || txtTitle.text.isBlank() || txtDescription.text.isBlank()) {
            showMessage(R.string.TextRequiredFields)
            return
        }
        val task = Task(
            id = currentTask?.id ?: "",
            title = txtTitle.text.toString().trim(),
            description = txtDescription.text.toString().trim(),
            dueDate = dueDate,
            creationDate = currentTask?.creationDate ?: LocalDate.now(),
            status = spStatus.selectedItem as TaskStatus,
            assignedTo = spAssignTo.selectedItem.toString(),
            priority = spPriority.selectedItem as TaskPriority,
            notes = txtObservation.text.toString().trim(),
            parentTaskId = selectedParentId,
            cancellationReason = txtCancellationReason.text.toString().trim()
        )
        setBusy(true)
        lifecycleScope.launch {
            try {
                val result = if (currentTask == null) taskViewModel.saveTask(task)
                else taskViewModel.updateTask(task)
                if (result) {
                    cleanScreen()
                    showMessage(R.string.TextSaveSuccess)
                } else {
                    showMessage(taskViewModel.errorMessage)
                }
            } catch (e: Exception) {
                showMessage(R.string.TextSaveError)
            } finally {
                setBusy(false)
            }
        }
    }

    private fun deleteTask() {
        val task = currentTask ?: return
        if (isBusy) return
        setBusy(true)
        lifecycleScope.launch {
            try {
                val deleted = taskViewModel.removeTask(task.id)
                if (deleted) {
                    cleanScreen()
                    showMessage(R.string.TextDeleteSuccess)
                } else {
                    showMessage(taskViewModel.errorMessage)
                }
            } catch (e: Exception) {
                showMessage(R.string.TextDeleteError)
            } finally {
                setBusy(false)
            }
        }
    }

    private fun showMessage(message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.crud_actions, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val closed = currentTask?.let { taskViewModel.isClosed(it) } ?: false
        menu?.findItem(R.id.mnuSave)?.isVisible = !closed
        menu?.findItem(R.id.mnuSave)?.isEnabled = !isBusy
        menu?.findItem(R.id.mnuDelete)?.isVisible = currentTask != null && !closed
        menu?.findItem(R.id.mnuDelete)?.isEnabled = !isBusy
        menu?.findItem(R.id.mnuCancel)?.isEnabled = !isBusy
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (isBusy) return true
        return when (item.itemId) {
            R.id.mnuSave -> {
                if (currentTask == null) saveTask()
                else util.showDialogCondition(this, getString(R.string.TextSaveQuestion)) { saveTask() }
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentTaskId", currentTask?.id)
        outState.putString("title", txtTitle.text.toString())
        outState.putString("description", txtDescription.text.toString())
        outState.putString("notes", txtObservation.text.toString())
        outState.putString("reason", txtCancellationReason.text.toString())
        outState.putString("dueDate", dueDate.toString())
        outState.putString("status", (spStatus.selectedItem as TaskStatus).name)
        outState.putInt("priority", spPriority.selectedItemPosition)
        outState.putInt("person", spAssignTo.selectedItemPosition)
        outState.putString("parentId", selectedParentId)
    }

    private fun restoreForm(state: Bundle?) {
        if (state == null || !state.containsKey("dueDate")) return
        txtTitle.setText(state.getString("title"))
        txtDescription.setText(state.getString("description"))
        txtObservation.setText(state.getString("notes"))
        txtCancellationReason.setText(state.getString("reason"))
        dueDate = LocalDate.parse(state.getString("dueDate"))
        showDueDate()
        val statuses = taskViewModel.getAllowedStatuses(currentTask)
        val position = statuses.indexOfFirst { it.name == state.getString("status") }
        spStatus.setSelection(if (position >= 0) position else 0)
        spPriority.setSelection(state.getInt("priority"))
        spAssignTo.setSelection(state.getInt("person"))
        val parentId = state.getString("parentId")
        if (parentId != null) {
            lifecycleScope.launch {
                selectedParent = taskViewModel.getTaskById(parentId)
                selectedParentId = parentId
                updateParentSpinner()
            }
        } else {
            selectedParent = null
            selectedParentId = null
            updateParentSpinner()
        }
        updateCancellationFields()
    }
}
