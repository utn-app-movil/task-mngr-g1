package cr.ac.utn.taskmanager

import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import cr.ac.utn.taskmanager.domain.model.TaskPriority
import android.widget.Button
import android.widget.EditText

class TaskActivity : AppCompatActivity() {

    private lateinit var txtId: EditText
    private lateinit var txtTitle: EditText
    private lateinit var txtDescription: EditText
    private lateinit var txtStatus: EditText
    private lateinit var txtPriority: EditText
    private lateinit var txtAssignTo: EditText
    private lateinit var txtObservation: EditText
    private lateinit var tvDueDate: EditText

    private var DueDay: Int=0
    private var DueMonth: Int=0
    private var DueYear: Int=0


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
        txtStatus = findViewById(R.id.txtStatus_Task)
        txtPriority = findViewById(R.id.txtPriority_Task)
        txtObservation = findViewById(R.id.txtObservation_Task)
        tvDueDate = findViewById(R.id.tvDueDate_Task)
        txtAssignTo = findViewById(R.id.txtAssignTo_Task)

        val btnSearch= findViewById<ImageButton>(R.id.btnSearch_Task)
        val btnDueDate= findViewById<ImageButton>(R.id.btnDueDate_Task)
        val btnSave= findViewById<Button>(R.id.btnSave_Task)
        val btnRemove= findViewById<Button>(R.id.btnRemove_Task)
        val btnCancel= findViewById<Button>(R.id.btnCancel_Task)


        //Agregar los eventos click a cada botón
        btnSave.setOnClickListener {

    }

        btnRemove.setOnClickListener {

        }

        btnCancel.setOnClickListener {

        }

        btnDueDate.setOnClickListener {

        }

        btnSearch.setOnClickListener {

        }

        }


}