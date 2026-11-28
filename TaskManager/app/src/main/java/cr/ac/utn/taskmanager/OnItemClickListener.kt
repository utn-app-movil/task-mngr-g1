package cr.ac.utn.taskmanager

import cr.ac.utn.taskmanager.domain.model.Task

interface OnitemClickListener {
    fun onItemClick(task: Task)
}
