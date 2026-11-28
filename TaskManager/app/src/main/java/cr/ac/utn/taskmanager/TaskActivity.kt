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
    private lateinit var lblCancellationReason: TextView
    private lateinit var txtCancellationReason: EditText
    private lateinit var tvStatusChangedAt: TextView
    private lateinit var tvDueDate: TextView
    private lateinit var btnDueDate: ImageButton

    private var dueDay: Int = 0
    private var dueMonth: Int = 0
    private var dueYear: Int = 0
    private var isEditMode: Boolean = false
    private var currentTask: Task? = null
    private var peopleList: List<String> = emptyList()
    private var eligibleParents: List<Task> = emptyList()
    private var selectedParentTaskId: String? = null

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
        lblCancellationReason = findViewById(R.id.lblCancellationReason_Task)
        txtCancellationReason = findViewById(R.id.txtCancellationReason_Task)
        tvStatusChangedAt = findViewById(R.id.tvStatusChangedAt_Task)
        spAssignTo = findViewById(R.id.spAssignTo_Task)
        spParentTask = findViewById(R.id.spParentTask_Task)
        tvDueDate = findViewById(R.id.tvDueDate_Task)
        btnDueDate = findViewById(R.id.btnDueDate_Task)

        setupSpinners()
        setupStatusListener()
        observeEligibleParents()

        val btnSearch = findViewById<ImageButton>(R.id.btnSearch_Task)

        resetDate()
        val taskId = intent.getStringExtra(EXTRA_MESSAGE_ID)
        if (!taskId.isNullOrBlank()) {
            txtId.setText(taskId)
            searchTask(taskId)
        } else {
            loadNextTaskId()
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
        // Req. 3: No se permite modificar una tarea COMPLETED o CANCELLED
        val editable = !isEditMode || (currentTask?.isEditable == true)
        menu?.findItem(R.id.mnuSave)?.isVisible = editable
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

    private fun loadNextTaskId() {
        // Req. 10: ID incremental automático consecutivo
        lifecycleScope.launch {
            val nextId = taskViewModel.getNextTaskId()
            if (!isEditMode) {
                txtId.setText(nextId)
                txtId.isEnabled = false
            }
        }
    }

    private fun setupSpinners() {
        // Req. 2: Toda tarea nueva se inserta en estado PENDING
        updateStatusSpinner(listOf(TaskStatus.PENDING), TaskStatus.PENDING)
        spStatus.isEnabled = false

        val priorityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TaskPriority.entries.toTypedArray()
        )
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPriority.adapter = priorityAdapter

        // Req. 5: Lista seleccionable de personas asignadas
        peopleList = util.loadPeopleFromAssets(this)
        val assignAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            peopleList
        )
        assignAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spAssignTo.adapter = assignAdapter
    }

    private fun setupStatusListener() {
        spStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = spStatus.selectedItem as? TaskStatus
                // Req. 8: Mostrar etiqueta y campo de razón cuando el estado es CANCELLED
                val visibility = if (selected == TaskStatus.CANCELLED) View.VISIBLE else View.GONE
                lblCancellationReason.visibility = visibility
                txtCancellationReason.visibility = visibility
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateStatusSpinner(statuses: List<TaskStatus>, selected: TaskStatus) {
        val statusAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            statuses
        )
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spStatus.adapter = statusAdapter
        val index = statuses.indexOf(selected)
        if (index >= 0) {
            spStatus.setSelection(index)
        }
    }

    private fun observeEligibleParents() {
        // Req. 1: Cargar tareas padre elegibles (PENDING o IN_PROGRESS)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.eligibleParentTasks.collect { parents ->
                    val currentId = txtId.text.toString().trim()
                    eligibleParents = parents.filter { it.id != currentId }

                    val parentOptions = mutableListOf("Ninguna (Tarea Principal)")
                    parentOptions.addAll(eligibleParents.map { "${it.id} - ${it.title}" })

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
        if (spParentTask.adapter == null || spParentTask.count == 0) return
        val parentId = selectedParentTaskId
        if (parentId.isNullOrBlank()) {
            spParentTask.setSelection(0)
            return
        }
        val index = eligibleParents.indexOfFirst { it.id == parentId }
        spParentTask.setSelection(if (index >= 0) index + 1 else 0)
    }

    private fun setFormEditable(editable: Boolean) {
        txtTitle.isEnabled = editable
        txtDescription.isEnabled = editable
        spStatus.isEnabled = editable && isEditMode
        spPriority.isEnabled = editable
        spAssignTo.isEnabled = editable
        spParentTask.isEnabled = editable
        txtObservation.isEnabled = editable
        txtCancellationReason.isEnabled = editable
        btnDueDate.isEnabled = editable
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
                    txtId.isEnabled = false
                    txtTitle.setText(task.title)
                    txtDescription.setText(task.description)

                    // Req. 4, 6, 7: Mostrar solo estados permitidos según estado actual
                    updateStatusSpinner(task.allowedNextStatuses(), task.status)
                    spPriority.setSelection(task.priority.ordinal)

                    val personIndex = peopleList.indexOf(task.assignedTo)
                    if (personIndex >= 0) {
                        spAssignTo.setSelection(personIndex)
                    }

                    selectedParentTaskId = task.parentTaskid
                    updateParentSpinnerSelection()

                    txtObservation.setText(task.notes)

                    // Req. 8: Mostrar razón de cancelación si la tarea fue cancelada
                    if (!task.cancellationReason.isNullOrBlank()) {
                        txtCancellationReason.setText(task.cancellationReason)
                        lblCancellationReason.visibility = View.VISIBLE
                        txtCancellationReason.visibility = View.VISIBLE
                    } else {
                        txtCancellationReason.setText("")
                        lblCancellationReason.visibility = View.GONE
                        txtCancellationReason.visibility = View.GONE
                    }

                    // Req. 9b: Mostrar fecha y hora del último cambio de estado
                    if (task.statusChangedAt != null) {
                        tvStatusChangedAt.text = "Último cambio de estado: ${task.statusChangedAt.format(dateTimeFormatter)}"
                        tvStatusChangedAt.visibility = View.VISIBLE
                    } else {
                        tvStatusChangedAt.visibility = View.GONE
                    }

                    dueDay = task.dueDate.dayOfMonth
                    dueMonth = task.dueDate.monthValue
                    dueYear = task.dueDate.year
                    tvDueDate.text = getDateFormatString(dueDay, dueMonth, dueYear)

                    // Req. 3: Bloquear edición si está COMPLETED o CANCELLED
                    setFormEditable(task.isEditable)
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
        currentTask = null
        isEditMode = false
        selectedParentTaskId = null
        txtTitle.setText("")
        txtDescription.setText("")
        updateStatusSpinner(listOf(TaskStatus.PENDING), TaskStatus.PENDING)
        if (spPriority.adapter != null && spPriority.count > 0) spPriority.setSelection(0)
        if (spAssignTo.adapter != null && spAssignTo.count > 0) spAssignTo.setSelection(0)
        if (spParentTask.adapter != null && spParentTask.count > 0) spParentTask.setSelection(0)
        txtObservation.setText("")
        txtCancellationReason.setText("")
        lblCancellationReason.visibility = View.GONE
        txtCancellationReason.visibility = View.GONE
        tvStatusChangedAt.text = ""
        tvStatusChangedAt.visibility = View.GONE
        tvDueDate.text = ""
        setFormEditable(true)
        invalidateOptionsMenu()
        loadNextTaskId()
    }

    private fun isValidationData(): Boolean {
        return txtId.text.toString().trim().isNotBlank()
                && txtTitle.text.toString().trim().isNotBlank()
                && txtDescription.text.toString().trim().isNotBlank()
                && spStatus.selectedItem != null
                && spPriority.selectedItem != null
                && tvDueDate.text.toString().trim().isNotBlank()
                && txtObservation.text.toString().trim().isNotBlank()
                && spAssignTo.selectedItem != null
    }

    private fun saveTask() {
        if (!isValidationData()) {
            Toast.makeText(this@TaskActivity, "Datos incompletos", Toast.LENGTH_LONG).show()
            return
        }

        // Req. 3: Bloquear modificación si ya está COMPLETED o CANCELLED
        if (isEditMode && currentTask?.isEditable == false) {
            Toast.makeText(
                this@TaskActivity,
                "No se puede modificar una tarea completada o cancelada",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val selectedStatus = if (!isEditMode) {
            TaskStatus.PENDING // Req. 2
        } else {
            spStatus.selectedItem as TaskStatus
        }

        // Req. 4, 6, 7: Validar transición de estado permitida
        if (isEditMode && currentTask != null && !currentTask!!.canTransitionTo(selectedStatus)) {
            Toast.makeText(this@TaskActivity, "Transición de estado no permitida", Toast.LENGTH_LONG).show()
            return
        }

        // Req. 8: Validar razón de cancelación obligatoria
        val reason = txtCancellationReason.text.toString().trim()
        if (selectedStatus == TaskStatus.CANCELLED && reason.isBlank()) {
            txtCancellationReason.error = "Debe indicar la razón de cancelación"
            Toast.makeText(
                this@TaskActivity,
                "Debe ingresar una razón para cancelar la tarea",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val dueDateLocal = LocalDate.of(dueYear, dueMonth, dueDay)

        // Req. 9a: No permitir iniciar (IN_PROGRESS) si la fecha de vencimiento ya pasó
        if (selectedStatus == TaskStatus.IN_PROGRESS && dueDateLocal.isBefore(LocalDate.now())) {
            Toast.makeText(
                this@TaskActivity,
                "No se puede iniciar una tarea cuya fecha de vencimiento ya pasó",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val selectedParentId = if (spParentTask.selectedItemPosition > 0) {
            eligibleParents.getOrNull(spParentTask.selectedItemPosition - 1)?.id
        } else {
            null
        }

        // Req. 9b: Guardar fecha y hora cuando cambia el estado
        val statusChangedTimestamp = when {
            !isEditMode -> LocalDateTime.now()
            currentTask != null && currentTask!!.status != selectedStatus -> LocalDateTime.now()
            else -> currentTask?.statusChangedAt
        }

        val progressDialog = util.showProgressDialog(this, getString(R.string.TextSaving))
        lifecycleScope.launch {
            try {
                val existingTask = taskViewModel.getTaskById(txtId.text.toString().trim())
                if (existingTask != null && !isEditMode) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@TaskActivity,
                        getString(R.string.TextDuplicateData),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val selectedPriority = spPriority.selectedItem as TaskPriority
                    val task = Task(
                        id = txtId.text.toString().trim(),
                        title = txtTitle.text.toString().trim(),
                        description = txtDescription.text.toString().trim(),
                        dueDate = dueDateLocal,
                        creationDate = existingTask?.creationDate ?: LocalDate.now(),
                        status = selectedStatus,
                        assignedTo = spAssignTo.selectedItem?.toString() ?: "",
                        priority = selectedPriority,
                        notes = txtObservation.text.toString().trim(),
                        parentTaskid = selectedParentId,
                        cancellationReason = if (selectedStatus == TaskStatus.CANCELLED) reason else null,
                        statusChangedAt = statusChangedTimestamp
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