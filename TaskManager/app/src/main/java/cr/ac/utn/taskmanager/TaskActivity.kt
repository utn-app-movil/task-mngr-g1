package cr.ac.utn.taskmanager

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
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
    private lateinit var lblCancellationReason: TextView
    private lateinit var txtCancellationReason: EditText
    private lateinit var tvDueDate: TextView
    private var dueDay: Int = 0
    private var dueMonth: Int = 0
    private var dueYear: Int = 0
    private var isEditMode: Boolean = false

    private var parentTaskList: List<Task> = emptyList()
    private var currentTaskParentId: Long? = null
    private var peopleList: List<String> = emptyList()

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
        lblCancellationReason = findViewById(R.id.lblCancellationReason_Task)
        txtCancellationReason = findViewById(R.id.txtCancellationReason_Task)
        spAssignTo = findViewById(R.id.spAssignTo_Task)
        spParentTask = findViewById(R.id.spParentTask_Task)
        tvDueDate = findViewById(R.id.tvDueDate_Task)

        setupSpinners()
        observeEligibleParents()

        val btnSearch = findViewById<ImageButton>(R.id.btnSearch_Task)
        val btnDueDate = findViewById<ImageButton>(R.id.btnDueDate_Task)

        cleanScreen()
        val taskIdStr = intent.getStringExtra(EXTRA_MESSAGE_ID)
        if (!taskIdStr.isNullOrBlank()) {
            val taskId = taskIdStr.toLongOrNull()
            if (taskId != null) {
                txtId.setText(taskId.toString())
                searchTask(taskId)
            }
        }

        btnSearch.setOnClickListener {
            val idVal = txtId.text.trim().toString().toLongOrNull()
            if (idVal != null) {
                searchTask(idVal)
            } else {
                Toast.makeText(this, "ID inválido", Toast.LENGTH_SHORT).show()
            }
        }

        btnDueDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
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
        val statusAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TaskStatus.entries.toTypedArray()
        )
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spStatus.adapter = statusAdapter

        spStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedStatus = parent?.getItemAtPosition(position) as? TaskStatus
                if (selectedStatus == TaskStatus.CANCELLED) {
                    lblCancellationReason.visibility = View.VISIBLE
                    txtCancellationReason.visibility = View.VISIBLE
                } else {
                    lblCancellationReason.visibility = View.GONE
                    txtCancellationReason.visibility = View.GONE
                    txtCancellationReason.setText("")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
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
    }

    private fun observeEligibleParents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.eligibleParentTasks.collect { parents ->
                    parentTaskList = parents
                    val parentOptions = mutableListOf(getString(R.string.TextNoneParent))
                    parentOptions.addAll(parents.map { "${it.id} - ${it.title}" })

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
        if (currentTaskParentId != null && parentTaskList.isNotEmpty()) {
            val index = parentTaskList.indexOfFirst { it.id == currentTaskParentId }
            if (index != -1) {
                spParentTask.setSelection(index + 1) // +1 because index 0 is "Ninguna"
            } else {
                spParentTask.setSelection(0)
            }
        } else {
            spParentTask.setSelection(0)
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

    private fun setFieldsEnabled(enabled: Boolean) {
        txtTitle.isEnabled = enabled
        txtDescription.isEnabled = enabled
        spStatus.isEnabled = enabled
        spPriority.isEnabled = enabled
        spAssignTo.isEnabled = enabled
        spParentTask.isEnabled = enabled
        txtObservation.isEnabled = enabled
        txtCancellationReason.isEnabled = enabled
    }

    private fun searchTask(id: Long) {
        val progressDialog = util.showProgressDialog(this, getString(R.string.TextLoading))
        lifecycleScope.launch {
            try {
                val task = taskViewModel.getTaskById(id)
                if (task != null) {
                    isEditMode = true
                    txtId.isEnabled = false
                    txtId.setText(task.id.toString())
                    txtTitle.setText(task.title)
                    txtDescription.setText(task.description)
                    spStatus.setSelection(task.status.ordinal)
                    spPriority.setSelection(task.priority.ordinal)

                    val cancelReason = task.cancellationReason ?: ""
                    txtCancellationReason.setText(cancelReason)
                    if (task.status == TaskStatus.CANCELLED) {
                        lblCancellationReason.visibility = View.VISIBLE
                        txtCancellationReason.visibility = View.VISIBLE
                    } else {
                        lblCancellationReason.visibility = View.GONE
                        txtCancellationReason.visibility = View.GONE
                    }

                    val assignIndex = peopleList.indexOf(task.assignedTo)
                    if (assignIndex != -1) {
                        spAssignTo.setSelection(assignIndex)
                    } else {
                        spAssignTo.setSelection(0)
                    }

                    if (task.status == TaskStatus.COMPLETED || task.status == TaskStatus.CANCELLED) {
                        setFieldsEnabled(false)
                        Toast.makeText(
                            this@TaskActivity,
                            "Una tarea completed o cancelled no se puede modificar",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        setFieldsEnabled(true)
                        txtId.isEnabled = false
                    }

                    currentTaskParentId = task.parentId
                    updateParentSpinnerSelection()

                    txtObservation.setText(task.notes)
                    val dueDateStr = getDateFormatString(
                        task.dueDate.dayOfMonth,
                        task.dueDate.monthValue,
                        task.dueDate.year
                    )
                    tvDueDate.text = dueDateStr
                    dueDay = task.dueDate.dayOfMonth
                    dueMonth = task.dueDate.monthValue
                    dueYear = task.dueDate.year
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
        txtId.isEnabled = false
        txtId.setText("") // Auto-generated ID for new tasks
        txtTitle.setText("")
        txtDescription.setText("")
        spStatus.setSelection(TaskStatus.PENDING.ordinal)
        spStatus.isEnabled = false // New tasks must be inserted in PENDING state
        spPriority.setSelection(0)
        spAssignTo.setSelection(0)
        currentTaskParentId = null
        spParentTask.setSelection(0)
        txtObservation.setText("")
        txtCancellationReason.setText("")
        lblCancellationReason.visibility = View.GONE
        txtCancellationReason.visibility = View.GONE
        tvDueDate.text = ""
        setFieldsEnabled(true)
        spStatus.isEnabled = false
        txtId.isEnabled = false
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
                getString(R.string.TextIncompleteData),
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val taskId = if (isEditMode) {
            txtId.text.toString().trim().toLongOrNull() ?: 0L
        } else {
            0L // Auto-generated by Room
        }

        val selectedStatus = spStatus.selectedItem as TaskStatus
        val cancellationReasonStr = txtCancellationReason.text.toString().trim()

        if (selectedStatus == TaskStatus.CANCELLED && cancellationReasonStr.isBlank()) {
            Toast.makeText(
                this@TaskActivity,
                "Cuando una tarea se mueve a cancelled debe indicarse la razón de la cancelación",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!isEditMode) {
            if (selectedStatus != TaskStatus.PENDING) {
                Toast.makeText(
                    this@TaskActivity,
                    "Las tareas deben insertarse en estado pending",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        val selectedParentIndex = spParentTask.selectedItemPosition
        val parentId = if (selectedParentIndex > 0 && selectedParentIndex - 1 < parentTaskList.size) {
            parentTaskList[selectedParentIndex - 1].id
        } else {
            null
        }

        if (parentId == taskId && isEditMode) {
            Toast.makeText(
                this@TaskActivity,
                "Una tarea no puede ser subtarea de sí misma",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val progressDialog = util.showProgressDialog(this, getString(R.string.TextSaving))
        lifecycleScope.launch {
            try {
                val existingTask = if (isEditMode) taskViewModel.getTaskById(taskId) else null
                val dueDate = util.parseStringToDateModern(
                    tvDueDate.text.toString(),
                    "dd/MM/yyyy"
                )

                val isMovingToInProgress = selectedStatus == TaskStatus.IN_PROGRESS && (existingTask == null || existingTask.status != TaskStatus.IN_PROGRESS)
                if (isMovingToInProgress && dueDate != null && dueDate.isBefore(LocalDate.now())) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@TaskActivity,
                        "Una tarea no se puede mover a in progress si la fecha de vencimiento es menor a la fecha actual",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                if (isEditMode) {
                    if (existingTask != null && (existingTask.status == TaskStatus.COMPLETED || existingTask.status == TaskStatus.CANCELLED)) {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@TaskActivity,
                            "Una tarea completed o cancelled no se pueden modificar",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                    if (existingTask != null && existingTask.status == TaskStatus.PENDING && selectedStatus == TaskStatus.COMPLETED) {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@TaskActivity,
                            "Una tarea pending solo puede moverse a cancelled o in progress",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                    if (existingTask != null && existingTask.status == TaskStatus.IN_PROGRESS && selectedStatus == TaskStatus.PENDING) {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@TaskActivity,
                            "Una tarea in progress no puede moverse a pending",
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                }

                val statusChanged = existingTask == null || existingTask.status != selectedStatus
                val statusChangeDateTime = if (statusChanged) {
                    LocalDateTime.now()
                } else {
                    existingTask.lastStatusChangeDateTime
                }

                val selectedPriority = spPriority.selectedItem as TaskPriority
                val assignedPerson = spAssignTo.selectedItem?.toString() ?: ""

                val task = Task(
                    id = taskId,
                    title = txtTitle.text.toString().trim(),
                    description = txtDescription.text.toString().trim(),
                    dueDate = LocalDate.of(
                        dueDate?.year!!,
                        dueDate.month!!,
                        dueDate.dayOfMonth
                    ),
                    creationDate = existingTask?.creationDate ?: LocalDate.now(),
                    status = selectedStatus,
                    assignedTo = assignedPerson,
                    priority = selectedPriority,
                    notes = txtObservation.text.toString().trim(),
                    parentId = parentId,
                    cancellationReason = if (selectedStatus == TaskStatus.CANCELLED) cancellationReasonStr else null,
                    lastStatusChangeDateTime = statusChangeDateTime
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
        val id = txtId.text.toString().trim().toLongOrNull()
        if (id == null) return
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
