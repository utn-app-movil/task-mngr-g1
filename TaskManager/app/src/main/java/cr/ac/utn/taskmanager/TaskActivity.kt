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
    private lateinit var btnDueDate: ImageButton
    private lateinit var btnSearch: ImageButton

    private var dueDay: Int = 0
    private var dueMonth: Int = 0
    private var dueYear: Int = 0
    private var isEditMode: Boolean = false

    private var selectablePeople: List<String> = emptyList()
    private var eligibleParentsList: List<Task> = emptyList()
    private var currentParentTaskId: String? = null

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
        txtCancellationReason = findViewById(R.id.txtCancellationReason_Task)
        lblCancellationReason = findViewById(R.id.lblCancellationReason_Task)
        txtObservation = findViewById(R.id.txtObservation_Task)
        tvDueDate = findViewById(R.id.tvDueDate_Task)
        tvStatusChangeDate = findViewById(R.id.tvStatusChangeDate_Task)

        btnSearch = findViewById(R.id.btnSearch_Task)
        btnDueDate = findViewById(R.id.btnDueDate_Task)

        setupSpinners()
        observeEligibleParents()

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

        val priorityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TaskPriority.entries.toTypedArray()
        )
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPriority.adapter = priorityAdapter

        selectablePeople = util.loadPeopleFromAssets(this)
        val assignAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            selectablePeople
        )
        assignAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spAssignTo.adapter = assignAdapter

        spStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedStatus = TaskStatus.entries[position]
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
                    val currentEditingId = txtId.text.toString().trim()
                    eligibleParentsList = parents.filter { it.id != currentEditingId }

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
        if (!currentParentTaskId.isNullOrBlank()) {
            val parentIndex = eligibleParentsList.indexOfFirst { it.id == currentParentTaskId }
            if (parentIndex >= 0) {
                spParentTask.setSelection(parentIndex + 1)
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
                    txtTitle.setText(task.title)
                    txtDescription.setText(task.description)
                    spStatus.setSelection(task.status.ordinal)
                    spPriority.setSelection(task.priority.ordinal)

                    val assignIndex = selectablePeople.indexOf(task.assignedTo)
                    if (assignIndex >= 0) {
                        spAssignTo.setSelection(assignIndex)
                    }

                    txtObservation.setText(task.notes)
                    txtCancellationReason.setText(task.cancellationReason ?: "")

                    currentParentTaskId = task.parentTaskId
                    updateParentSpinnerSelection()

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
                        tvStatusChangeDate.text = "Último cambio de estado: ${task.statusChangeDateTime.format(formatter)}"
                    } else {
                        tvStatusChangeDate.text = ""
                    }

                    if (task.status == TaskStatus.COMPLETED || task.status == TaskStatus.CANCELLED) {
                        setFormEnabled(false)
                        Toast.makeText(
                            this@TaskActivity,
                            "La tarea está Finalizada/Cancelada y no se puede modificar",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        setFormEnabled(true)
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

    private fun setFormEnabled(enabled: Boolean) {
        txtTitle.isEnabled = enabled
        txtDescription.isEnabled = enabled
        spStatus.isEnabled = enabled
        spPriority.isEnabled = enabled
        spAssignTo.isEnabled = enabled
        spParentTask.isEnabled = enabled
        txtObservation.isEnabled = enabled
        txtCancellationReason.isEnabled = enabled
        btnDueDate.isEnabled = enabled
    }

    private fun getDateFormatString(dayOfMonth: Int, monthValue: Int, yearValue: Int): String {
        return "${if (dayOfMonth < 10) "0" else ""}$dayOfMonth/${if (monthValue < 10) "0" else ""}$monthValue/$yearValue"
    }

    private fun cleanScreen() {
        resetDate()
        isEditMode = false
        currentParentTaskId = null
        txtId.isEnabled = true
        setFormEnabled(true)
        txtId.setText("")
        txtTitle.setText("")
        txtDescription.setText("")
        spStatus.setSelection(0)
        spPriority.setSelection(0)
        spAssignTo.setSelection(0)
        spParentTask.setSelection(0)
        txtObservation.setText("")
        txtCancellationReason.setText("")
        tvDueDate.text = ""
        tvStatusChangeDate.text = ""
        invalidateOptionsMenu()
    }

    private fun isValidationData(): Boolean {
        return txtId.text.toString().trim().isNotBlank()
                && txtTitle.text.toString().trim().isNotBlank()
                && txtDescription.text.toString().trim().isNotBlank()
                && tvDueDate.text.toString().trim().isNotBlank()
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

        val id = txtId.text.toString().trim()
        val title = txtTitle.text.toString().trim()
        val description = txtDescription.text.toString().trim()
        val selectedStatus = spStatus.selectedItem as TaskStatus
        val selectedPriority = spPriority.selectedItem as TaskPriority
        val selectedAssignee = spAssignTo.selectedItem?.toString() ?: ""
        val notes = txtObservation.text.toString().trim()
        val cancellationReason = txtCancellationReason.text.toString().trim()

        val parentSelectionIndex = spParentTask.selectedItemPosition
        val parentTaskId = if (parentSelectionIndex > 0 && parentSelectionIndex <= eligibleParentsList.size) {
            eligibleParentsList[parentSelectionIndex - 1].id
        } else {
            null
        }

        val dueDate = util.parseStringToDateModern(
            tvDueDate.text.toString(),
            "dd/MM/yyyy"
        ) ?: LocalDate.now()

        val task = Task(
            id = id,
            title = title,
            description = description,
            dueDate = dueDate,
            creationDate = LocalDate.now(),
            status = selectedStatus,
            assignedTo = selectedAssignee,
            priority = selectedPriority,
            notes = notes,
            parentTaskId = parentTaskId,
            cancellationReason = if (selectedStatus == TaskStatus.CANCELLED) cancellationReason else null
        )

        val progressDialog = util.showProgressDialog(this, getString(R.string.TextSaving))

        val onResultCallback = { success: Boolean, error: String? ->
            progressDialog.dismiss()
            if (success) {
                cleanScreen()
                Toast.makeText(
                    this@TaskActivity,
                    getString(R.string.TextSaveSuccess),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@TaskActivity,
                    error ?: getString(R.string.TextSaveError),
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        if (!isEditMode) {
            taskViewModel.saveTask(task, onResultCallback)
        } else {
            taskViewModel.updateTask(task, onResultCallback)
        }
    }

    private fun deleteTask() {
        val id = txtId.text.toString().trim()
        if (id.isBlank()) return
        val progressDialog = util.showProgressDialog(this, getString(R.string.TextDeleting))
        taskViewModel.removeTask(id) { success, error ->
            progressDialog.dismiss()
            if (success) {
                cleanScreen()
                Toast.makeText(
                    this@TaskActivity,
                    getString(R.string.TextDeleteSuccess),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@TaskActivity,
                    error ?: getString(R.string.TextDeleteError),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
