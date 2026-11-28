package cr.ac.utn.taskmanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cr.ac.utn.taskmanager.domain.model.Task

class TaskListAdapter(
    private var tasks: List<Task>,
    val itemClickListener: OnItemClickListener
) : RecyclerView.Adapter<CustomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_task_item, parent, false)
        return CustomViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val item = tasks[position]
        val subtasks = tasks.filter { it.parentTaskId == item.id }
        if (subtasks.isNotEmpty()) {
            val titles = subtasks.joinToString("\n") { "• ${it.title}" }
            holder.tvTitle.text = "${item.title}\nSubtareas:\n$titles"
        }
        holder.bind(item, itemClickListener)
    }

    override fun getItemCount(): Int {
        return tasks.size
    }

    fun updateTasks(newTasks: List<Task>) {
        this.tasks = newTasks
        notifyDataSetChanged()
    }
}

class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val tvTitle: TextView = view.findViewById(R.id.txtTitle_Item)
    val tvStatus: TextView = view.findViewById(R.id.txtStatus_Item)
    val tvPriority: TextView = view.findViewById(R.id.txtPriority_Item)
    val tvAssingTo: TextView = view.findViewById(R.id.txtAssignTo_Item)
    val imgPicture: ImageView = view.findViewById(R.id.imgPicture_Item)

    fun bind(task: Task, itemClickListener: OnItemClickListener) {
        tvTitle.text = task.title
        tvStatus.text = task.status.name
        tvPriority.text = task.priority.name
        tvAssingTo.text = task.assignedTo
        imgPicture.setImageResource(R.drawable.ic_launcher_foreground)
        itemView.setOnClickListener {
            itemClickListener.onItemClick(task)
        }
    }
}
