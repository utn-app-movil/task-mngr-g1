package cr.ac.utn.taskmanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cr.ac.utn.taskmanager.domain.model.Task

class TaskListAdapter (private var tasks: List<Task>,
                        val itemClickListener: OnItemClickListener):
                    RecyclerView.Adapter<CustomViewHolder>()
{
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int):
            CustomViewHolder {
        val itemView = LayoutInflater.from(parent.context).
                inflate(R.layout.activity_task_item,
                        parent, false)
        return CustomViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val item = tasks[position]
        holder.bind(item, itemClickListener)
    }

    override fun getItemCount(): Int {
        return tasks.size
    }
}

class CustomViewHolder(view: View): RecyclerView.ViewHolder(view)
{
    val tvTitle = view.findViewById<TextView>(R.id.txtTitle_Item)
    val tvStatus = view.findViewById<TextView>(R.id.txtStatus_Item)
    val tvPriority = view.findViewById<TextView>(R.id.txtPriority_Item)
    val tvAssingTo = view.findViewById<TextView>(R.id.txtAssignTo_Item)
    val imgPicture = view.findViewById<ImageView>(R.id.imgPicture_Item)

    fun bind(task: Task, itemClickListener: OnItemClickListener){
        tvTitle.text = task.title
        tvStatus.text = task.status.name
        tvPriority.text = task.priority.name
        tvAssingTo.text = task.assignedTo
        imgPicture.setImageResource(R.drawable.ic_launcher_foreground)
        itemView.setOnClickListener{
            itemClickListener.onItemClick(task)
        }
    }
}