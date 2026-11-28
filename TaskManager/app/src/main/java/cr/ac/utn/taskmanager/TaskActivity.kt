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
import androidx.appcompat.app.AlertDialog
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
    private lateinit var tvDueDate: TextView
    private lateinit var lblCancellationReason: TextView
    private lateinit var txtCancellationReason: EditText
    private lateinit var tvStatusChangeDate: TextView
    private var loadedStatusChangeDate: String = ""
    private var dueDay: Int = 0
    private var dueMonth: Int = 0
    private var dueYear: Int = 0
    private var isEditMode: Boolean = false
    private var isReadOnlyTask: Boolean = false
    private var currentLoadedStatus: TaskStatus? = null
    private var eligibleParentsList: List<Task> = emptyList()
    private var loadedParentTaskId: String? = null

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

        setupSpinners()
        spStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateCancellationVisibility()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        observeEligibleParents()

        val btnSearch = findViewById<ImageButton>(R.id.btnSearch_Task)
        val btnDueDate = findViewById<ImageButton>(R.id.btnDueDate_Task)

        cleanScreen()
        val taskId = intent.getStringExtra(EXTRA_MESSAGE_ID)
        if (!taskId.isNullOrBlank()) {
            txtId.setText(taskId)
            searchTask(taskId)
        }

        btnSearch.setOnClickListener {
            showSearchDialog()
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
        menu?.findItem(R.id.mnuSave)?.isVisible = !isReadOnlyTask
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

        val priorityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TaskPriority.entries.toTypedArray()
        )
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPriority.adapter = priorityAdapter

        val peopleList = mutableListOf("Seleccione una persona...")
        peopleList.addAll(util.loadPeopleFromAssets(this))
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
                    val currentId = txtId.text.toString().trim()
                    eligibleParentsList = parents.filter { it.id != currentId }

                    val parentOptions = mutableListOf("Ninguna (Tarea Principal)")
                    parentOptions.addAll(eligibleParentsList.map { "${it.id} - ${it.title}" })

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
        if (!loadedParentTaskId.isNullOrBlank()) {
            val index = eligibleParentsList.indexOfFirst { it.id == loadedParentTaskId }
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
        if (id.isBlank()) return
        val progressDialog = util.showProgressDialog(this, getString(R.string.TextLoading))
        lifecycleScope.launch {
            try {
                val task = taskViewModel.getTaskById(id)
                if (task != null) {
                    isEditMode = true
                    txtId.isEnabled = false
                    currentLoadedStatus = task.status
                    txtTitle.setText(task.title)
                    txtDescription.setText(task.description)
                    spStatus.setSelection(task.status.ordinal)
                    spPriority.setSelection(task.priority.ordinal)

                    val assignAdapter = spAssignTo.adapter as? ArrayAdapter<String>
                    if (assignAdapter != null && task.assignedTo.isNotBlank()) {
                        var foundIndex = -1
                        for (i in 0 until assignAdapter.count) {
                            if (assignAdapter.getItem(i) == task.assignedTo) {
                                foundIndex = i
                                break
                            }
                        }
                        if (foundIndex != -1) {
                            spAssignTo.setSelection(foundIndex)
                        } else {
                            spAssignTo.setSelection(0)
                        }
                    } else {
                        spAssignTo.setSelection(0)
                    }

                    loadedParentTaskId = task.parentTaskId
                    updateParentSpinnerSelection()

                    txtObservation.setText(task.notes)
                    txtCancellationReason.setText(task.cancellationReason)
                    loadedStatusChangeDate = task.statusChangeDate
                    showStatusChangeDate()
                    val dueDateStr = getDateFormatString(
                        task.dueDate.dayOfMonth,
                        task.dueDate.monthValue,
                        task.dueDate.year
                    )
                    tvDueDate.text = dueDateStr
                    dueDay = task.dueDate.dayOfMonth
                    dueMonth = task.dueDate.monthValue
                    dueYear = task.dueDate.year

                    if (task.status == TaskStatus.COMPLETED || task.status == TaskStatus.CANCELLED) {
                        isReadOnlyTask = true
                        setFormEnabled(false)
                        Toast.makeText(
                            this@TaskActivity,
                            "Tarea en estado ${task.status.name}: no se puede modificar",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        isReadOnlyTask = false
                        setFormEnabled(true)
                        spStatus.isEnabled = true

                        val allowedStatuses = when (task.status) {
                            TaskStatus.PENDING -> listOf(TaskStatus.PENDING, TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED)
                            TaskStatus.IN_PROGRESS -> listOf(TaskStatus.IN_PROGRESS, TaskStatus.COMPLETED, TaskStatus.CANCELLED)
                            else -> TaskStatus.entries.toList()
                        }
                        val statusAdapter = ArrayAdapter(
                            this@TaskActivity,
                            android.R.layout.simple_spinner_item,
                            allowedStatuses
                        )
                        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spStatus.adapter = statusAdapter

                        val selectedIndex = allowedStatuses.indexOf(task.status)
                        if (selectedIndex != -1) {
                            spStatus.setSelection(selectedIndex)
                        }
                    }

                    updateCancellationVisibility()
                    invalidateOptionsMenu()
                } else {
                    Toast.makeText(
                        this@TaskActivity,
                        getString(R.string.TextDataNoFound),
                        Toast.LENGTH_LONG
                    ).show()
                    cleanScreen()
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

    private suspend fun getNextId(): String {
        val lastUsed = getSharedPreferences("task_prefs", MODE_PRIVATE).getInt("last_id", 0)
        val next = maxOf(taskViewModel.getNextId().toInt(), lastUsed + 1)
        return next.toString()
    }

    private fun showSearchDialog() {
        val input = EditText(this)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.TextId))
            .setView(input)
            .setPositiveButton("Buscar") { _, _ ->
                val id = input.text.toString().trim()
                if (id.isNotBlank()) {
                    cleanScreen()
                    txtId.setText(id)
                    searchTask(id)
                }
            }
            .setNegativeButton(getString(R.string.TextCancel), null)
            .show()
    }

    private fun showStatusChangeDate() {
        tvStatusChangeDate.text = if (loadedStatusChangeDate.isBlank()) {
            ""
        } else {
            "Último cambio de estado: $loadedStatusChangeDate"
        }
    }

    private fun updateCancellationVisibility() {
        val show = spStatus.selectedItem == TaskStatus.CANCELLED
        val visibility = if (show) View.VISIBLE else View.GONE
        lblCancellationReason.visibility = visibility
        txtCancellationReason.visibility = visibility
    }

    private fun setFormEnabled(enabled: Boolean) {
        txtCancellationReason.isEnabled = enabled
        txtTitle.isEnabled = enabled
        txtDescription.isEnabled = enabled
        spStatus.isEnabled = enabled
        spPriority.isEnabled = enabled
        spAssignTo.isEnabled = enabled
        spParentTask.isEnabled = enabled
        txtObservation.isEnabled = enabled
        findViewById<ImageButton>(R.id.btnDueDate_Task)?.isEnabled = enabled
    }

    private fun getDateFormatString(dayOfMonth: Int, monthValue: Int, yearValue: Int): String {
        return "${if (dayOfMonth < 10) "0" else ""}$dayOfMonth/${if (monthValue < 10) "0" else ""}$monthValue/$yearValue"
    }

    private fun cleanScreen() {
        resetDate()
        isEditMode = false
        isReadOnlyTask = false
        currentLoadedStatus = null
        setFormEnabled(true)
        txtId.isEnabled = false
        txtId.setText("")
        lifecycleScope.launch {
            val nextId = getNextId()
            if (!isEditMode && txtId.text.isBlank()) {
                txtId.setText(nextId)
            }
        }
        txtTitle.setText("")
        txtDescription.setText("")

        val defaultStatusAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TaskStatus.entries.toTypedArray()
        )
        defaultStatusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spStatus.adapter = defaultStatusAdapter

        spStatus.setSelection(TaskStatus.PENDING.ordinal)
        spStatus.isEnabled = false
        spPriority.setSelection(0)
        spAssignTo.setSelection(0)
        loadedParentTaskId = null
        spParentTask.setSelection(0)
        txtObservation.setText("")
        txtCancellationReason.setText("")
        loadedStatusChangeDate = ""
        showStatusChangeDate()
        updateCancellationVisibility()
        tvDueDate.text = ""
        invalidateOptionsMenu()
    }

    private fun isValidationData(): Boolean {
        val isPersonSelected = spAssignTo.selectedItemPosition > 0 &&
                spAssignTo.selectedItem != null &&
                spAssignTo.selectedItem.toString() != "Seleccione una persona..."

        return txtTitle.text.toString().trim().isNotBlank()
                && txtDescription.text.toString().trim().isNotBlank()
                && spPriority.selectedItem != null
                && tvDueDate.text.toString().trim().isNotBlank()
                && txtObservation.text.toString().trim().isNotBlank()
                && isPersonSelected
    }

    private fun saveTask() {
        if (isEditMode && (currentLoadedStatus == TaskStatus.COMPLETED || currentLoadedStatus == TaskStatus.CANCELLED)) {
            Toast.makeText(
                this@TaskActivity,
                "No se puede modificar una tarea completada o cancelada",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (isEditMode && currentLoadedStatus == TaskStatus.PENDING) {
            val selectedStatus = spStatus.selectedItem as TaskStatus
            if (selectedStatus != TaskStatus.PENDING &&
                selectedStatus != TaskStatus.IN_PROGRESS &&
                selectedStatus != TaskStatus.CANCELLED) {
                Toast.makeText(
                    this@TaskActivity,
                    "Una tarea PENDING solo puede cambiar a IN_PROGRESS o CANCELLED",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        if (isEditMode && currentLoadedStatus == TaskStatus.IN_PROGRESS) {
            val selectedStatus = spStatus.selectedItem as TaskStatus
            if (selectedStatus != TaskStatus.IN_PROGRESS &&
                selectedStatus != TaskStatus.COMPLETED &&
                selectedStatus != TaskStatus.CANCELLED) {
                Toast.makeText(
                    this@TaskActivity,
                    "Una tarea IN_PROGRESS solo puede cambiar a COMPLETED o CANCELLED",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        if (isEditMode && currentLoadedStatus == TaskStatus.PENDING &&
            spStatus.selectedItem == TaskStatus.IN_PROGRESS &&
            LocalDate.of(dueYear, dueMonth, dueDay).isBefore(LocalDate.now())) {
            Toast.makeText(
                this@TaskActivity,
                "No se puede pasar a IN_PROGRESS una tarea con fecha de vencimiento anterior a hoy",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (isEditMode && spStatus.selectedItem == TaskStatus.CANCELLED &&
            txtCancellationReason.text.toString().trim().isBlank()) {
            Toast.makeText(
                this@TaskActivity,
                "Debe indicar la razón de la cancelación",
                Toast.LENGTH_LONG
            ).show()
            return
        }

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
                val taskId = if (isEditMode) {
                    txtId.text.toString().trim()
                } else {
                    getNextId()
                }
                val existingTask = taskViewModel.getTaskById(taskId)
                if (existingTask != null && !isEditMode) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this@TaskActivity,
                        getString(R.string.TextDuplicateData),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val dueDate = util.parseStringToDateModern(
                        tvDueDate.text.toString(),
                        "dd/MM/yyyy"
                    )
                    val selectedStatus = if (!isEditMode) {
                        TaskStatus.PENDING
                    } else {
                        spStatus.selectedItem as TaskStatus
                    }
                    val selectedPriority = spPriority.selectedItem as TaskPriority
                    val selectedParentTaskId = if (spParentTask.selectedItemPosition > 0 && spParentTask.selectedItemPosition - 1 < eligibleParentsList.size) {
                        eligibleParentsList[spParentTask.selectedItemPosition - 1].id
                    } else {
                        null
                    }

                    val task = Task(
                        taskId,
                        txtTitle.text.toString().trim(),
                        txtDescription.text.toString().trim(),
                        LocalDate.of(
                            dueDate?.year!!,
                            dueDate.month!!,
                            dueDate.dayOfMonth
                        ),
                        LocalDate.now(),
                        selectedStatus,
                        spAssignTo.selectedItem?.toString() ?: "",
                        selectedPriority,
                        txtObservation.text.toString().trim(),
                        selectedParentTaskId,
                        if (selectedStatus == TaskStatus.CANCELLED)
                            txtCancellationReason.text.toString().trim()
                        else "",
                        if (!isEditMode || selectedStatus != currentLoadedStatus)
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        else loadedStatusChangeDate
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
                        getSharedPreferences("task_prefs", MODE_PRIVATE)
                            .edit()
                            .putInt("last_id", taskId.toInt())
                            .apply()
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