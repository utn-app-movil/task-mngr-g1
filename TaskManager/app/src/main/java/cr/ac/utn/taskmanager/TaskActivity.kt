package cr.ac.utn.taskmanager

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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

class TaskActivity : AppCompatActivity() {
    private lateinit var txtId: EditText
    private lateinit var txtTitle: EditText
    private lateinit var txtDescription: EditText
    private lateinit var spnStatus: Spinner
    private lateinit var spnPriority: Spinner
    private lateinit var txtAssignTo: EditText
    private lateinit var txtObservation: EditText
    private lateinit var tvDueDate: TextView
    private val viewModel = TaskViewModel(repository)
    private var dueDate: LocalDate? = null
    private var loadedTaskId: String? = null

    // Conserva las tareas en memoria al salir y volver a esta pantalla.
    companion object {
        private val repository = TaskMemoryManager()
    }

    // Prepara los controles y conecta los eventos de los botones.
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
        spnStatus = findViewById(R.id.spnStatus_Task)
        spnPriority = findViewById(R.id.spnPriority_Task)
        txtObservation = findViewById(R.id.txtObservation_Task)
        txtAssignTo = findViewById(R.id.txtAssignTo_Task)
        tvDueDate = findViewById(R.id.tvDueDate_Task)

        configureSpinner(spnStatus, resources.getStringArray(R.array.task_status_options))
        configureSpinner(spnPriority, resources.getStringArray(R.array.task_priority_options))
        if (savedInstanceState == null) {
            clearForm()
        } else {
            loadedTaskId = savedInstanceState.getString("loadedTaskId")
            dueDate = savedInstanceState.getString("dueDate")?.let { LocalDate.parse(it) }
            tvDueDate.text = dueDate?.toString().orEmpty()
        }

        val btnSearch = findViewById<ImageButton>(R.id.btnSearch_Task)
        val btnDueDate = findViewById<ImageButton>(R.id.btnDueDate_Task)
        val btnSave = findViewById<Button>(R.id.btnSave_Task)
        val btnRemove = findViewById<Button>(R.id.btnRemove_Task)
        val btnCancel = findViewById<Button>(R.id.btnCancel_Task)

        btnSearch.setOnClickListener { searchTask() }
        btnDueDate.setOnClickListener { selectDueDate() }
        btnSave.setOnClickListener { saveTask() }
        btnRemove.setOnClickListener { deleteTask() }
        btnCancel.setOnClickListener { clearForm() }
    }

    // Conservar la fecha y el ID consultado al girar el dispositivo.
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("loadedTaskId", loadedTaskId)
        outState.putString("dueDate", dueDate?.toString())
        super.onSaveInstanceState(outState)
    }

    // Configura una lista desplegable; se usa para estado y prioridad.
    private fun configureSpinner(spinner: Spinner, options: Array<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }
    // Busca por ID y coloca los datos encontrados en el formulario.
    private fun searchTask() {
        val task = viewModel.getTaskById(txtId.text.toString().trim())
        if (task == null) {
            loadedTaskId = null
            showMessage(R.string.task_not_found)
            return
        }

        loadedTaskId = task.id
        txtId.setText(task.id)
        txtTitle.setText(task.title)
        txtDescription.setText(task.description)
        spnStatus.setSelection(task.status.ordinal)
        spnPriority.setSelection(task.priority.ordinal)
        txtAssignTo.setText(task.assignedTo)
        txtObservation.setText(task.notes)
        dueDate = task.dueDate
        tvDueDate.text = task.dueDate.toString()
    }

    // Abre el calendario y muestra la fecha seleccionada.
    private fun selectDueDate() {
        val date = dueDate ?: LocalDate.now()
        DatePickerDialog(this, { _, year, month, day ->
            dueDate = LocalDate.of(year, month + 1, day)
            tvDueDate.text = dueDate.toString()
        }, date.year, date.monthValue - 1, date.dayOfMonth).show()
    }

    // Revisa los campos y confirma si se guarda o actualiza la tarea.
    private fun saveTask() {
        val id = txtId.text.toString().trim()
        val title = txtTitle.text.toString().trim()
        val date = dueDate
        val status = TaskStatus.values()[spnStatus.selectedItemPosition]
        val priority = TaskPriority.values()[spnPriority.selectedItemPosition]

        if (id.isEmpty() || title.isEmpty() || date == null) {
            showMessage(R.string.task_save_error)
            return
        }

        val existingTask = viewModel.getTaskById(id)
        // Solo se actualiza cuando la tarea fue cargada con Buscar.
        if (existingTask != null && loadedTaskId != id) {
            showMessage(R.string.task_duplicate_error)
            return
        }

        val task = Task(
            id = id,
            title = title,
            description = txtDescription.text.toString().trim(),
            dueDate = date,
            creationDate = existingTask?.creationDate ?: LocalDate.now(),
            status = status,
            assignedTo = txtAssignTo.text.toString().trim(),
            priority = priority,
            notes = txtObservation.text.toString().trim()
        )

        confirmAction(R.string.task_save_button) {
            if (existingTask == null) {
                viewModel.saveTask(task)
            } else {
                viewModel.updateTask(task)
            }
            loadedTaskId = task.id
            showMessage(R.string.task_save_success)
        }
    }

    // Impide eliminar tareas completas y pide confirmación.
    private fun deleteTask() {
        val task = viewModel.getTaskById(txtId.text.toString().trim())
        if (task == null) {
            showMessage(R.string.task_not_found)
            return
        }
        if (task.status == TaskStatus.COMPLETED) {
            showMessage(R.string.task_completed_action_error)
            return
        }

        confirmAction(R.string.task_delete_button) {
            viewModel.removeTask(task.id)
            clearForm()
            showMessage(R.string.task_delete_success)
        }
    }

    // vacia el formulario y selecciona Pendiente y Baja
    private fun clearForm() {
        txtId.text.clear()
        txtTitle.text.clear()
        txtDescription.text.clear()
        txtAssignTo.text.clear()
        txtObservation.text.clear()
        spnStatus.setSelection(TaskStatus.PENDING.ordinal)
        spnPriority.setSelection(TaskPriority.LOW.ordinal)
        tvDueDate.text = ""
        dueDate = null
        loadedTaskId = null
        txtId.requestFocus()
    }

    // reutilizo el mismo diálogo de Sí y No para ambas acciones
    private fun confirmAction(title: Int, action: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(R.string.task_action_confirmation)
            .setPositiveButton(R.string.dialog_yes) { _, _ -> action() }
            .setNegativeButton(R.string.dialog_no, null)
            .show()
    }

    // Reutilizo un Toast con el texto del idioma del dispositivo
    private fun showMessage(message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}