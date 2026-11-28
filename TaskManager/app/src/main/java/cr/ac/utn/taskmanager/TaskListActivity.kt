package cr.ac.utn.taskmanager

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.util.EXTRA_MESSAGE_ID
import cr.ac.utn.taskmanager.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

class TaskListActivity : AppCompatActivity(),
    OnitemClickListener {

    private lateinit var taskAdapter: TaskListAdapter

    private val taskViewModel: TaskViewModel by viewModels {
        TaskViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_task_list)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->

            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        val recyclerView =
            findViewById<RecyclerView>(R.id.rvTasks)

        taskAdapter = TaskListAdapter(
            emptyList(),
            this
        )

        recyclerView.layoutManager =
            LinearLayoutManager(this)

        recyclerView.adapter = taskAdapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                taskViewModel.tasks.collect { tasks ->
                    taskAdapter.updateTasks(tasks)
                }
            }
        }
    }

    override fun onItemClick(task: Task) {

        val intent = Intent(
            this,
            TaskActivity::class.java
        )

        intent.putExtra(EXTRA_MESSAGE_ID, task.id)

        startActivity(intent)
    }
}