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
    private lateinit var txtCancelReason: EditText
    private lateinit var tvDueDate: TextView
    private var dueDay: Int = 0
    private var dueMonth: Int = 0
    private var dueYear: Int = 0
    private var isEditMode: Boolean = false
    private var parentList: List<Task> = emptyList()
    private var pendingParentId: String? = null

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
        txtCancelReason = findViewById(R.id.txtCancellationReason_Task)

        setupSpinners()
        spStatus.isEnabled = false
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

        val assignAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.PeopleList,
            android.R.layout.simple_spinner_item
        )
        assignAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spAssignTo.adapter = assignAdapter
    }

    private fun observeEligibleParents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.eligibleParentTasks.collect { parents ->
                    parentList = parents.filter { it.id != txtId.text.toString().trim() }

                    val parentOptions = mutableListOf("Ninguna (Tarea Principal)")
                    parentOptions.addAll(parentList.map { "${it.id} - ${it.title}" })

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
        val index = parentList.indexOfFirst { it.id == pendingParentId }
        spParentTask.setSelection(if (index >= 0) index + 1 else 0)
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
                    spStatus.isEnabled = true
                    txtId.isEnabled = false
                    txtTitle.setText(task.title)
                    txtDescription.setText(task.description)
                    spStatus.setSelection(task.status.ordinal)
                    spPriority.setSelection(task.priority.ordinal)

                    val people = resources.getStringArray(R.array.PeopleList)
                    val personIndex = people.indexOf(task.assignedTo)
                    if (personIndex >= 0) spAssignTo.setSelection(personIndex)

                    txtCancelReason.setText(task.cancellationReason)
                    pendingParentId = task.parentId
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
        spStatus.isEnabled = false
        txtId.isEnabled = true
        txtId.setText("")
        txtTitle.setText("")
        txtDescription.setText("")
        spStatus.setSelection(0)
        spPriority.setSelection(0)
        spAssignTo.setSelection(0)
        pendingParentId = null
        spParentTask.setSelection(0)
        txtObservation.setText("")
        txtCancelReason.setText("")
        tvDueDate.text = ""
        invalidateOptionsMenu()
    }

    private fun isValidationData(): Boolean {
        return (!isEditMode || txtId.text.toString().trim().isNotBlank())
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
                val existingTask = if (isEditMode) {
                    taskViewModel.getTaskById(txtId.text.toString().trim())
                } else {
                    null
                }

                val dueDate = util.parseStringToDateModern(
                    tvDueDate.text.toString(),
                    "dd/MM/yyyy"
                )
                val selectedStatus = if (isEditMode) spStatus.selectedItem as TaskStatus else TaskStatus.PENDING

                if (isEditMode && existingTask != null) {
                    val oldStatus = existingTask.status

                    if (oldStatus == TaskStatus.COMPLETED || oldStatus == TaskStatus.CANCELLED) {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@TaskActivity,
                            getString(R.string.TextTaskLocked),
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

                    if (oldStatus != selectedStatus && !oldStatus.canChangeTo(selectedStatus)) {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@TaskActivity,
                            getString(R.string.TextStatusChangeNotAllowed),
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

                    if (selectedStatus == TaskStatus.IN_PROGRESS &&
                        oldStatus != TaskStatus.IN_PROGRESS &&
                        dueDate != null &&
                        dueDate.isBefore(LocalDate.now())
                    ) {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@TaskActivity,
                            getString(R.string.TextDueDateExpired),
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }

                    if (selectedStatus == TaskStatus.CANCELLED &&
                        txtCancelReason.text.toString().trim().isBlank()
                    ) {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this@TaskActivity,
                            getString(R.string.TextCancelReasonRequired),
                            Toast.LENGTH_LONG
                        ).show()
                        return@launch
                    }
                }

                val newId = if (isEditMode) {
                    txtId.text.toString().trim()
                } else {
                    taskViewModel.getNextId()
                }

                val parentPosition = spParentTask.selectedItemPosition
                val parentId = if (parentPosition > 0 && parentPosition <= parentList.size) {
                    parentList[parentPosition - 1].id
                } else {
                    null
                }

                val selectedPriority = spPriority.selectedItem as TaskPriority
                val task = Task(
                    newId,
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
                    cancellationReason = if (selectedStatus == TaskStatus.CANCELLED) {
                        txtCancelReason.text.toString().trim()
                    } else {
                        ""
                    },
                    statusChangedAt = if (existingTask == null || existingTask.status != selectedStatus) {
                        LocalDateTime.now()
                    } else {
                        existingTask.statusChangedAt
                    },
                    parentId = parentId.takeIf { it != newId }
                )

                val onCompleteCallback = {
                    progressDialog.dismiss()
                    cleanScreen()
                    Toast.makeText(
                        this@TaskActivity,
                        "${getString(R.string.TextSaveSuccess)} (Id: $newId)",
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