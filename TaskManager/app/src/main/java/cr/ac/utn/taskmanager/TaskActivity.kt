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
    private var loadedTask: Task? = null
    private var peopleList: List<String> = emptyList()
    private var statusOptions: List<TaskStatus> = listOf(TaskStatus.PENDING)

    private var parentOptions: List<Task> = emptyList()
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
        txtCancellationReason = findViewById(R.id.txtCancellationReason_Task)
        lblCancellationReason = findViewById(R.id.lblCancellationReason_Task)
        tvStatusChangeDate = findViewById(R.id.tvStatusChangeDate_Task)

        setupSpinners()
        observeEligibleParents()

        val btnSearch = findViewById<ImageButton>(R.id.btnSearch_Task)
        btnDueDate = findViewById(R.id.btnDueDate_Task)

        resetDate()
        val taskId = intent.getStringExtra(EXTRA_MESSAGE_ID)
        if (!taskId.isNullOrBlank()) {
            txtId.setText(taskId)
            searchTask(taskId)
        } else {
            showNextId()
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
        menu?.findItem(R.id.mnuDelete)?.isVisible = isEditMode
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

    private fun setupSpinners() {
        fillStatusSpinner(null)

        // show the cancellation reason only when the status is CANCELLED
        spStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                showCancellationReason()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        val priorityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TaskPriority.entries.toTypedArray()
        )
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPriority.adapter = priorityAdapter

        peopleList = util.loadPeopleFromAssets(this)
        val assignAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            peopleList
        )
        assignAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spAssignTo.adapter = assignAdapter

        fillParentSpinner()
    }

    private fun fillStatusSpinner(current: TaskStatus?) {
        statusOptions = if (current == null) {
            listOf(TaskStatus.PENDING)
        } else {
            listOf(current) + current.nextStatuses()
        }
        val statusAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            statusOptions
        )
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spStatus.adapter = statusAdapter
        spStatus.isEnabled = current != null && !current.isClosed()
    }

    private fun showCancellationReason() {
        val selected = spStatus.selectedItem as? TaskStatus
        val visibility = if (selected == TaskStatus.CANCELLED) View.VISIBLE else View.GONE
        lblCancellationReason.visibility = visibility
        txtCancellationReason.visibility = visibility
    }

    private fun observeEligibleParents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.eligibleParentTasks.collect { parents ->
                    parentOptions = parents
                    fillParentSpinner()
                }
            }
        }
    }

    private fun fillParentSpinner() {
        // keep the parent that was selected before refreshing the list
        val keepId = if (loadedTask != null) {
            loadedTask?.parentId
        } else {
            parentIds.getOrNull(spParentTask.selectedItemPosition)
        }

        val ids = mutableListOf<String?>(null)
        val names = mutableListOf(getString(R.string.TextNoParent))
        for (parent in parentOptions) {
            // a task can not be its own parent
            if (parent.id == loadedTask?.id) continue
            ids.add(parent.id)
            names.add("${parent.id} - ${parent.title}")
        }
        if (keepId != null && !ids.contains(keepId)) {
            ids.add(keepId)
            names.add("${getString(R.string.TextId)} $keepId")
        }
        parentIds = ids

        val parentAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            names
        )
        parentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spParentTask.adapter = parentAdapter
        spParentTask.setSelection(ids.indexOf(keepId).coerceAtLeast(0))
        // the parent is only chosen when the task is created
        spParentTask.isEnabled = !isEditMode
    }

    private fun showNextId() {
        lifecycleScope.launch {
            txtId.setText(taskViewModel.getNextId())
        }
    }

    private fun setFieldsEnabled(enabled: Boolean) {
        txtTitle.isEnabled = enabled
        txtDescription.isEnabled = enabled
        txtObservation.isEnabled = enabled
        txtCancellationReason.isEnabled = enabled
        spPriority.isEnabled = enabled
        spAssignTo.isEnabled = enabled
        btnDueDate.isEnabled = enabled
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
                    loadedTask = task
                    txtId.setText(task.id)
                    txtId.isEnabled = false
                    txtTitle.setText(task.title)
                    txtDescription.setText(task.description)
                    spPriority.setSelection(task.priority.ordinal)
                    spAssignTo.setSelection(peopleList.indexOf(task.assignedTo).coerceAtLeast(0))
                    txtObservation.setText(task.notes)
                    txtCancellationReason.setText(task.cancellationReason)

                    fillStatusSpinner(task.status)
                    spStatus.setSelection(0)
                    showCancellationReason()
                    fillParentSpinner()

                    setFieldsEnabled(!task.status.isClosed())

                    val dueDateStr = getDateFormatString(
                        task.dueDate.dayOfMonth,
                        task.dueDate.monthValue,
                        task.dueDate.year
                    )
                    tvDueDate.text = dueDateStr
                    dueDay = task.dueDate.dayOfMonth
                    dueMonth = task.dueDate.monthValue
                    dueYear = task.dueDate.year

                    val changeDate = task.statusChangeDate.format(
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                    )
                    tvStatusChangeDate.text = getString(R.string.TextStatusChangeDate, changeDate)
                    invalidateOptionsMenu()
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

    private fun cleanScreen() {
        resetDate()
        isEditMode = false
        loadedTask = null
        setFieldsEnabled(true)
        txtId.isEnabled = true
        txtId.setText("")
        txtTitle.setText("")
        txtDescription.setText("")
        fillStatusSpinner(null)
        showCancellationReason()
        spPriority.setSelection(0)
        spAssignTo.setSelection(0)
        spParentTask.setSelection(0)
        fillParentSpinner()
        txtObservation.setText("")
        txtCancellationReason.setText("")
        tvDueDate.text = ""
        tvStatusChangeDate.text = ""
        showNextId()
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

    private fun showMessage(message: String) {
        Toast.makeText(this@TaskActivity, message, Toast.LENGTH_LONG).show()
    }

    private fun saveTask() {
        if (!isValidationData()) {
            showMessage(getString(R.string.TextDataIncomplete))
            return
        }

        val newStatus = spStatus.selectedItem as TaskStatus
        val reason = txtCancellationReason.text.toString().trim()
        if (newStatus == TaskStatus.CANCELLED && reason.isBlank()) {
            showMessage(getString(R.string.TextReasonRequired))
            return
        }

        val dueDate = util.parseStringToDateModern(
            tvDueDate.text.toString(),
            "dd/MM/yyyy"
        )
        if (dueDate == null) {
            showMessage(getString(R.string.TextDataIncomplete))
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
                    showMessage(getString(R.string.TextDataNoFound))
                    return@launch
                }

                if (existingTask != null) {
                    if (existingTask.status.isClosed()) {
                        progressDialog.dismiss()
                        showMessage(getString(R.string.TextTaskClosed))
                        return@launch
                    }
                    if (!existingTask.status.canMoveTo(newStatus)) {
                        progressDialog.dismiss()
                        showMessage(getString(R.string.TextInvalidStatusChange))
                        return@launch
                    }
                    if (existingTask.status != newStatus
                        && newStatus == TaskStatus.IN_PROGRESS
                        && dueDate.isBefore(LocalDate.now())) {
                        progressDialog.dismiss()
                        showMessage(getString(R.string.TextDueDateExpired))
                        return@launch
                    }
                }

                val taskId = existingTask?.id ?: taskViewModel.getNextId()
                val statusChanged = existingTask == null || existingTask.status != newStatus
                val changeDate = if (statusChanged) {
                    LocalDateTime.now()
                } else {
                    existingTask!!.statusChangeDate
                }

                val parentId = if (existingTask != null) {
                    existingTask.parentId
                } else {
                    parentIds.getOrNull(spParentTask.selectedItemPosition)
                }

                val task = Task(
                    id = taskId,
                    title = txtTitle.text.toString().trim(),
                    description = txtDescription.text.toString().trim(),
                    dueDate = dueDate,
                    creationDate = existingTask?.creationDate ?: LocalDate.now(),
                    status = newStatus,
                    assignedTo = spAssignTo.selectedItem?.toString() ?: "",
                    priority = spPriority.selectedItem as TaskPriority,
                    notes = txtObservation.text.toString().trim(),
                    parentId = parentId,
                    cancellationReason = if (newStatus == TaskStatus.CANCELLED) reason else "",
                    statusChangeDate = changeDate
                )

                val onCompleteCallback = {
                    progressDialog.dismiss()
                    cleanScreen()
                    showMessage(getString(R.string.TextSaveSuccess))
                }

                if (!isEditMode) {
                    taskViewModel.saveTask(task, onCompleteCallback)
                } else {
                    taskViewModel.updateTask(task, onCompleteCallback)
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                showMessage(e.message.toString())
            }
        }
    }

    private fun deleteTask() {
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