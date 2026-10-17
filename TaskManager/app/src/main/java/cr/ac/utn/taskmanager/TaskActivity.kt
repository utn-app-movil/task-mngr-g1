package cr.ac.utn.taskmanager

import android.app.DatePickerDialog
import android.os.Bundle
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
import java.util.Calendar

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_task)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        txtId= findViewById<EditText>(R.id.txtId)
        txtTitle= findViewById<EditText>(R.id.txtTitleTask)
        txtDescription= findViewById<EditText>(R.id.txtDescription)
        txtStatus= findViewById<EditText>(R.id.txtStatus_Task)
        txtPriority= findViewById<EditText>(R.id.txtPriority_Task)
        txtObservation= findViewById<EditText>(R.id.txtObservation_Task)
        txtAssignTo= findViewById<EditText>(R.id.txtAssignTo_Task)
        tvDueDate= findViewById<TextView>(R.id.tvDueDate_Task)

        val btnSearch =  findViewById<ImageButton>(R.id.btnSearch)
        val btnDueDate = findViewById<ImageButton>(R.id.btnDueDate_Task)
        val btnSave = findViewById<Button>(R.id.btnSave_Task)
        val btnRemove = findViewById<Button>(R.id.btnRemove_Task)
        val btnCancel = findViewById<Button>(R.id.btnCancel_Task)
        // Evento del botón Buscar.
        btnSearch.setOnClickListener {
            Toast.makeText(
                this,
                R.string.action_search,
                Toast.LENGTH_SHORT
            ).show()
        }

// Evento del botón para seleccionar fecha.
        btnDueDate.setOnClickListener {
            val calendar = Calendar.getInstance()

            DatePickerDialog(
                this,
                { _, year, month, day ->
                    dueYear = year
                    dueMonth = month + 1
                    dueDay = day

                    tvDueDate.text = getString(
                        R.string.task_date_format,
                        dueDay,
                        dueMonth,
                        dueYear
                    )
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

// Evento del botón Guardar.
        btnSave.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.action_save)
                .setMessage(R.string.dialog_confirm_message)
                .setPositiveButton(R.string.dialog_yes) { _, _ ->
                    Toast.makeText(
                        this,
                        R.string.action_save,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton(R.string.dialog_no, null)
                .show()
        }

// Evento del botón Eliminar.
        btnRemove.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.action_delete)
                .setMessage(R.string.dialog_confirm_message)
                .setPositiveButton(R.string.dialog_yes) { _, _ ->
                    Toast.makeText(
                        this,
                        R.string.action_delete,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton(R.string.dialog_no, null)
                .show()
        }

// Evento del botón Limpiar.
        btnCancel.setOnClickListener {
            txtId.text.clear()
            txtTitle.text.clear()
            txtDescription.text.clear()
            txtStatus.text.clear()
            txtPriority.text.clear()
            txtAssignTo.text.clear()
            txtObservation.text.clear()

            tvDueDate.text = ""
            dueDay = 0
            dueMonth = 0
            dueYear = 0
        }


    }
}