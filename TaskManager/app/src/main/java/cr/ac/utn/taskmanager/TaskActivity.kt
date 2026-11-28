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
import androidx.core.view.isVisible
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
    private lateinit var tvDueDate: TextView
    private lateinit var lblCancellationReason: TextView
    private lateinit var txtCancellationReason: EditText
    private lateinit var tvStatusChangeDate: TextView
    private lateinit var btnDueDate: ImageButton
    private var dueDay: Int = 0
    private var dueMonth: Int = 0
    private var dueYear: Int = 0
    private var isEditMode: Boolean = false
    private var isReadOnly: Boolean = false
    private var currentTask: Task? = null
    private var peopleList: List<String> = emptyList()
    private var parentTasks: List<Task> = emptyList()

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
        lblCancellationReason = findViewById(R.id.lblCancellationReason_Task)
        txtCancellationReason = findViewById(R.id.txtCancellationReason_Task)
        tvStatusChangeDate = findViewById(R.id.tvStatusChangeDate_Task)
        btnDueDate = findViewById(R.id.btnDueDate_Task)

        setupSpinners()
        observeEligibleParents()

        val btnSearch = findViewById<ImageButton>(R.id.btnSearch_Task)

        resetDate()
        val taskId = intent.getStringExtra(EXTRA_MESSAGE_ID)
        if (!taskId.isNullOrBlank()) {
            txtId.setText(taskId)
            searchTask(taskId)
        } else {
            setNextId()
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
        menu?.findItem(R.id.mnuSave)?.isVisible = !isReadOnly
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
        val priorityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TaskPriority.entries.toTypedArray()
        )
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPriority.adapter = priorityAdapter

        peopleList = util.loadPeopleFromAssets(this)
        val peopleAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            peopleList
        )
        peopleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spAssignTo.adapter = peopleAdapter

        setStatusOptions(listOf(TaskStatus.PENDING))
        spStatus.isEnabled = false

        spStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                showCancellationReason()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        showCancellationReason()
    }

    private fun setStatusOptions(statuses: List<TaskStatus>) {
        val statusAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            statuses
        )
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spStatus.adapter = statusAdapter
    }

    private fun getAllowedStatuses(currentStatus: TaskStatus): List<TaskStatus> {
        return when (currentStatus) {
            TaskStatus.PENDING -> listOf(TaskStatus.PENDING, TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED)
            TaskStatus.IN_PROGRESS -> listOf(TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED, TaskStatus.CANCELLED)
            TaskStatus.COMPLETED -> listOf(TaskStatus.COMPLETED)
            TaskStatus.CANCELLED -> listOf(TaskStatus.CANCELLED)
        }
    }

    private fun showCancellationReason() {
        val isCancelled = spStatus.selectedItem == TaskStatus.CANCELLED
        lblCancellationReason.isVisible = isCancelled
        txtCancellationReason.isVisible = isCancelled
    }

    private fun observeEligibleParents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.eligibleParentTasks.collect { parents ->

                    parentTasks = parents.filter { it.id != currentTask?.id }

                    val parentOptions = mutableListOf(getString(R.string.TextParentTaskNone))
                    parentTasks.forEach { parentOptions.add("${it.id} - ${it.title}") }

                    val parentAdapter = ArrayAdapter(
                        this@TaskActivity,
                        android.R.layout.simple_spinner_item,
                        parentOptions
                    )
                    parentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spParentTask.adapter = parentAdapter

                    updateParentSpinnerSelection()
                }
            }
        }
    }

    private fun updateParentSpinnerSelection() {
        val parentId = currentTask?.parentTaskId
        if (parentId == null) {
            spParentTask.setSelection(0)
        } else {
            val index = parentTasks.indexOfFirst { it.id == parentId }
            spParentTask.setSelection(if (index >= 0) index + 1 else 0)
        }
    }

    private fun setNextId() {
        lifecycleScope.launch {
            txtId.setText(taskViewModel.getNextId())
        }
    }

    private fun setFieldsEnabled(enabled: Boolean) {
        txtTitle.isEnabled = enabled
        txtDescription.isEnabled = enabled
        spPriority.isEnabled = enabled
        spAssignTo.isEnabled = enabled
        spParentTask.isEnabled = enabled
        txtObservation.isEnabled = enabled
        txtCancellationReason.isEnabled = enabled
        btnDueDate.isEnabled = enabled
    }

    private fun showStatusChangeDate(date: LocalDateTime?) {
        if (date == null) {
            tvStatusChangeDate.text = ""
        } else {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            tvStatusChangeDate.text = getString(R.string.TextStatusChangeDate, date.format(formatter))
        }
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
                    currentTask = task
                    isEditMode = true
                    isReadOnly = task.status == TaskStatus.COMPLETED
                            || task.status == TaskStatus.CANCELLED
                    txtId.isEnabled = false
                    txtTitle.setText(task.title)
                    txtDescription.setText(task.description)

                    setStatusOptions(getAllowedStatuses(task.status))
                    spStatus.setSelection(0)
                    spStatus.isEnabled = !isReadOnly

                    spPriority.setSelection(task.priority.ordinal)
                    val personIndex = peopleList.indexOf(task.assignedTo)
                    spAssignTo.setSelection(if (personIndex >= 0) personIndex else 0)

                    txtObservation.setText(task.notes)
                    txtCancellationReason.setText(task.cancellationReason ?: "")
                    showCancellationReason()
                    showStatusChangeDate(task.statusChangeDate)

                    val dueDateStr = getDateFormatString(
                        task.dueDate.dayOfMonth,
                        task.dueDate.monthValue,
                        task.dueDate.year
                    )
                    tvDueDate.text = dueDateStr
                    dueDay = task.dueDate.dayOfMonth
                    dueMonth = task.dueDate.monthValue
                    dueYear = task.dueDate.year

                    updateParentSpinnerSelection()
                    setFieldsEnabled(!isReadOnly)
                    invalidateOptionsMenu()

                    if (isReadOnly) {
                        Toast.makeText(
                            this@TaskActivity,
                            getString(R.string.TextTaskReadOnly),
                            Toast.LENGTH_LONG
                        ).show()
                    }
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
        isReadOnly = false
        currentTask = null
        txtId.isEnabled = true
        txtId.setText("")
        txtTitle.setText("")
        txtDescription.setText("")
        setStatusOptions(listOf(TaskStatus.PENDING))
        spStatus.isEnabled = false
        spPriority.setSelection(0)
        spAssignTo.setSelection(0)
        spParentTask.setSelection(0)
        txtObservation.setText("")
        txtCancellationReason.setText("")
        showCancellationReason()
        tvDueDate.text = ""
        tvStatusChangeDate.text = ""
        setFieldsEnabled(true)
        setNextId()
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
        if (isReadOnly) {
            Toast.makeText(
                this@TaskActivity,
                getString(R.string.TextTaskReadOnly),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!isValidationData()) {
            Toast.makeText(
                this@TaskActivity,
                getString(R.string.TextIncompleteData),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val selectedStatus = spStatus.selectedItem as TaskStatus
        val cancellationReason = txtCancellationReason.text.toString().trim()

        if (selectedStatus == TaskStatus.CANCELLED && cancellationReason.isBlank()) {
            Toast.makeText(
                this@TaskActivity,
                getString(R.string.TextCancellationReasonRequired),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val dueDate = util.parseStringToDateModern(
            tvDueDate.text.toString(),
            "dd/MM/yyyy"
        )
        if (dueDate == null) {
            Toast.makeText(
                this@TaskActivity,
                getString(R.string.TextIncompleteData),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val previousStatus = currentTask?.status
        if (selectedStatus == TaskStatus.IN_PROGRESS
            && previousStatus != TaskStatus.IN_PROGRESS
            && dueDate.isBefore(LocalDate.now())) {
            Toast.makeText(
                this@TaskActivity,
                getString(R.string.TextDueDateExpired),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val progressDialog = util.showProgressDialog(this, getString(R.string.TextSaving))
        lifecycleScope.launch {
            try {
                val taskId = if (isEditMode) {
                    txtId.text.toString().trim()
                } else {
                    taskViewModel.getNextId()
                }

                val statusChangeDate = if (selectedStatus != previousStatus) {
                    LocalDateTime.now()
                } else {
                    currentTask?.statusChangeDate
                }

                val parentPosition = spParentTask.selectedItemPosition
                val parentTaskId = if (parentPosition > 0) parentTasks[parentPosition - 1].id else null

                val task = Task(
                    taskId,
                    txtTitle.text.toString().trim(),
                    txtDescription.text.toString().trim(),
                    dueDate,
                    currentTask?.creationDate ?: LocalDate.now(),
                    selectedStatus,
                    spAssignTo.selectedItem?.toString() ?: "",
                    spPriority.selectedItem as TaskPriority,
                    txtObservation.text.toString().trim(),
                    if (parentTaskId != taskId) parentTaskId else null,
                    if (selectedStatus == TaskStatus.CANCELLED) cancellationReason else null,
                    statusChangeDate
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