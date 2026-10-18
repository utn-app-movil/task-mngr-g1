package cr.ac.utn.taskmanager

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
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.util.util
import cr.ac.utn.taskmanager.viewmodel.TaskViewModel

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

        txtId = findViewById<EditText>(R.id.txtId_Task)
        txtTitle = findViewById<EditText>(R.id.txtTitle_Task)
        txtDescription = findViewById<EditText>(R.id.txtDescription_Task)
        txtStatus = findViewById<EditText>(R.id.txtStatus_Task)
        txtPriority = findViewById<EditText>(R.id.txtPriority_Task)
        txtObservation = findViewById<EditText>(R.id.txtObservation_Task)
        txtAssignTo = findViewById<EditText>(R.id.txtAssignTo_Task)
        tvDueDate = findViewById<TextView>(R.id.tvDueDate_Task)

        resetDate()

        val btnSearch = findViewById<ImageButton>(R.id.btnSearch_Task)
        val btnDueDate = findViewById<ImageButton>(R.id.btnDueDate_Task)
        val btnSave= findViewById<Button>(R.id.btnSave_Task)
        val btnRemove= findViewById<Button>(R.id.btnRemove_Task)
        val btnCancel= findViewById<Button>(R.id.btnCancel_Task)

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
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

    private fun resetDate(){

    }

    private fun showDatePickerDialog(){

    }

    private fun searchTask(id: String){
        try {
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

        invalidateOptionsMenu()
    }

    fun isValidationData(): Boolean{
        val dateparse = util.parseStringToDateModern(tvDueDate.text.toString(), "dd/MM/yyyy")
        return false
    }

    fun saveTask(){
        try {
            if (isValidationData()){
                val task = taskViewModel.getTaskById(txtId.text.toString().trim())
                if (task != null
                    && !isEditMode){
                    Toast.makeText(this, getString(R.string.TextDuplicateData)
                        , Toast.LENGTH_LONG).show()
                }else{
                    val dueDate = util.parseStringToDateModern(tvDueDate.text.toString(),
                        "dd/MM/yyyy")


                    val task = Task()

                    if (!isEditMode)
                        taskViewModel.saveTask(task)
                    else
                        taskViewModel.updateTask(task)

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