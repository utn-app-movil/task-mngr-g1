package cr.ac.utn.taskmanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cr.ac.utn.taskmanager.TaskManagerApp
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ITaskRepository = (application as TaskManagerApp).taskRepository

    val tasks: StateFlow<List<Task>> = repository.getTasks()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val eligibleParentTasks: StateFlow<List<Task>> = repository.getEligibleParentTasks()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    suspend fun getTaskById(id: String): Task? {
        return repository.getById(id)
    }

    fun saveTask(task: Task, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val taskToInsert = task.copy(
                status = TaskStatus.PENDING,
                creationDate = LocalDate.now(),
                statusChangeDateTime = LocalDateTime.now()
            )

            val existing = repository.getById(taskToInsert.id)
            if (existing != null) {
                onResult(false, "La tarea ya existe")
                return@launch
            }

            repository.addTask(taskToInsert)
            onResult(true, null)
        }
    }

    fun updateTask(task: Task, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val existingTask = repository.getById(task.id)
            if (existingTask == null) {
                onResult(false, "La tarea no existe")
                return@launch
            }

            if (existingTask.status == TaskStatus.COMPLETED || existingTask.status == TaskStatus.CANCELLED) {
                onResult(false, "Una tarea completada o cancelada no se puede modificar")
                return@launch
            }

            val currentStatus = existingTask.status
            val newStatus = task.status

            if (currentStatus == TaskStatus.IN_PROGRESS && newStatus == TaskStatus.PENDING) {
                onResult(false, "Una tarea en progreso no puede moverse a estado Pendiente")
                return@launch
            }

            if (newStatus == TaskStatus.IN_PROGRESS && task.dueDate.isBefore(LocalDate.now())) {
                onResult(false, "No se puede mover a En Progreso porque la fecha de vencimiento es menor a la fecha actual")
                return@launch
            }

            if (newStatus == TaskStatus.CANCELLED && task.cancellationReason.isNullOrBlank()) {
                onResult(false, "Debe indicar la razón de cancelación")
                return@launch
            }

            val updatedStatusDateTime = if (currentStatus != newStatus) {
                LocalDateTime.now()
            } else {
                existingTask.statusChangeDateTime
            }

            val taskToUpdate = task.copy(
                statusChangeDateTime = updatedStatusDateTime
            )

            repository.updateTask(taskToUpdate)
            onResult(true, null)
        }
    }

    fun removeTask(id: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val existingTask = repository.getById(id)
            if (existingTask != null && (existingTask.status == TaskStatus.COMPLETED || existingTask.status == TaskStatus.CANCELLED)) {
                onResult(false, "Una tarea completada o cancelada no se puede eliminar/modificar")
                return@launch
            }

            repository.deleteTask(id)
            onResult(true, null)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
                return TaskViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
