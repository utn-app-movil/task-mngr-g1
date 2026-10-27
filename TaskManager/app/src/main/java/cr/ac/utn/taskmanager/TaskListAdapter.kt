package cr.ac.utn.taskmanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cr.ac.utn.taskmanager.domain.model.Task

class TaskListAdapter (private var itemList: List<Task>,
                    val itemClickListener: OnItemClickListener):
    RecyclerView.Adapter<CustomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val itemView = LayoutInflater.from(parent.context).
                inflate(R.layout.activity_task_item, parent, false)
        return CustomViewHolder(itemView)

    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val item = itemList[position]
        holder.bind(item, itemClickListener)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }
}

class CustomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val txtTitle: TextView = view.findViewById(R.id.txtTitle_Item)
    val txtStatus: TextView = view.findViewById(R.id.txtStatus_Item)
    val txtPriority: TextView = view.findViewById(R.id.txtPriority_Item)
    val txtAssignTo: TextView = view.findViewById(R.id.txtAssignTo_Item)
    val imgPicture: ImageView = view.findViewById(R.id.imgPicture_Item)

    fun bind (item: Task, clickListener: OnItemClickListener) {
        txtTitle.text = item.title
        txtStatus.text = item.status.toString()
        txtPriority.text = item.priority.toString()
        txtAssignTo.text = item.assignedTo

        itemView.setOnClickListener {
            clickListener.onItemClicked(item)
        }
    }
}