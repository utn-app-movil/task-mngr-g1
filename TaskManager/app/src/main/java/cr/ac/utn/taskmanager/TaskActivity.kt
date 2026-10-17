package cr.ac.utn.taskmanager

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cr.ac.utn.taskmanager.data.memory.TaskMemoryManager
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskPriority
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import cr.ac.utn.taskmanager.viewmodel.TaskViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class TaskActivity : AppCompatActivity() {

    private lateinit var txtId: EditText
    private lateinit var txtTitle: EditText
    private lateinit var txtDescription: EditText
    private lateinit var txtStatus: Spinner
    private lateinit var txtPriority: Spinner
    private lateinit var txtAssignTo: EditText
    private lateinit var txtObservation: EditText
    private lateinit var tvDueDate: TextView

    private lateinit var viewModel: TaskViewModel

    private var dueDate: LocalDate? = null
    private var currentTask: Task? = null

    private val dateFormatter: DateTimeFormatter
        get() = DateTimeFormatter.ofPattern(
            "dd/MM/yyyy",
            Locale.getDefault()
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_task)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        viewModel = TaskViewModel(TaskMemoryManager())

        txtId = findViewById(R.id.txtId_Task)
        txtTitle = findViewById(R.id.txtTitle_Task)
        txtDescription = findViewById(R.id.txtDescription_Task)
        txtStatus = findViewById(R.id.txtStatus_Task)
        txtPriority = findViewById(R.id.txtPriority_Task)
        txtObservation = findViewById(R.id.txtObservation_Task)
        txtAssignTo = findViewById(R.id.txtAssignTo_Task)
        tvDueDate = findViewById(R.id.tvDueDate_Task)

        val btnSearch = findViewById<ImageButton>(R.id.btnSearch_Task)
        val btnDueDate = findViewById<ImageButton>(R.id.btnDueDate_Task)
        val btnSave = findViewById<Button>(R.id.btnSave_Task)
        val btnDelete = findViewById<Button>(R.id.btnRemove_Task)
        val btnClear = findViewById<Button>(R.id.btnClear_Task)

        btnSearch.setOnClickListener {
            searchTask()
        }

        btnDueDate.setOnClickListener {
            showDatePicker()
        }

        btnSave.setOnClickListener {
            confirmSave()
        }

        btnDelete.setOnClickListener {
            confirmDelete()
        }

        btnClear.setOnClickListener {
            clearForm()
        }

        clearForm()
    }

    private fun showDatePicker() {
        val initialDate = dueDate ?: LocalDate.now()

        DatePickerDialog(
            this,
            { _, year, month, day ->
                dueDate = LocalDate.of(year, month + 1, day)
                tvDueDate.text = dueDate!!.format(dateFormatter)
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        ).show()
    }

    private fun searchTask() {
        val id = txtId.text.toString().trim()

        if (id.isEmpty()) {
            showMessage(R.string.task_error_not_found)
            return
        }

        try {
            val task = viewModel.getTaskById(id)

            if (task == null) {
                currentTask = null
                showMessage(R.string.task_error_not_found)
                return
            }

            currentTask = task
            txtTitle.setText(task.title)
            txtDescription.setText(task.description)
            txtAssignTo.setText(task.assignedTo)
            txtObservation.setText(task.notes)
            dueDate = task.dueDate
            tvDueDate.text = task.dueDate.format(dateFormatter)

            txtStatus.setSelection(task.status.ordinal)
            txtPriority.setSelection(task.priority.ordinal)

        } catch (_: Exception) {
            showMessage(R.string.task_error_not_found)
        }
    }

    private fun confirmSave() {
        val id = txtId.text.toString().trim()
        val existingTask = if (id.isNotEmpty()) {
            viewModel.getTaskById(id)
        } else {
            null
        }

        val selectedStatus = TaskStatus.values()[txtStatus.selectedItemPosition]

        if (
            existingTask?.status == TaskStatus.COMPLETED &&
            selectedStatus == TaskStatus.CANCELLED
        ) {
            showMessage(R.string.task_error_completed_protected)
            return
        }

        AlertDialog.Builder(this)
            .setMessage(
                getString(
                    R.string.task_confirm_action,
                    getString(R.string.task_button_save)
                )
            )
            .setPositiveButton(R.string.task_yes) { _, _ ->
                saveTask()
            }
            .setNegativeButton(R.string.task_no, null)
            .show()
    }

    private fun saveTask() {
        val id = txtId.text.toString().trim()
        val title = txtTitle.text.toString().trim()
        val description = txtDescription.text.toString().trim()
        val assignedTo = txtAssignTo.text.toString().trim()
        val notes = txtObservation.text.toString().trim()
        val selectedDueDate = dueDate

        if (
            id.isEmpty() ||
            title.isEmpty() ||
            description.isEmpty() ||
            assignedTo.isEmpty() ||
            selectedDueDate == null
        ) {
            showMessage(R.string.task_error_save)
            return
        }

        try {
            val existingTask = viewModel.getTaskById(id)
            val selectedStatus = TaskStatus.values()[txtStatus.selectedItemPosition]
            val selectedPriority = TaskPriority.values()[txtPriority.selectedItemPosition]

            if (existingTask == null) {
                val task = Task(
                    id = id,
                    title = title,
                    description = description,
                    dueDate = selectedDueDate,
                    creationDate = LocalDate.now(),
                    status = selectedStatus,
                    assignedTo = assignedTo,
                    priority = selectedPriority,
                    notes = notes
                )

                viewModel.saveTask(task)
                currentTask = task
            } else {
                if (currentTask?.id != id) {
                    showMessage(R.string.task_error_duplicate)
                    return
                }

                if (
                    existingTask.status == TaskStatus.COMPLETED &&
                    selectedStatus == TaskStatus.CANCELLED
                ) {
                    showMessage(R.string.task_error_completed_protected)
                    return
                }

                val task = existingTask.copy(
                    title = title,
                    description = description,
                    dueDate = selectedDueDate,
                    status = selectedStatus,
                    assignedTo = assignedTo,
                    priority = selectedPriority,
                    notes = notes
                )

                viewModel.updateTask(task)
                currentTask = task
            }

            showMessage(R.string.task_message_save_success)

        } catch (_: Exception) {
            showMessage(R.string.task_error_save)
        }
    }

    private fun confirmDelete() {
        val id = txtId.text.toString().trim()

        if (id.isEmpty()) {
            showMessage(R.string.task_error_not_found)
            return
        }

        val task = try {
            viewModel.getTaskById(id)
        } catch (_: Exception) {
            null
        }

        if (task == null) {
            showMessage(R.string.task_error_not_found)
            return
        }

        if (task.status == TaskStatus.COMPLETED) {
            showMessage(R.string.task_error_completed_protected)
            return
        }

        AlertDialog.Builder(this)
            .setMessage(
                getString(
                    R.string.task_confirm_action,
                    getString(R.string.task_button_delete)
                )
            )
            .setPositiveButton(R.string.task_yes) { _, _ ->
                deleteTask(task)
            }
            .setNegativeButton(R.string.task_no, null)
            .show()
    }

    private fun deleteTask(task: Task) {
        try {
            viewModel.removeTask(task.id)

            if (viewModel.getTaskById(task.id) == null) {
                showMessage(R.string.task_message_delete_success)
                clearForm()
            } else {
                showMessage(R.string.task_error_delete)
            }
        } catch (_: Exception) {
            showMessage(R.string.task_error_delete)
        }
    }

    private fun clearForm() {
        txtId.setText("")
        txtTitle.setText("")
        txtDescription.setText("")
        txtAssignTo.setText("")
        txtObservation.setText("")
        txtStatus.setSelection(0)
        txtPriority.setSelection(0)

        dueDate = null
        tvDueDate.text = getString(R.string.task_label_due_date)
        currentTask = null
    }

    private fun showMessage(messageId: Int) {
        Toast.makeText(this, getString(messageId), Toast.LENGTH_LONG).show()
    }
}