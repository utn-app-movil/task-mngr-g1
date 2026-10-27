package cr.ac.utn.taskmanager

import cr.ac.utn.taskmanager.domain.model.Task

interface OnItemClickListener {
    fun onItemClicked (task: Task)
}