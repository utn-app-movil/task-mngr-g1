package cr.ac.utn.taskmanager

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.util.EXTRA_MESSAGE_ID
import cr.ac.utn.taskmanager.util.util
import cr.ac.utn.taskmanager.viewmodel.TaskViewModel

class TaskListActivity : AppCompatActivity(), OnItemClickListener {

    private lateinit var taskViewModel: TaskViewModel
    private lateinit var customAdapter: TaskListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_task_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val recycler = findViewById<RecyclerView>(R.id.rvTasks_List)
        taskViewModel = util.buildPersonViewModel()

        customAdapter = TaskListAdapter(taskViewModel.getTasks(), this)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = customAdapter

        // BUG INTENCIONAL #5: Falta llamar a customAdapter.notifyDataSetChanged() aquí 
        // y falta recargar la lista en onResume() al regresar de TaskActivity tras guardar o editar tareas.
    }

    override fun onItemClicked(task: Task) {
        util.openActivity(this, TaskActivity::class.java, EXTRA_MESSAGE_ID, task.id)
    }
}
