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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class TaskViewModel(application: Application) :
    AndroidViewModel(application) {

    private val repository: ITaskRepository =
        (application as TaskManagerApp).taskRepository

    val tasks: StateFlow<List<Task>> = repository.getTasks()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val eligibleParentTasks: StateFlow<List<Task>> =
        repository.getEligibleParentTasks()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    suspend fun getTaskById(id: String): Task? {
        return repository.getById(id)
    }

    fun saveTask(
        task: Task,
        onComplete: () -> Unit,
        onError: (Int) -> Unit
    ) {
        viewModelScope.launch {
            try {

                if (task.assignedTo.isBlank()) {
                    onError(R.string.TextAssignedPersonRequired)
                    return@launch
                }

                val parentId = task.parentTaskId

                if (parentId != null) {

                    val parentTask = repository.getById(parentId)

                    if (parentTask == null) {
                        onError(R.string.TextParentNotFound)
                        return@launch
                    }

                    if (
                        parentTask.status != TaskStatus.PENDING &&
                        parentTask.status != TaskStatus.IN_PROGRESS
                    ) {
                        onError(R.string.TextInvalidParentStatus)
                        return@launch
                    }
                }

                repository.addTask(task)
                onComplete()

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(R.string.TextTaskOperationError)
            }
        }
    }

    fun updateTask(
        task: Task,
        onComplete: () -> Unit,
        onError: (Int) -> Unit
    ) {
        viewModelScope.launch {
            try {

                val currentTask = repository.getById(task.id)

                if (currentTask == null) {
                    onError(R.string.TextTaskNotFound)
                    return@launch
                }

                val validationError =
                    validateTaskUpdate(currentTask, task)

                if (validationError != null) {
                    onError(validationError)
                    return@launch
                }

                repository.updateTask(task)
                onComplete()

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(R.string.TextTaskOperationError)
            }
        }
    }

    private fun validateTaskUpdate(
        currentTask: Task,
        updatedTask: Task
    ): Int? {

        if (
            currentTask.status == TaskStatus.COMPLETED ||
            currentTask.status == TaskStatus.CANCELLED
        ) {
            return R.string.TextTaskCannotBeModified
        }

        if (updatedTask.assignedTo.isBlank()) {
            return R.string.TextAssignedPersonRequired
        }

        val currentStatus = currentTask.status
        val newStatus = updatedTask.status

        if (currentStatus != newStatus) {

            val allowedStatuses = when (currentStatus) {

                TaskStatus.PENDING -> setOf(
                    TaskStatus.IN_PROGRESS,
                    TaskStatus.CANCELLED
                )

                TaskStatus.IN_PROGRESS -> setOf(
                    TaskStatus.COMPLETED,
                    TaskStatus.CANCELLED
                )

                TaskStatus.COMPLETED,
                TaskStatus.CANCELLED -> emptySet()
            }

            if (newStatus !in allowedStatuses) {
                return R.string.TextInvalidStatusChange
            }

            if (
                newStatus == TaskStatus.IN_PROGRESS &&
                updatedTask.dueDate.isBefore(LocalDate.now())
            ) {
                return R.string.TextOverdueTask
            }

            if (
                newStatus == TaskStatus.CANCELLED &&
                updatedTask.cancellationReason.isNullOrBlank()
            ) {
                return R.string.TextCancellationReasonRequired
            }
        }

        return null
    }

    fun removeTask(
        id: String,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            repository.deleteTask(id)
            onComplete()
        }
    }

    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(
            modelClass: Class<T>
        ): T {
            if (
                modelClass.isAssignableFrom(
                    TaskViewModel::class.java
                )
            ) {
                @Suppress("UNCHECKED_CAST")
                return TaskViewModel(application) as T
            }

            throw IllegalArgumentException()

        }
    }
}