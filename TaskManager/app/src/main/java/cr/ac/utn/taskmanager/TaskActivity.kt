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
    private var dueDay: Int = 0
    private var dueMonth: Int = 0
    private var dueYear: Int = 0

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
        txtTitle = findViewById(R.id.txTitle_Task)
        txtDescription = findViewById(R.id.txtDescription_Task)
        txtStatus = findViewById(R.id.txtStatus_Task)
        txtPriority = findViewById(R.id.txtPriority_Task)
        txtObservation = findViewById(R.id.txtObservation_Task)
        txtAssignTo = findViewById(R.id.txtAssignTo_Task)
        tvDueDate = findViewById(R.id.tvDueDate_Task)

        val btnSearch = findViewById<ImageButton>(R.id.btnSearch_Task)
        val btnDueDate = findViewById<ImageButton>(R.id.btnDueDate_Task)
        val btnSave = findViewById<Button>(R.id.btnSave_Task)
        val btnRemove = findViewById<Button>(R.id.btnRemove_Task)
        val btnCancel = findViewById<Button>(R.id.btnCancel_Task)

        btnSearch.setOnClickListener {
            val id = txtId.text.toString().trim()
            if (id.isEmpty()) {
                Toast.makeText(this, R.string.msg_data_not_found, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.msg_data_not_found, Toast.LENGTH_SHORT).show()
            }
        }

        btnDueDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                dueYear = selectedYear
                dueMonth = selectedMonth + 1
                dueDay = selectedDay
                tvDueDate.text = "$dueDay/$dueMonth/$dueYear"
            }, year, month, day).show()
        }

        btnSave.setOnClickListener {
            val id = txtId.text.toString().trim()
            if (id.isEmpty()) {
                Toast.makeText(this, R.string.msg_error_save, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setMessage(R.string.dialog_confirm_action)
                .setPositiveButton(R.string.dialog_btn_yes) { _, _ ->
                    Toast.makeText(this, R.string.msg_save_success, Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.dialog_btn_no, null)
                .show()
        }

        btnRemove.setOnClickListener {
            val status = txtStatus.text.toString().trim().uppercase()
            if (status == "COMPLETED" || status == "COMPLETA") {
                Toast.makeText(this, R.string.msg_error_completed_task, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val id = txtId.text.toString().trim()
            if (id.isEmpty()) {
                Toast.makeText(this, R.string.msg_data_not_found, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setMessage(R.string.dialog_confirm_action)
                .setPositiveButton(R.string.dialog_btn_yes) { _, _ ->
                    Toast.makeText(this, R.string.msg_delete_success, Toast.LENGTH_SHORT).show()
                    clearFields()
                }
                .setNegativeButton(R.string.dialog_btn_no, null)
                .show()
        }

        btnCancel.setOnClickListener {
            clearFields()
            Toast.makeText(this, "Operación cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearFields() {
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
