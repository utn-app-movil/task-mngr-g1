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
    private lateinit var txtCancellationReason: EditText
    private lateinit var lblCancellationReason: TextView
    private lateinit var txtObservation: EditText
    private lateinit var tvDueDate: TextView
    private lateinit var tvStatusChangeDate: TextView

    private var dueDay: Int = 0
    private var dueMonth: Int = 0
    private var dueYear: Int = 0
    private var isEditMode: Boolean = false
    private var loadedTask: Task? = null
    private var parentTasksList: List<Task> = emptyList()

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
        spAssignTo = findViewById(R.id.spAssignTo_Task)
        spParentTask = findViewById(R.id.spParentTask_Task)
        lblCancellationReason = findViewById(R.id.lblCancellationReason_Task)
        txtCancellationReason = findViewById(R.id.txtCancellationReason_Task)
        txtObservation = findViewById(R.id.txtObservation_Task)
        tvDueDate = findViewById(R.id.tvDueDate_Task)
        tvStatusChangeDate = findViewById(R.id.tvStatusChangeDate_Task)

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
        val isCompletedOrCancelled = loadedTask?.status == TaskStatus.COMPLETED || loadedTask?.status == TaskStatus.CANCELLED
        menu?.findItem(R.id.mnuSave)?.isVisible = !isCompletedOrCancelled
        menu?.findItem(R.id.mnuDelete)?.isVisible = isEditMode && !isCompletedOrCancelled
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

        spStatus.setSelection(TaskStatus.PENDING.ordinal)
        spStatus.isEnabled = false

        val priorityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TaskPriority.entries.toTypedArray()
        )
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPriority.adapter = priorityAdapter

        val peopleList = util.loadPeopleFromAssets(this)
        val assignAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            peopleList
        )
        assignAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spAssignTo.adapter = assignAdapter

        spStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedStatus = spStatus.selectedItem as TaskStatus
                if (selectedStatus == TaskStatus.CANCELLED) {
                    lblCancellationReason.visibility = View.VISIBLE
                    txtCancellationReason.visibility = View.VISIBLE
                } else {
                    lblCancellationReason.visibility = View.GONE
                    txtCancellationReason.visibility = View.GONE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun observeEligibleParents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.eligibleParentTasks.collect { parents ->
                    parentTasksList = parents
                    val parentOptions = mutableListOf("Ninguna (Tarea Principal)")
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
        val parentId = loadedTask?.parentTaskId
        if (parentId != null && parentId > 0) {
            val index = parentTasksList.indexOfFirst { it.id == parentId }
            if (index != -1) {
                spParentTask.setSelection(index + 1)
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

    private fun searchTask(id: String) {
        val idStr = id.trim()
        if (idStr.isBlank()) {
            Toast.makeText(this, "Ingrese un ID para buscar", Toast.LENGTH_SHORT).show()
            return
        }
        val idInt = idStr.toIntOrNull()
        if (idInt == null || idInt <= 0) {
            Toast.makeText(this, "El ID debe ser un número entero positivo", Toast.LENGTH_SHORT).show()
            return
        }
        val progressDialog = util.showProgressDialog(this, getString(R.string.TextLoading))
        lifecycleScope.launch {
            try {
                val task = taskViewModel.getTaskById(idInt)
                if (task != null) {
                    loadedTask = task
                    isEditMode = true
                    txtId.isEnabled = false
                    txtId.setText(task.id.toString())
                    txtTitle.setText(task.title)
                    txtDescription.setText(task.description)
                    spStatus.setSelection(task.status.ordinal)
                    spPriority.setSelection(task.priority.ordinal)

                    val peopleList = util.loadPeopleFromAssets(this@TaskActivity)
                    val assignIndex = peopleList.indexOf(task.assignedTo)
                    if (assignIndex != -1) {
                        spAssignTo.setSelection(assignIndex)
                    }

                    txtObservation.setText(task.notes)
                    txtCancellationReason.setText(task.cancellationReason ?: "")

                    val dueDateStr = getDateFormatString(
                        task.dueDate.dayOfMonth,
                        task.dueDate.monthValue,
                        task.dueDate.year
                    )
                    tvDueDate.text = dueDateStr
                    dueDay = task.dueDate.dayOfMonth
                    dueMonth = task.dueDate.monthValue
                    dueYear = task.dueDate.year

                    if (task.statusChangeDateTime != null) {
                        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
                        tvStatusChangeDate.text = "Cambio estado: ${task.statusChangeDateTime.format(formatter)}"
                    } else {
                        tvStatusChangeDate.text = ""
                    }

                    updateParentSpinnerSelection()

                    val isCompletedOrCancelled = task.status == TaskStatus.COMPLETED || task.status == TaskStatus.CANCELLED
                    if (isCompletedOrCancelled) {
                        disableControls()
                    } else {
                        spStatus.isEnabled = true
                    }

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

    private fun disableControls() {
        txtTitle.isEnabled = false
        txtDescription.isEnabled = false
        spStatus.isEnabled = false
        spPriority.isEnabled = false
        spAssignTo.isEnabled = false
        spParentTask.isEnabled = false
        txtObservation.isEnabled = false
        txtCancellationReason.isEnabled = false
        findViewById<ImageButton>(R.id.btnDueDate_Task).isEnabled = false
    }

    private fun getDateFormatString(dayOfMonth: Int, monthValue: Int, yearValue: Int): String {
        return "${if (dayOfMonth < 10) "0" else ""}$dayOfMonth/${if (monthValue < 10) "0" else ""}$monthValue/$yearValue"
    }

    private fun cleanScreen() {
        resetDate()
        isEditMode = false
        loadedTask = null
        txtId.isEnabled = true
        txtId.setText("")
        txtTitle.setText("")
        txtDescription.setText("")

        spStatus.setSelection(TaskStatus.PENDING.ordinal)
        spStatus.isEnabled = false

        spPriority.setSelection(0)
        spAssignTo.setSelection(0)
        spParentTask.setSelection(0)
        txtObservation.setText("")
        txtCancellationReason.setText("")
        tvDueDate.text = ""
        tvStatusChangeDate.text = ""
        lblCancellationReason.visibility = View.GONE
        txtCancellationReason.visibility = View.GONE

        txtTitle.isEnabled = true
        txtDescription.isEnabled = true
        spPriority.isEnabled = true
        spAssignTo.isEnabled = true
        spParentTask.isEnabled = true
        txtObservation.isEnabled = true
        txtCancellationReason.isEnabled = true
        findViewById<ImageButton>(R.id.btnDueDate_Task).isEnabled = true

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

        val typedIdStr = txtId.text.toString().trim()
        val typedIdInt = typedIdStr.toIntOrNull()

        if (typedIdInt != null && typedIdInt <= 0) {
            Toast.makeText(this, "El ID debe ser mayor a 0", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedStatus = spStatus.selectedItem as TaskStatus
        val selectedPriority = spPriority.selectedItem as TaskPriority
        val dueDate = util.parseStringToDateModern(
            tvDueDate.text.toString(),
            "dd/MM/yyyy"
        ) ?: LocalDate.now()

        if (isEditMode && loadedTask != null) {
            val currentStatus = loadedTask!!.status
            if (currentStatus == TaskStatus.COMPLETED || currentStatus == TaskStatus.CANCELLED) {
                Toast.makeText(this, "Tarea finalizada no se puede modificar", Toast.LENGTH_LONG).show()
                return
            }

            if (currentStatus == TaskStatus.IN_PROGRESS && selectedStatus == TaskStatus.PENDING) {
                Toast.makeText(this, "No puede pasar de En Progreso a Pendiente", Toast.LENGTH_LONG).show()
                return
            }
        }

        val finalStatus = if (!isEditMode) TaskStatus.PENDING else selectedStatus

        if (finalStatus == TaskStatus.IN_PROGRESS && dueDate.isBefore(LocalDate.now())) {
            Toast.makeText(this, "Fecha de vencimiento es menor a la actual", Toast.LENGTH_LONG).show()
            return
        }

        val reason = txtCancellationReason.text.toString().trim()
        if (finalStatus == TaskStatus.CANCELLED && reason.isBlank()) {
            Toast.makeText(this, "Debe indicar la razón de la cancelación", Toast.LENGTH_LONG).show()
            return
        }

        val changeDate = if (!isEditMode || loadedTask?.status != finalStatus) {
            LocalDateTime.now()
        } else {
            loadedTask?.statusChangeDateTime
        }

        val parentPos = spParentTask.selectedItemPosition
        val parentId = if (parentPos > 0 && parentPos - 1 < parentTasksList.size) {
            parentTasksList[parentPos - 1].id
        } else {
            null
        }

        lifecycleScope.launch {
            val progressDialog = util.showProgressDialog(this@TaskActivity, getString(R.string.TextSaving))
            try {
                if (!isEditMode && typedIdInt != null) {
                    val existing = taskViewModel.getTaskById(typedIdInt)
                    if (existing != null) {
                        progressDialog.dismiss()
                        Toast.makeText(this@TaskActivity, "El ID $typedIdInt ya existe. Use la búsqueda para modificarlo.", Toast.LENGTH_LONG).show()
                        return@launch
                    }
                }

                val taskId = if (isEditMode && loadedTask != null) loadedTask!!.id else 0

                val task = Task(
                    id = taskId,
                    title = txtTitle.text.toString().trim(),
                    description = txtDescription.text.toString().trim(),
                    dueDate = dueDate,
                    creationDate = loadedTask?.creationDate ?: LocalDate.now(),
                    status = finalStatus,
                    assignedTo = spAssignTo.selectedItem.toString(),
                    priority = selectedPriority,
                    notes = txtObservation.text.toString().trim(),
                    parentTaskId = parentId,
                    cancellationReason = if (finalStatus == TaskStatus.CANCELLED) reason else null,
                    statusChangeDateTime = changeDate
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
                Toast.makeText(this@TaskActivity, e.message.toString(), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun deleteTask() {
        val idInt = txtId.text.toString().trim().toIntOrNull()
        if (idInt == null) return
        val progressDialog = util.showProgressDialog(this, getString(R.string.TextDeleting))
        taskViewModel.removeTask(idInt) {
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