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
import cr.ac.utn.taskmanager.domain.rules.TaskRuleException
import cr.ac.utn.taskmanager.domain.rules.TaskRuleViolation
import cr.ac.utn.taskmanager.domain.rules.TaskRules
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ITaskRepository =
        (application as TaskManagerApp).taskRepository

    val tasks: StateFlow<List<Task>> = repository.getTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val eligibleParentTasks: StateFlow<List<Task>> = repository.getEligibleParentTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun getTaskById(id: Long): Task? = repository.getById(id)

    fun createTask(
        task: Task,
        onSuccess: () -> Unit,
        onError: (TaskRuleViolation) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val parent = task.parentTaskId?.let { repository.getById(it) }
                val newTask = task.copy(
                    id = 0, // auto-incremental by the database
                    status = TaskStatus.PENDING,
                    cancellationReason = null,
                    statusChangedAt = LocalDateTime.now()
                )
                TaskRules.validateCreate(newTask, parent)
                repository.addTask(newTask)
                onSuccess()
            } catch (e: TaskRuleException) {
                onError(e.violation)
            }
        }
    }

    fun updateTask(
        task: Task,
        onSuccess: () -> Unit,
        onError: (TaskRuleViolation) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val current = repository.getById(task.id)
                    ?: throw TaskRuleException(TaskRuleViolation.TASK_NOT_FOUND)

                val statusChanged = task.status != current.status
                val toSave = task.copy(
                    creationDate = current.creationDate,
                    parentTaskId = current.parentTaskId,
                    cancellationReason = if (task.status == TaskStatus.CANCELLED)
                        task.cancellationReason?.trim() else null,
                    statusChangedAt = if (statusChanged) LocalDateTime.now()
                    else current.statusChangedAt
                )
                TaskRules.validateUpdate(current, toSave)
                repository.updateTask(toSave)
                onSuccess()
            } catch (e: TaskRuleException) {
                onError(e.violation)
            }
        }
    }

    fun removeTask(
        id: Long,
        onSuccess: () -> Unit,
        onError: (TaskRuleViolation) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val current = repository.getById(id)
                    ?: throw TaskRuleException(TaskRuleViolation.TASK_NOT_FOUND)
                TaskRules.validateDelete(current)
                repository.deleteTask(id)
                onSuccess()
            } catch (e: TaskRuleException) {
                onError(e.violation)
            }
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