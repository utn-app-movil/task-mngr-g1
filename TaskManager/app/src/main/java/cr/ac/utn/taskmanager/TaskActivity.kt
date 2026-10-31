package cr.ac.utn.taskmanager

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import cr.ac.utn.taskmanager.data.memory.TaskMemoryManager
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskPriority
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import cr.ac.utn.taskmanager.util.EXTRA_MESSAGE_ID
import cr.ac.utn.taskmanager.util.util
import cr.ac.utn.taskmanager.viewmodel.TaskViewModel
import java.time.LocalDate
import java.util.Calendar

class TaskActivity : AppCompatActivity(),
            DatePickerDialog.OnDateSetListener{

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
    private var isEditMode: Boolean=false
    private lateinit var taskViewModel: TaskViewModel
    private lateinit var menuDelete: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_task)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //taskViewModel = TaskViewModel(TaskMemoryManager())
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

        resetDate()
        val taskId = intent.getStringExtra(EXTRA_MESSAGE_ID)
        if (!taskId.isNullOrBlank()){
            txtId.setText(taskId)
            searchTask(taskId)
        }

        btnSearch.setOnClickListener(View.OnClickListener{ view ->
            searchTask(txtId.text.trim().toString())
        })

        btnDueDate.setOnClickListener(View.OnClickListener{ view ->
            showDatePickerDialog()
        })
    }

    override fun onDateSet(view: android.widget.DatePicker?, year: Int
                            , month: Int, dayOfMonth: Int) {
        dueDay=dayOfMonth
        dueMonth=month
        dueYear=year
        tvDueDate.text = getDateFormatString(dayOfMonth,
             month, year)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.crud_actions, menu)
        menuDelete = menu!!.findItem(R.id.mnuDelete)
        menuDelete.isVisible = isEditMode
        invalidateOptionsMenu()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mnuSave -> {
                saveTask()
                return true
            }
            R.id.mnuDelete -> {
                deleteTask()
                return true
            }
            R.id.mnuCancel -> {
                cleanScreen()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun resetDate(){
        val calendar = Calendar.getInstance()
        dueYear= calendar.get(Calendar.YEAR)
        dueMonth= calendar.get(Calendar.MONTH)
        dueDay= calendar.get(Calendar.DAY_OF_MONTH)
    }

    private fun showDatePickerDialog(){
        val datePickerDialog = DatePickerDialog(this,
            this, dueYear, dueMonth, dueDay)
        datePickerDialog.show()
    }

    private fun searchTask(id: String){
        try {
            val task = TaskViewModel.getTaskById(id)
            if (task != null){
                isEditMode=true
                txtId.isEnabled=false
                txtTitle.setText(task.title)
                txtDescription.setText(task.description)
                txtStatus.setText(task.status.name)
                txtPriority.setText(task.priority.name)
                txtAssignTo.setText(task.assignedTo)
                txtObservation.setText(task.notes)
                val dueDateStr = getDateFormatString(task.dueDate.dayOfMonth,
                    task.dueDate.monthValue, task.dueDate.year)
                tvDueDate.setText(dueDateStr)
                dueDay = task.dueDate.dayOfMonth
                dueMonth = task.dueDate.monthValue
                dueYear = task.dueDate.year
                invalidateOptionsMenu()
            }else{
                Toast.makeText(this, getString(R.string.TextDataNoFound),
                    Toast.LENGTH_LONG).show()
            }
        }catch (e: Exception){
            cleanScreen()
            Toast.makeText(this, e.message.toString(),
                Toast.LENGTH_LONG).show()
        }
    }

    private fun getDateFormatString(dayOfMonth: Int, monthValue: Int, yearValue: Int): String{
        return "${if (dayOfMonth < 10) "0" else ""}$dayOfMonth/${if (monthValue < 10) "0" else ""}$monthValue/$yearValue"
    }

    private fun cleanScreen(){
        resetDate()
        isEditMode=false
        txtId.isEnabled = true
        txtId.setText("")
        txtTitle.setText("")
        txtDescription.setText("")
        txtStatus.setText("")
        txtPriority.setText("")
        txtAssignTo.setText("")
        txtObservation.setText("")
        tvDueDate.setText("")
        invalidateOptionsMenu()
    }

    fun isValidationData(): Boolean{
        return txtId.text.toString().trim().isNotBlank()
                && txtTitle.text.toString().trim().isNotBlank()
                && txtDescription.text.toString().trim().isNotBlank()
                && txtStatus.text.toString().trim().isNotBlank()
                && txtPriority.text.toString().trim().isNotBlank()
                && tvDueDate.text.toString().trim().isNotBlank()
                && txtObservation.text.toString().trim().isNotBlank()
                && txtAssignTo.text.toString().trim().isNotBlank()
    }

    fun saveTask(){
        try {
            if (isValidationData()){
                //val task = taskViewModel.getTaskById(txtId.text.toString().trim())
                val task = TaskViewModel.getTaskById(txtId.text.toString().trim())
                if (task != null
                    && !isEditMode){
                    Toast.makeText(this, getString(R.string.TextDuplicateData)
                        , Toast.LENGTH_LONG).show()
                }else{
                    val dueDate = util.parseStringToDateModern(tvDueDate.text.toString(),
                        "dd/MM/yyyy")

                    val task = Task(txtId.text.toString().trim(),
                        txtTitle.text.toString().trim(),
                        txtDescription.text.toString().trim(),
                        LocalDate.of(dueDate?.year!!, dueDate?.month!!
                            , dueDate?.dayOfMonth!!),
                        LocalDate.now(),
                        TaskStatus.valueOf(txtStatus.text.toString().uppercase()),
                        txtAssignTo.text.toString().trim(),
                        TaskPriority.valueOf(txtPriority.text.toString().uppercase()),
                        txtObservation.text.toString().trim())

                    if (!isEditMode)
                        TaskViewModel.saveTask(task)
                    else
                        TaskViewModel.updateTask(task)

                    cleanScreen()

                    Toast.makeText(this, getString(R.string.TextSaveSuccess)
                        , Toast.LENGTH_LONG).show()
                }
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
            TaskViewModel.removeTask(txtId.text.toString())
            cleanScreen()
            Toast.makeText(this, getString(R.string.TextDeleteSuccess)
                , Toast.LENGTH_LONG).show()
        }catch (e: Exception){
            Toast.makeText(this, e.message.toString()
                , Toast.LENGTH_LONG).show()
        }
    }
}