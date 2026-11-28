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
        holder.bind(item, tasks.count { it.parentTaskId == item.id }, itemClickListener)
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

    val tvParent: TextView = view.findViewById(R.id.txtParent_Item)
    val tvSubtasks: TextView = view.findViewById(R.id.txtSubtasks_Item)

    fun bind(task: Task, subtaskCount: Int, itemClickListener: OnItemClickListener) {
        tvTitle.text = itemView.context.getString(R.string.TextListTitle, task.id, task.title)
        tvStatus.text = task.status.name
        tvPriority.text = task.priority.name
        tvAssingTo.text = task.assignedTo
        tvParent.visibility = if (task.parentTaskId == null) View.GONE else View.VISIBLE
        tvParent.text = itemView.context.getString(R.string.TextParentId, task.parentTaskId)
        tvSubtasks.text = itemView.context.getString(R.string.TextSubtaskCount, subtaskCount)
        imgPicture.setImageResource(R.drawable.ic_launcher_foreground)
        itemView.setOnClickListener {
            itemClickListener.onItemClick(task)
        }
    }
}
