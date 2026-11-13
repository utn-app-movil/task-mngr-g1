package cr.ac.utn.taskmanager

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
import cr.ac.utn.taskmanager.util.util
import cr.ac.utn.taskmanager.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

class TaskListActivity : AppCompatActivity(), OnItemClickListener {

    private val taskViewModel: TaskViewModel by viewModels {
        TaskViewModel.Factory(application)
    }

    private lateinit var taskAdapter: TaskListAdapter

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
        taskAdapter = TaskListAdapter(emptyList(), this)
        recycler.layoutManager = LinearLayoutManager(applicationContext)
        recycler.adapter = taskAdapter

        val progressDialog = util.showProgressDialog(this, getString(R.string.TextLoading))
        var isFirstLoad = true

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                taskViewModel.tasks.collect { tasks ->
                    taskAdapter.updateTasks(tasks)
                    if (isFirstLoad) {
                        isFirstLoad = false
                        progressDialog.dismiss()
                    }
                }
            }
        }
    }

    override fun onItemClick(task: Task) {
        util.openActivity(
            this,
            TaskActivity::class.java,
            EXTRA_MESSAGE_ID, task.id
        )
    }
}
