package cr.ac.utn.taskmanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cr.ac.utn.taskmanager.R
import cr.ac.utn.taskmanager.TaskManagerApp
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalDateTime

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ITaskRepository =
        (application as TaskManagerApp).taskRepository

    var errorMessage: Int = R.string.TextSaveError

    val tasks: StateFlow<List<Task>> = repository.getTasks().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val eligibleParentTasks: StateFlow<List<Task>> =
        repository.getEligibleParentTasks().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    suspend fun getTaskById(id: String): Task? {
        return repository.getById(id)
    }

    fun isClosed(task: Task): Boolean {
        return task.status == TaskStatus.COMPLETED || task.status == TaskStatus.CANCELLED
    }

    fun getAllowedStatuses(task: Task?): List<TaskStatus> {
        return when (task?.status) {
            null -> listOf(TaskStatus.PENDING)
            TaskStatus.PENDING -> listOf(
                TaskStatus.PENDING, TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED
            )
            TaskStatus.IN_PROGRESS -> listOf(
                TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED, TaskStatus.COMPLETED
            )
            else -> listOf(task.status)
        }
    }

    private fun validateTask(task: Task, original: Task?): Boolean {
        if (original != null && isClosed(original)) {
            errorMessage = R.string.TextClosedTask
            return false
        }
        if (task.title.isBlank() || task.description.isBlank() || task.assignedTo.isBlank()) {
            errorMessage = R.string.TextRequiredFields
            return false
        }
        if (!getAllowedStatuses(original).contains(task.status)) {
            errorMessage = R.string.TextInvalidStatus
            return false
        }
        if (task.status == TaskStatus.CANCELLED && task.cancellationReason.isBlank()) {
            errorMessage = R.string.TextCancellationRequired
            return false
        }
        if (task.status == TaskStatus.IN_PROGRESS && original?.status != TaskStatus.IN_PROGRESS
            && task.dueDate.isBefore(LocalDate.now())) {
            errorMessage = R.string.TextPastDueDate
            return false
        }
        if (original != null && task.parentTaskId != original.parentTaskId) {
            errorMessage = R.string.TextInvalidParent
            return false
        }
        return true
    }

    private fun prepareTask(task: Task, original: Task?): Task {
        var changedAt = original?.statusChangeDate
        if (original == null || task.status != original.status) {
            changedAt = LocalDateTime.now()
        }

        var reason = ""
        if (task.status == TaskStatus.CANCELLED) {
            reason = task.cancellationReason.trim()
        }

        return Task(
            id = original?.id ?: "",
            title = task.title.trim(),
            description = task.description.trim(),
            dueDate = task.dueDate,
            creationDate = original?.creationDate ?: LocalDate.now(),
            status = task.status,
            assignedTo = task.assignedTo,
            priority = task.priority,
            notes = task.notes,
            parentTaskId = task.parentTaskId,
            cancellationReason = reason,
            statusChangeDate = changedAt
        )
    }

    suspend fun saveTask(task: Task): Boolean {
        if (!validateTask(task, null)) return false

        val parentId = task.parentTaskId
        if (parentId != null) {
            val parent = repository.getById(parentId)
            if (parent == null || isClosed(parent)) {
                errorMessage = R.string.TextInvalidParent
                return false
            }
        }

        repository.addTask(prepareTask(task, null))
        return true
    }

    suspend fun updateTask(task: Task): Boolean {
        val original = repository.getById(task.id)
        if (original == null) {
            errorMessage = R.string.TextDataNoFound
            return false
        }
        if (!validateTask(task, original)) return false

        repository.updateTask(prepareTask(task, original))
        return true
    }

    suspend fun removeTask(id: String): Boolean {
        val task = repository.getById(id)
        if (task == null) {
            errorMessage = R.string.TextDataNoFound
            return false
        }
        if (isClosed(task)) {
            errorMessage = R.string.TextClosedTask
            return false
        }
        if (repository.hasSubtasks(task.id)) {
            errorMessage = R.string.TextHasSubtasks
            return false
        }

        repository.deleteTask(task.id)
        return true
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
                return TaskViewModel(application) as T
            } else {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
