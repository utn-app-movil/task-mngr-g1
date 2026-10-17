package cr.ac.utn.taskmanager

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.app.DatePickerDialog
import java.util.Calendar
import android.widget.Toast
import cr.ac.utn.taskmanager.data.memory.TaskMemoryManager
import cr.ac.utn.taskmanager.viewmodel.TaskViewModel
import androidx.appcompat.app.AlertDialog
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskPriority
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import java.time.LocalDate
import java.util.Locale
class TaskActivity : AppCompatActivity() {
    private lateinit var txtId: EditText
    private lateinit var txtTitle: EditText
    private lateinit var txtDescription: EditText
    private lateinit var txtStatus: EditText
    private lateinit var txtPriority: EditText
    private lateinit var txtAssignTo: EditText
    private lateinit var txtObservation: EditText
    private lateinit var tvDueDate: TextView
    private var dueDay: Int=0
    private var dueMonth: Int=0
    private var dueYear: Int=0

    private val taskViewModel = TaskViewModel(TaskMemoryManager())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_task)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        txtId = findViewById<EditText>(R.id.txtId_Task)
        txtTitle = findViewById<EditText>(R.id.txtTitle_Task)
        txtDescription = findViewById<EditText>(R.id.txtDescription_Task)
        txtStatus = findViewById<EditText>(R.id.txtStatus_Task)
        txtPriority = findViewById<EditText>(R.id.txtPriority_Task)
        txtObservation = findViewById<EditText>(R.id.txtObservation_Task)
        txtAssignTo = findViewById<EditText>(R.id.txtAssignTo_Task)
        tvDueDate = findViewById<TextView>(R.id.tvDueDate_Task)

        val btnSearch = findViewById<ImageButton>(R.id.btnSearch_Task)
        val btnDueDate = findViewById<ImageButton>(R.id.btnDueDate_Task)
        val btnSave= findViewById<Button>(R.id.btnSave_Task)
        val btnRemove= findViewById<Button>(R.id.btnRemove_Task)
        val btnCancel= findViewById<Button>(R.id.btnCancel_Task)
        btnCancel.setOnClickListener {
            txtId.text.clear()
            txtTitle.text.clear()
            txtDescription.text.clear()
            txtStatus.text.clear()
            txtPriority.text.clear()
            txtAssignTo.text.clear()
            txtObservation.text.clear()

            tvDueDate.setText(R.string.task_date_not_selected)

            findViewById<TextView>(R.id.tvCreationDate_Task)
                .setText(R.string.task_date_not_selected)

            dueDay = 0
            dueMonth = 0
            dueYear = 0

            txtId.requestFocus()
        }

        btnDueDate.setOnClickListener {
            val today = Calendar.getInstance()

            val initialYear = if (dueYear > 0) {
                dueYear
            } else {
                today.get(Calendar.YEAR)
            }

            val initialMonth = if (dueYear > 0) {
                dueMonth
            } else {
                today.get(Calendar.MONTH)
            }

            val initialDay = if (dueYear > 0) {
                dueDay
            } else {
                today.get(Calendar.DAY_OF_MONTH)
            }

            DatePickerDialog(
                this,
                { _, year, month, day ->
                    dueYear = year
                    dueMonth = month
                    dueDay = day

                    tvDueDate.text = "$day/${month + 1}/$year"
                },
                initialYear,
                initialMonth,
                initialDay
            ).show()
        }

        btnSearch.setOnClickListener {
            val id = txtId.text.toString().trim()

            val task = taskViewModel.getTaskById(id)

            if (task == null) {
                Toast.makeText(
                    this,
                    R.string.message_data_not_found,
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                txtId.setText(task.id)
                txtTitle.setText(task.title)
                txtDescription.setText(task.description)
                txtStatus.setText(task.status.name)
                txtPriority.setText(task.priority.name)
                txtAssignTo.setText(task.assignedTo)
                txtObservation.setText(task.notes)

                dueDay = task.dueDate.dayOfMonth
                dueMonth = task.dueDate.monthValue - 1
                dueYear = task.dueDate.year

                tvDueDate.text = "$dueDay/${dueMonth + 1}/$dueYear"

                findViewById<TextView>(R.id.tvCreationDate_Task).text =
                    task.creationDate.toString()
            }
        }

        btnSave.setOnClickListener {
            val id = txtId.text.toString().trim()
            val title = txtTitle.text.toString().trim()

            val status = runCatching {
                TaskStatus.valueOf(
                    txtStatus.text.toString().trim().uppercase(Locale.ROOT)
                )
            }.getOrNull()

            val priority = runCatching {
                TaskPriority.valueOf(
                    txtPriority.text.toString().trim().uppercase(Locale.ROOT)
                )
            }.getOrNull()

            val dueDate = runCatching {
                LocalDate.of(dueYear, dueMonth + 1, dueDay)
            }.getOrNull()

            if (
                id.isBlank() ||
                title.isBlank() ||
                status == null ||
                priority == null ||
                dueDate == null
            ) {
                Toast.makeText(
                    this,
                    R.string.message_invalid_task_data,
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (taskViewModel.getTaskById(id) != null) {
                Toast.makeText(
                    this,
                    R.string.message_duplicate_data,
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val task = Task(
                id = id,
                title = title,
                description = txtDescription.text.toString().trim(),
                dueDate = dueDate,
                creationDate = LocalDate.now(),
                status = status,
                assignedTo = txtAssignTo.text.toString().trim(),
                priority = priority,
                notes = txtObservation.text.toString().trim()
            )

            AlertDialog.Builder(this)
                .setMessage(R.string.dialog_confirm_action)
                .setPositiveButton(R.string.dialog_button_yes) { _, _ ->
                    try {
                        taskViewModel.saveTask(task)

                        findViewById<TextView>(R.id.tvCreationDate_Task).text =
                            task.creationDate.toString()

                        Toast.makeText(
                            this,
                            R.string.message_save_success,
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (exception: Exception) {
                        Toast.makeText(
                            this,
                            R.string.message_save_error,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton(R.string.dialog_button_no) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        btnRemove.setOnClickListener {
            val id = txtId.text.toString().trim()
            val task = taskViewModel.getTaskById(id)

            if (task == null) {
                Toast.makeText(
                    this,
                    R.string.message_data_not_found,
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (task.status == TaskStatus.COMPLETED) {
                Toast.makeText(
                    this,
                    R.string.message_task_completed,
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setMessage(R.string.dialog_confirm_action)
                .setPositiveButton(R.string.dialog_button_yes) { _, _ ->
                    try {
                        taskViewModel.removeTask(id)

                        btnCancel.performClick()

                        Toast.makeText(
                            this,
                            R.string.message_delete_success,
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (exception: Exception) {
                        Toast.makeText(
                            this,
                            R.string.message_delete_error,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton(R.string.dialog_button_no) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

    }





}