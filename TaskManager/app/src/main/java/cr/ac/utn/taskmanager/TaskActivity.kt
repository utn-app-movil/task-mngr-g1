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
import cr.ac.utn.taskmanager.util.TaskValidation
import androidx.appcompat.app.AlertDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private var currentTask: Task? = null

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

        val isFinalTask = currentTask?.let {
            TaskValidation.isFinalStatus(it.status)
        } ?: false

        menu?.findItem(R.id.mnuSave)?.isVisible =
            !isFinalTask

        menu?.findItem(R.id.mnuDelete)?.isVisible =
            isEditMode && !isFinalTask

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
        statusAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        spStatus.adapter = statusAdapter
        configureStatusSpinner(TaskStatus.PENDING)
        spStatus.isEnabled = false

        val priorityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TaskPriority.entries.toTypedArray()
        )
        priorityAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        spPriority.adapter = priorityAdapter

        val people = listOf(
            "Ana",
            "Carlos",
            "Maria",
            "Jose",
            "Sofia"
        )

        val peopleAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            people
        )
        peopleAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        spAssignTo.adapter = peopleAdapter
    }

    private fun observeEligibleParents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.eligibleParentTasks.collect { parents ->


                    val parentOptions = mutableListOf("Ninguna (Tarea Principal)")


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

        val progressDialog = util.showProgressDialog(
            this,
            getString(R.string.TextLoading)
        )

        lifecycleScope.launch {
            try {

                val task = taskViewModel.getTaskById(id)

                if (task != null) {

                    currentTask = task
                    isEditMode = true
                    txtId.setText(task.id)
                    txtId.isEnabled = false
                    txtTitle.setText(task.title)
                    txtDescription.setText(task.description)
                    configureStatusSpinner(task.status)
                    spPriority.setSelection(
                        task.priority.ordinal
                    )

                    for (i in 0 until spAssignTo.count) {
                        if (spAssignTo.getItemAtPosition(i)
                                .toString() == task.assignedTo
                        ) {
                            spAssignTo.setSelection(i)
                            break
                        }
                    }

                    txtObservation.setText(task.notes)
                    dueDay = task.dueDate.dayOfMonth
                    dueMonth = task.dueDate.monthValue
                    dueYear = task.dueDate.year
                    tvDueDate.text = getDateFormatString(
                        dueDay,
                        dueMonth,
                        dueYear
                    )

                    val canEdit =
                        !TaskValidation.isFinalStatus(
                            task.status
                        )

                    setFormEnabled(canEdit)
                    invalidateOptionsMenu()
                    showStatusInformation(task)

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
        txtId.isEnabled = true
        txtId.setText("")
        txtTitle.setText("")
        txtDescription.setText("")
        setFormEnabled(true)
        configureStatusSpinner(TaskStatus.PENDING)
        spPriority.setSelection(0)
        spAssignTo.setSelection(0)
        spParentTask.setSelection(0)
        txtObservation.setText("")
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

    private fun persistTask(task: Task) {

        val progressDialog = util.showProgressDialog(
            this,
            getString(R.string.TextSaving)
        )

        val onCompleteCallback = {

            progressDialog.dismiss()

            cleanScreen()

            Toast.makeText(
                this,
                getString(R.string.TextSaveSuccess),
                Toast.LENGTH_LONG
            ).show()
        }

        val onErrorCallback: (String) -> Unit = { message ->

            progressDialog.dismiss()

            Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
            ).show()
        }

        if (isEditMode) {

            taskViewModel.updateTask(
                task,
                onCompleteCallback,
                onErrorCallback
            )

        } else {

            taskViewModel.saveTask(
                task,
                onCompleteCallback,
                onErrorCallback
            )
        }
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

        val existingTask = currentTask

        val selectedStatus = if (isEditMode) {
            spStatus.selectedItem as TaskStatus
        } else {
            TaskStatus.PENDING
        }

        val selectedPriority =
            spPriority.selectedItem as TaskPriority

        val selectedDueDate = LocalDate.of(
            dueYear,
            dueMonth,
            dueDay
        )

        if (existingTask != null) {

            val validationError =
                TaskValidation.validateStatusChange(
                    existingTask.status,
                    selectedStatus,
                    selectedDueDate
                )

            if (validationError != null) {

                Toast.makeText(
                    this,
                    validationError,
                    Toast.LENGTH_LONG
                ).show()

                return
            }
        }

        val task = Task(
            id = existingTask?.id ?: "",
            title = txtTitle.text.toString().trim(),
            description = txtDescription.text.toString().trim(),
            dueDate = selectedDueDate,
            creationDate = existingTask?.creationDate
                ?: LocalDate.now(),
            status = selectedStatus,
            assignedTo = spAssignTo.selectedItem.toString(),
            priority = selectedPriority,
            notes = txtObservation.text.toString().trim(),
            cancellationReason =
                existingTask?.cancellationReason ?: "",
            statusChangedAt =
                existingTask?.statusChangedAt
        )

        if (existingTask != null &&
            selectedStatus == TaskStatus.CANCELLED &&
            existingTask.status != TaskStatus.CANCELLED
        ) {

            showCancellationDialog { reason ->

                persistTask(
                    task.copy(
                        cancellationReason = reason
                    )
                )
            }

        } else {

            persistTask(task)
        }
    }

    private fun deleteTask() {

        val id = txtId.text.toString().trim()

        if (id.isBlank()) return

        val progressDialog = util.showProgressDialog(
            this,
            getString(R.string.TextDeleting)
        )

        taskViewModel.removeTask(
            id,

            onComplete = {

                progressDialog.dismiss()

                cleanScreen()

                Toast.makeText(
                    this,
                    getString(R.string.TextDeleteSuccess),
                    Toast.LENGTH_LONG
                ).show()
            },

            onError = { message ->

                progressDialog.dismiss()

                Toast.makeText(
                    this,
                    message,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun showCancellationDialog(
        onConfirm: (String) -> Unit
    ) {

        val reasonInput = EditText(this)

        reasonInput.hint = "Cancellation reason"
        reasonInput.minLines = 2

        val dialog = AlertDialog.Builder(this)
            .setTitle("Cancel task")
            .setMessage("Please enter the cancellation reason.")
            .setView(reasonInput)
            .setNegativeButton("Back", null)
            .setPositiveButton("Confirm", null)
            .create()

        dialog.setOnShowListener {

            val confirmButton = dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            )

            confirmButton.setOnClickListener {

                val reason =
                    reasonInput.text.toString().trim()

                if (reason.isBlank()) {

                    reasonInput.error =
                        "Cancellation reason is required."

                } else {

                    dialog.dismiss()

                    onConfirm(reason)
                }
            }
        }

        dialog.show()
    }

    private fun configureStatusSpinner(status: TaskStatus) {

        val allowedStatuses =
            TaskValidation.getAllowedStatuses(status)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            allowedStatuses
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spStatus.adapter = adapter

        spStatus.setSelection(
            allowedStatuses.indexOf(status)
        )

        spStatus.isEnabled =
            isEditMode &&
                    !TaskValidation.isFinalStatus(status)
    }

    private fun setFormEnabled(enabled: Boolean) {

        txtTitle.isEnabled = enabled
        txtDescription.isEnabled = enabled
        spPriority.isEnabled = enabled
        spAssignTo.isEnabled = enabled
        spParentTask.isEnabled = enabled
        txtObservation.isEnabled = enabled

        findViewById<ImageButton>(
            R.id.btnDueDate_Task
        ).isEnabled = enabled

        spStatus.isEnabled = enabled && isEditMode
    }

    private fun showStatusInformation(task: Task) {

        val dateFormat = SimpleDateFormat(
            "dd/MM/yyyy HH:mm:ss",
            Locale.getDefault()
        )

        val formattedDate = task.statusChangedAt?.let {
            dateFormat.format(Date(it))
        } ?: "Not registered"

        val message = StringBuilder()

        message.append("Status: ${task.status}\n\n")

        message.append(
            "Last status change:\n$formattedDate"
        )

        if (task.status == TaskStatus.CANCELLED) {

            val reason = task.cancellationReason
                .ifBlank { "Not registered" }

            message.append(
                "\n\nCancellation reason:\n$reason"
            )
        }

        AlertDialog.Builder(this)
            .setTitle("Task information")
            .setMessage(message.toString())
            .setPositiveButton("OK", null)
            .show()
    }


}