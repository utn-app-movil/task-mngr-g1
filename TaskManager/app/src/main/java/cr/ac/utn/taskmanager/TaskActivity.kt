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
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import android.view.View
import android.widget.AdapterView
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
    private var loadedTask: Task? = null

    private var allTasks: List<Task> = emptyList()

    private lateinit var tvSubtasks: TextView

    private data class ParentOption(
        val id: Long?,
        val label: String
    ) {
        override fun toString(): String = label
    }

    private lateinit var tvStatusChangeDate: TextView

    private lateinit var lblCancellationReason: TextView
    private lateinit var txtCancellationReason: EditText

    private var people: List<String> = emptyList()

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
        tvSubtasks = findViewById(R.id.tvSubtasks_Task)
        tvDueDate = findViewById(R.id.tvDueDate_Task)

        lblCancellationReason =
            findViewById(R.id.lblCancellationReason_Task)

        txtCancellationReason =
            findViewById(R.id.txtCancellationReason_Task)

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
        val readOnly = loadedTask?.status?.isTerminal == true
        menu?.findItem(R.id.mnuSave)?.isVisible = !readOnly
        menu?.findItem(R.id.mnuDelete)?.isVisible = isEditMode && !readOnly
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

        // Estados de las tareas
        setStatusOptions(null)

        spStatus.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    updateCancellationVisibility()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                    updateCancellationVisibility()
                }
            }

        updateCancellationVisibility()

        // Prioridades de las tareas
        val priorityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TaskPriority.entries.toTypedArray()
        )

        priorityAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spPriority.adapter = priorityAdapter

        // Responsables disponibles
        people = util.loadPeopleFromAssets(this)

        val peopleOptions = listOf(
            "Seleccione una persona"
        ) + people

        val peopleAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            peopleOptions
        )

        peopleAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spAssignTo.adapter = peopleAdapter

        spAssignTo.setSelection(0)
    }

    private fun observeEligibleParents() {

        updateParentSpinnerSelection(false)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                taskViewModel.tasks.collect { tasks ->

                    allTasks = tasks

                    updateParentSpinnerSelection()
                    updateSubtasksList()
                }
            }
        }
    }

    private fun updateParentSpinnerSelection(
        preserveSelection: Boolean = true
    ) {

        val selectedId = if (isEditMode) {
            loadedTask?.parentTaskId
        } else if (preserveSelection) {
            (spParentTask.selectedItem as? ParentOption)?.id
        } else {
            null
        }

        val options = mutableListOf(
            ParentOption(
                null,
                "Ninguna (Tarea Principal)"
            )
        )

        if (isEditMode) {

            loadedTask?.parentTaskId?.let { parentId ->

                val parent = allTasks.find {
                    it.id == parentId
                }

                options.add(
                    ParentOption(
                        parentId,
                        parent?.let {
                            "#${it.id} - ${it.title}"
                        } ?: "Tarea padre #$parentId"
                    )
                )
            }

        } else {

            val eligibleParents = allTasks.filter {

                it.status == TaskStatus.PENDING ||
                        it.status == TaskStatus.IN_PROGRESS

            }.sortedBy { it.title }

            eligibleParents.forEach { parent ->

                options.add(
                    ParentOption(
                        parent.id,
                        "#${parent.id} - ${parent.title}"
                    )
                )
            }

            if (selectedId != null &&
                options.none { it.id == selectedId }
            ) {
                options.add(
                    ParentOption(
                        selectedId,
                        "Tarea #$selectedId - No disponible"
                    )
                )
            }
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            options
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spParentTask.adapter = adapter

        val position = options.indexOfFirst {
            it.id == selectedId
        }

        spParentTask.setSelection(
            if (position >= 0) position else 0
        )

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

        val numericId = id.trim().toLongOrNull()

        if (numericId == null || numericId <= 0L) {
            Toast.makeText(
                this,
                "Ingrese un identificador válido",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val progressDialog = util.showProgressDialog(
            this,
            getString(R.string.TextLoading)
        )

        lifecycleScope.launch {
            try {

                val task: Task? =
                    taskViewModel.getTaskById(numericId)

                if (task != null) {

                    isEditMode = true
                    loadedTask = task

                    txtId.setText(task.id.toString())
                    txtId.isEnabled = false
                    txtTitle.setText(task.title)
                    txtDescription.setText(task.description)

                    setStatusOptions(task.status)
                    txtCancellationReason.setText(task.cancellationReason.orEmpty())
                    updateCancellationVisibility()
                    tvStatusChangeDate.text = task.statusChangeDate?.let {
                        "Último cambio: ${it.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))}"
                    } ?: "Sin fecha de cambio registrada"
                    setReadOnly(task.status.isTerminal)
                    updateParentSpinnerSelection()
                    updateSubtasksList()
                    spPriority.setSelection(task.priority.ordinal)

                    val personPosition = people.indexOf(task.assignedTo)
                    spAssignTo.setSelection(
                        if (personPosition >= 0) {
                            personPosition + 1
                        } else {
                            0
                        }
                    )

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
        loadedTask = null
        setReadOnly(false)
        updateParentSpinnerSelection(false)
        updateSubtasksList()
        tvStatusChangeDate.text = ""

        txtId.isEnabled = true
        txtId.setText("")
        txtTitle.setText("")
        txtDescription.setText("")

        setStatusOptions(null)

        spPriority.setSelection(0)
        spAssignTo.setSelection(0)
        txtObservation.setText("")
        txtCancellationReason.setText("")

        tvDueDate.text = ""

        updateCancellationVisibility()

        invalidateOptionsMenu()
    }

    private fun isValidationData(): Boolean {

        val selectedPerson =
            spAssignTo.selectedItem?.toString()

        return txtTitle.text.toString().trim().isNotBlank()
                && txtDescription.text.toString().trim().isNotBlank()
                && spStatus.selectedItem != null
                && spPriority.selectedItem != null
                && tvDueDate.text.toString().trim().isNotBlank()
                && selectedPerson != null
                && selectedPerson in people
    }

    private fun saveTask() {

        if (!isValidationData()) {
            Toast.makeText(
                this,
                "Datos incompletos",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val progressDialog = util.showProgressDialog(
            this,
            getString(R.string.TextSaving)
        )

        lifecycleScope.launch {
            try {

                val existingTask = if (isEditMode) {
                    val id = txtId.text.toString()
                        .trim()
                        .toLongOrNull()
                        ?: throw IllegalArgumentException(
                            "Identificador inválido"
                        )

                    taskViewModel.getTaskById(id)
                        ?: throw IllegalArgumentException(
                            "La tarea no existe"
                        )
                } else {
                    null
                }

                val dueDate = util.parseStringToDateModern(
                    tvDueDate.text.toString(),
                    "dd/MM/yyyy"
                ) ?: throw IllegalArgumentException(
                    "Fecha de vencimiento inválida"
                )

                val selectedStatus = if (isEditMode) {
                    spStatus.selectedItem as TaskStatus
                } else {
                    TaskStatus.PENDING
                }

                val selectedPriority =
                    spPriority.selectedItem as TaskPriority

                val task = Task(
                    id = existingTask?.id ?: 0L,

                    title = txtTitle.text.toString().trim(),

                    description = txtDescription.text.toString().trim(),

                    dueDate = dueDate,

                    creationDate = existingTask?.creationDate
                        ?: LocalDate.now(),

                    status = selectedStatus,

                    assignedTo = spAssignTo.selectedItem
                        ?.toString() ?: "",

                    priority = selectedPriority,

                    notes = txtObservation.text.toString().trim(),

                    cancellationReason = if (
                        selectedStatus == TaskStatus.CANCELLED
                    ) {
                        txtCancellationReason.text.toString().trim()
                    } else {
                        null
                    },

                    statusChangeDate = existingTask?.statusChangeDate,

                    parentTaskId = if (existingTask != null) {

                        existingTask.parentTaskId

                    } else {

                        val selectedParent =
                            spParentTask.selectedItem as? ParentOption
                                ?: throw IllegalArgumentException(
                                    "Seleccione una opción de tarea padre"
                                )

                        selectedParent.id
                    }
                )

                if (isEditMode) {
                    taskViewModel.updateTask(task)
                } else {
                    taskViewModel.saveTask(task)
                }

                cleanScreen()
                Toast.makeText(
                    this@TaskActivity,
                    getString(R.string.TextSaveSuccess),
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(
                    this@TaskActivity,
                    e.message ?: "No se pudo guardar la tarea",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressDialog.dismiss()
            }
        }
    }

    private fun deleteTask() {
        val id = txtId.text.toString().trim().toLongOrNull() ?: return
        if (id <= 0L) return

        val progressDialog = util.showProgressDialog(
            this, getString(R.string.TextDeleting)
        )
        lifecycleScope.launch {
            try {
                taskViewModel.removeTask(id)
                cleanScreen()
                Toast.makeText(
                    this@TaskActivity,
                    getString(R.string.TextDeleteSuccess),
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(
                    this@TaskActivity,
                    e.message ?: "No se pudo eliminar la tarea",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progressDialog.dismiss()
            }
        }
    }

    private fun setStatusOptions(current: TaskStatus?) {

        val options = current?.allowedStatuses()
            ?: listOf(TaskStatus.PENDING)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            options
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spStatus.adapter = adapter

        spStatus.setSelection(
            options.indexOf(current ?: TaskStatus.PENDING)
        )

        spStatus.isEnabled =
            current != null && !current.isTerminal

        updateCancellationVisibility()
    }

    private fun setReadOnly(readOnly: Boolean) {
        txtTitle.isEnabled = !readOnly
        txtDescription.isEnabled = !readOnly
        spPriority.isEnabled = !readOnly
        spAssignTo.isEnabled = !readOnly
        spParentTask.isEnabled = !readOnly
        txtObservation.isEnabled = !readOnly
        txtCancellationReason.isEnabled = !readOnly
        findViewById<ImageButton>(R.id.btnDueDate_Task).isEnabled = !readOnly
        spStatus.isEnabled = !readOnly && isEditMode
        invalidateOptionsMenu()
    }

    private fun updateCancellationVisibility() {
        val selectedStatus =
            spStatus.selectedItem as? TaskStatus

        val showReason =
            isEditMode && selectedStatus == TaskStatus.CANCELLED

        val visibility = if (showReason) {
            View.VISIBLE
        } else {
            View.GONE
        }

        lblCancellationReason.visibility = visibility
        txtCancellationReason.visibility = visibility

        if (!showReason) {
            txtCancellationReason.text.clear()
        }
    }

    private fun updateSubtasksList() {

        val currentTask = loadedTask

        if (currentTask == null) {
            tvSubtasks.visibility = View.GONE
            tvSubtasks.text = ""
            return
        }

        val subtasks = allTasks.filter {
            it.parentTaskId == currentTask.id
        }.sortedBy { it.id }

        tvSubtasks.visibility = View.VISIBLE

        tvSubtasks.text = if (subtasks.isEmpty()) {

            "Subtareas: Ninguna"

        } else {

            val details = subtasks.joinToString("\n") {
                "#${it.id} - ${it.title} [${it.status.name}]"
            }

            "Subtareas (${subtasks.size}):\n$details"
        }
    }

}