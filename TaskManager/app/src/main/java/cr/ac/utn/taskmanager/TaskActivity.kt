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
        val taskIdStr = intent.getStringExtra(EXTRA_MESSAGE_ID)
        if (!taskIdStr.isNullOrBlank()) {
            txtId.setText(taskIdStr)
            val idLong = taskIdStr.toLongOrNull()
            if (idLong != null) {
                searchTask(idLong)
            }
        }

        btnSearch.setOnClickListener {
            val idText = txtId.text.trim().toString()
            val idLong = idText.toLongOrNull()
            if (idLong != null) {
                searchTask(idLong)
            }
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

    private var eligibleParentsList: List<Task> = emptyList()

    private fun setupSpinners() {
        val statusAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TaskStatus.entries.toTypedArray()
        )
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spStatus.adapter = statusAdapter

        val priorityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TaskPriority.entries.toTypedArray()
        )
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPriority.adapter = priorityAdapter

        val people = util.loadPeopleFromAssets(this, "people.json")
        val assignAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            if (people.isNotEmpty()) people else listOf("Sin asignar")
        )
        assignAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spAssignTo.adapter = assignAdapter
    }

    private fun observeEligibleParents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.eligibleParentTasks.collect { parents ->
                    eligibleParentsList = parents
                    val parentOptions = mutableListOf("Ninguna (Tarea Principal)")
                    parentOptions.addAll(parents.map { "${it.id}: ${it.title}" })

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

    private fun updateParentSpinnerSelection() {}

    private fun resetDate() {
        val calendar = Calendar.getInstance()
        dueYear = calendar.get(Calendar.YEAR)
        dueMonth = calendar.get(Calendar.MONTH) + 1
        dueDay = calendar.get(Calendar.DAY_OF_MONTH)
        tvDueDate.text = getDateFormatString(dueDay, dueMonth, dueYear)
    }

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(this, this, dueYear, dueMonth - 1, dueDay)
        datePickerDialog.show()
    }

    private fun searchTask(id: Long) {
        val progressDialog = util.showProgressDialog(this, getString(R.string.TextLoading))
        lifecycleScope.launch {
            try {
                val task = taskViewModel.getTaskById(id)
                if (task != null) {
                    isEditMode = true
                    txtId.isEnabled = false
                    txtTitle.setText(task.title)
                    txtDescription.setText(task.description)
                    spStatus.setSelection(task.status.ordinal)
                    spPriority.setSelection(task.priority.ordinal)

                    // Set assignee selection if matches
                    val adapter = spAssignTo.adapter
                    if (adapter != null) {
                        for (i in 0 until adapter.count) {
                            if (adapter.getItem(i).toString() == task.assignedTo) {
                                spAssignTo.setSelection(i)
                                break
                            }
                        }
                    }

                    // Set parent task selection
                    if (task.parentId != null) {
                        val index = eligibleParentsList.indexOfFirst { it.id == task.parentId }
                        if (index >= 0) {
                            spParentTask.setSelection(index + 1)
                        }
                    } else {
                        spParentTask.setSelection(0)
                    }

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
        txtId.isEnabled = true
        txtId.setText("")
        txtTitle.setText("")
        txtDescription.setText("")
        spStatus.setSelection(0)
        spPriority.setSelection(0)
        if (spAssignTo.adapter != null && spAssignTo.adapter.count > 0) {
            spAssignTo.setSelection(0)
        }
        if (spParentTask.adapter != null && spParentTask.adapter.count > 0) {
            spParentTask.setSelection(0)
        }
        txtObservation.setText("")
        invalidateOptionsMenu()
    }

    private fun isValidationData(): Boolean {
        return txtTitle.text.toString().trim().isNotBlank()
                && txtDescription.text.toString().trim().isNotBlank()
                && spStatus.selectedItem != null
                && spPriority.selectedItem != null
                && tvDueDate.text.toString().trim().isNotBlank()
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

        val progressDialog = util.showProgressDialog(this, getString(R.string.TextSaving))
        lifecycleScope.launch {
            try {
                val selectedStatus = spStatus.selectedItem as TaskStatus
                val selectedPriority = spPriority.selectedItem as TaskPriority
                val dueDate = util.parseStringToDateModern(
                    tvDueDate.text.toString(),
                    "dd/MM/yyyy"
                )
                val parsedDueDate = LocalDate.of(dueDate?.year!!, dueDate.month!!, dueDate.dayOfMonth)

                val taskIdText = txtId.text.toString().trim()
                val taskId = if (isEditMode && taskIdText.isNotEmpty()) taskIdText.toLong() else 0L

                val selectedParentPosition = spParentTask.selectedItemPosition
                val parentId = if (selectedParentPosition > 0 && selectedParentPosition - 1 < eligibleParentsList.size) {
                    eligibleParentsList[selectedParentPosition - 1].id
                } else {
                    null
                }

                val cancellationReasonInput =
                    findViewById<EditText>(R.id.txtCancellationReason_Task).text.toString().trim()

                if (isEditMode) {
                    val originalTask = taskViewModel.getTaskById(taskId)
                    if (originalTask != null) {
                        // Req 3: Una tarea completed o cancelled no se pueden modificar
                        if (originalTask.status == TaskStatus.COMPLETED || originalTask.status == TaskStatus.CANCELLED) {
                            progressDialog.dismiss()
                            Toast.makeText(this@TaskActivity, "No se puede modificar una tarea Completada o Cancelada", Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        // Req 6: Una tarea in progress no puede moverse a pending
                        if (originalTask.status == TaskStatus.IN_PROGRESS && selectedStatus == TaskStatus.PENDING) {
                            progressDialog.dismiss()
                            Toast.makeText(this@TaskActivity, "Una tarea En Progreso no puede volver a Pendiente", Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        // Req 9: Una tarea no se puede mover a in progress si la fecha de vencimiento es menor a la fecha actual
                        if (selectedStatus == TaskStatus.IN_PROGRESS && parsedDueDate.isBefore(LocalDate.now())) {
                            progressDialog.dismiss()
                            Toast.makeText(this@TaskActivity, "No se puede mover a In Progress si la fecha de vencimiento ya pasó", Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        // Req 8: Cuando una tarea se mueve a cancelled debe indicarse la razón de la cancelación
                        if (selectedStatus == TaskStatus.CANCELLED && cancellationReasonInput.isBlank()) {
                            progressDialog.dismiss()
                            Toast.makeText(this@TaskActivity, "Debe indicar la razón de la cancelación", Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        // Req 9 (Timestamp): Si cambia de estado, guardar la fecha y hora del cambio
                        val statusChangedAt = if (originalTask.status != selectedStatus) {
                            java.time.LocalDateTime.now()
                        } else {
                            originalTask.statusChangedAt
                        }

                        val updatedTask = originalTask.copy(
                            parentId = parentId,
                            title = txtTitle.text.toString().trim(),
                            description = txtDescription.text.toString().trim(),
                            dueDate = parsedDueDate,
                            status = selectedStatus,
                            assignedTo = spAssignTo.selectedItem?.toString() ?: "",
                            priority = selectedPriority,
                            notes = txtObservation.text.toString().trim(),
                            statusChangedAt = statusChangedAt,
                            cancellationReason = if (selectedStatus == TaskStatus.CANCELLED) cancellationReasonInput else originalTask.cancellationReason
                        )

                        taskViewModel.updateTask(updatedTask) {
                            progressDialog.dismiss()
                            cleanScreen()
                            Toast.makeText(this@TaskActivity, getString(R.string.TextSaveSuccess), Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    // Req 2: Las tareas nuevas se insertan en estado PENDING
                    val newCreationDate = LocalDate.now()
                    val newTask = Task(
                        id = 0L, // Room autogenera el Long
                        parentId = parentId,
                        title = txtTitle.text.toString().trim(),
                        description = txtDescription.text.toString().trim(),
                        dueDate = parsedDueDate,
                        creationDate = newCreationDate,
                        status = TaskStatus.PENDING, // Req 2
                        assignedTo = spAssignTo.selectedItem?.toString() ?: "",
                        priority = selectedPriority,
                        notes = txtObservation.text.toString().trim(),
                        statusChangedAt = java.time.LocalDateTime.now(),
                        cancellationReason = null
                    )

                    taskViewModel.saveTask(newTask) {
                        progressDialog.dismiss()
                        cleanScreen()
                        Toast.makeText(this@TaskActivity, getString(R.string.TextSaveSuccess), Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Toast.makeText(this@TaskActivity, e.message.toString(), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun deleteTask() {
        val idText = txtId.text.toString().trim()
        if (idText.isBlank()) return
        val id = idText.toLongOrNull() ?: return
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
