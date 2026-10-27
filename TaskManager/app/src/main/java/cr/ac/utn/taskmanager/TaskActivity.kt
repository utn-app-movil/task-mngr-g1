package cr.ac.utn.taskmanager

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.util.util
import cr.ac.utn.taskmanager.viewmodel.TaskViewModel
import java.time.LocalDate

import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import cr.ac.utn.taskmanager.domain.model.TaskPriority
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import cr.ac.utn.taskmanager.util.EXTRA_MESSAGE_ID
import kotlinx.coroutines.launch

class TaskActivity : AppCompatActivity() {
    private lateinit var txtId: EditText
    private lateinit var txtTitle: EditText
    private lateinit var txtDescription: EditText
    private lateinit var spStatus: Spinner
    private lateinit var spPriority: Spinner
    private lateinit var txtAssignTo: EditText
    private lateinit var txtObservation: EditText
    private lateinit var tvDueDate: TextView
    private var dueDay: Int=0
    private var dueMonth: Int=0
    private var dueYear: Int=0
    private var isEditMode: Boolean=false
    private lateinit var taskViewModel: TaskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_task)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        taskViewModel = util.buildPersonViewModel()
        txtId = findViewById<EditText>(R.id.txtId_Task)
        txtTitle = findViewById<EditText>(R.id.txtTitle_Task)
        txtDescription = findViewById<EditText>(R.id.txtDescription_Task)
        spStatus = findViewById(R.id.spStatus_Task)
        spPriority = findViewById(R.id.spPriority_Task)
        txtObservation = findViewById<EditText>(R.id.txtObservation_Task)
        txtAssignTo = findViewById<EditText>(R.id.txtAssignTo_Task)
        tvDueDate = findViewById<TextView>(R.id.tvDueDate_Task)

        setupSpinners()

        val btnSearch = findViewById<ImageButton>(R.id.btnSearch_Task)
        val btnDueDate = findViewById<ImageButton>(R.id.btnDueDate_Task)
        val btnSave= findViewById<Button>(R.id.btnSave_Task)
        val btnRemove= findViewById<Button>(R.id.btnRemove_Task)
        val btnCancel= findViewById<Button>(R.id.btnCancel_Task)

        resetDate()

        val taskId = intent.getStringExtra(EXTRA_MESSAGE_ID)
        if (taskId != null && taskId.trim().length == 0) searchTask(taskId)

        btnSearch.setOnClickListener(View.OnClickListener{ view ->
            searchTask(txtId.text.trim().toString())
        })

        btnDueDate.setOnClickListener(View.OnClickListener{ view ->
            showDatePickerDialog()
        })

        btnSave.setOnClickListener(View.OnClickListener{ view ->
            saveTask()
        })

        btnRemove.setOnClickListener(View.OnClickListener{ view ->
            deleteTask()
        })

        btnCancel.setOnClickListener(View.OnClickListener{ view ->
            cleanScreen()
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.selectedTask.collect { task ->
                    task?.let {
                        txtId.setText(it.id)
                        txtTitle.setText(it.title)
                        txtDescription.setText(it.description)
                        txtAssignTo.setText(it.assignedTo)
                        txtObservation.setText(it.notes)

                        spStatus.setSelection(it.status.ordinal)
                        spPriority.setSelection(it.priority.ordinal)
                    }
                }
            }
        }

        /*lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.selectedTask.collect {
                    it?.let { task ->
                        txtId.setText(task.id)
                        txtTitle.setText(task.title)
                        txtDescription.setText(task.description)
                        txtAssignedTo.setText(task.assignedTo)
                    }
                }
            }
        }*/

        /*lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.error.collect { error ->
                    error?.let {
                        Toast.makeText(this@TaskActivity, it, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }*/
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    private fun setupSpinners() {
        // Adaptador para TaskStatus
        val statusAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TaskStatus.entries.toTypedArray() // o TaskStatus.values() en Kotlin < 1.9
        )
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spStatus.adapter = statusAdapter

        // Adaptador para TaskPriority
        val priorityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            TaskPriority.entries.toTypedArray() // o TaskPriority.values()
        )
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPriority.adapter = priorityAdapter
    }

    private fun resetDate(){

    }

    private fun showDatePickerDialog(){

    }

    private fun searchTask(id: String){
        taskViewModel.getTaskById(id)
        /*try {
            val task = taskViewModel.getTaskById(id)
            if (task != null){
                isEditMode=true

            }else{
                Toast.makeText(this, getString(R.string.TextDataNoFound),
                    Toast.LENGTH_LONG).show()
            }
        }catch (e: Exception){
            cleanScreen()
            Toast.makeText(this, e.message.toString(),
                Toast.LENGTH_LONG).show()
        }*/
    }

    private fun getDateFormatString(dayOfMonth: Int, monthValue: Int, yearValue: Int): String{
        return "${if (dayOfMonth < 10) "0" else ""}$dayOfMonth/${if (monthValue < 10) "0" else ""}$monthValue/$yearValue"
    }

    private fun cleanScreen(){
        resetDate()
        isEditMode=false
        txtId.isEnabled = true
        txtId.setText("")

        // Resetear spinners
        spStatus.setSelection(0)
        spPriority.setSelection(0)

        invalidateOptionsMenu()
    }

    fun isValidationData(): Boolean{
        val dateparse = util.parseStringToDateModern(tvDueDate.text.toString(), "dd/MM/yyyy")
        return false
    }

    fun saveTask(){
        try {
            if (isValidationData()){
                /*val task = taskViewModel.getTaskById(txtId.text.toString().trim())
                if (task != null
                    && !isEditMode){
                    Toast.makeText(this, getString(R.string.TextDuplicateData)
                        , Toast.LENGTH_LONG).show()
                }else{*/
                    val dueDate = util.parseStringToDateModern(tvDueDate.text.toString(),
                        "dd/MM/yyyy")
                    // Obtener enum seleccionado directamente desde cada Spinner
                    val selectedStatus = spStatus.selectedItem as TaskStatus
                    val selectedPriority = spPriority.selectedItem as TaskPriority

                    val task = Task(
                        id = txtId.text.toString(),
                        title = txtTitle.text.toString(),
                        description = txtDescription.text.toString(),
                        dueDate = dueDate!!,
                        creationDate = LocalDate.now(),
                        status = selectedStatus,
                        assignedTo = txtAssignTo.text.toString(),
                        priority = selectedPriority,
                        notes = txtObservation.text.toString()
                    )
                    if (!isEditMode)
                        taskViewModel.saveTask(task)
                    else
                        taskViewModel.updateTask(task)

                    cleanScreen()

                    Toast.makeText(this, getString(R.string.TextSaveSuccess)
                        , Toast.LENGTH_LONG).show()
                //}
            }else{
                Toast.makeText(this, "Datos incompletos"
                    , Toast.LENGTH_LONG).show()
            }
        }catch (e: Exception){
            Toast.makeText(this, e.message.toString()
                , Toast.LENGTH_LONG).show()
        }
    }

    fun deleteTask(): Unit{
        try {
            taskViewModel.removeTask(txtId.text.toString())
            cleanScreen()
            Toast.makeText(this, getString(R.string.TextDeleteSuccess)
                , Toast.LENGTH_LONG).show()
        }catch (e: Exception){
            Toast.makeText(this, e.message.toString()
                , Toast.LENGTH_LONG).show()
        }
    }
}