
package cr.ac.utn.taskmanager

import android.os.Bundle
import android.app.AlertDialog
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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

        txtId = findViewById<EditText>(R.id.txtId_Task)
        txtTitle = findViewById<EditText>(R.id.txtTitle_Task)
        txtDescription = findViewById<EditText>(R.id.txtDescription)
        txtStatus = findViewById<EditText>(R.id.txtStatus_Task)
        txtPriority = findViewById<EditText>(R.id.txtPriority_Task)
        txtObservation = findViewById<EditText>(R.id.txtObservation_Task)
        txtAssignTo = findViewById<EditText>(R.id.txtAssignTo_Task)
        tvDueDate = findViewById<TextView>(R.id.tvDueDate_Task)

        val btnSearch = findViewById<ImageButton>(R.id.btnSearch_Task)
        val btnDueDate = findViewById<ImageButton>(R.id.btnDueDate_Task)
        val btnSave = findViewById<Button>(R.id.btnSave_Task)
        val btnRemove = findViewById<Button>(R.id.btnRemove_Task)
        val btnCancel = findViewById<Button>(R.id.btnCancel_Task)
        val btnSearchText = findViewById<Button>(R.id.btnSearchText_Task)

        btnSave.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage(R.string.dialog_confirmation)
                .setPositiveButton(R.string.dialog_yes) { _, _ ->
                    Toast.makeText(
                        this,
                        R.string.msg_action_pending,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton(R.string.dialog_no, null)
                .show()
        }

        btnRemove.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage(R.string.dialog_confirmation)
                .setPositiveButton(R.string.dialog_yes) { _, _ ->
                    Toast.makeText(
                        this,
                        R.string.msg_action_pending,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton(R.string.dialog_no, null)
                .show()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnSearch.setOnClickListener {
            Toast.makeText(
                this,
                R.string.msg_action_pending,
                Toast.LENGTH_SHORT
            ).show()
        }

        btnSearchText.setOnClickListener {
            Toast.makeText(
                this,
                R.string.msg_action_pending,
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
