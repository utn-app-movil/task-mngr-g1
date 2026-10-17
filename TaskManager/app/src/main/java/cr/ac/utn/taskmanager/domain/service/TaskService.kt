package cr.ac.utn.taskmanager.domain.service

import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository
import cr.ac.utn.taskmanager.domain.result.TaskError
import cr.ac.utn.taskmanager.domain.result.TaskOperationResult

class TaskService(
    private val repository: ITaskRepository
) {

    fun getTasks(): TaskOperationResult<List<Task>> {
        return try {
            TaskOperationResult.Success(repository.getTasks().toList())
        } catch (exception: Exception) {
            TaskOperationResult.Failure(TaskError.STORAGE_FAILURE)
        }
    }

    fun getTaskById(id: String): TaskOperationResult<Task> {
        val normalizedId = id.trim()

        if (normalizedId.isBlank()) {
            return TaskOperationResult.Failure(TaskError.INVALID_ID)
        }

        return try {
            val task = repository.getById(normalizedId)
                ?: return TaskOperationResult.Failure(TaskError.TASK_NOT_FOUND)

            TaskOperationResult.Success(task)
        } catch (exception: Exception) {
            TaskOperationResult.Failure(TaskError.STORAGE_FAILURE)
        }
    }

    fun saveTask(task: Task): TaskOperationResult<Task> {
        val normalizedId = task.id.trim()
        val normalizedTitle = task.title.trim()

        if (normalizedId.isBlank()) {
            return TaskOperationResult.Failure(TaskError.INVALID_ID)
        }

        if (normalizedTitle.isBlank()) {
            return TaskOperationResult.Failure(TaskError.INVALID_TITLE)
        }

        val normalizedTask = task.copy(
            id = normalizedId,
            title = normalizedTitle
        )

        return try {
            if (repository.getById(normalizedId) != null) {
                return TaskOperationResult.Failure(TaskError.DUPLICATE_TASK)
            }

            repository.addTask(normalizedTask)
            TaskOperationResult.Success(normalizedTask)
        } catch (exception: Exception) {
            TaskOperationResult.Failure(TaskError.STORAGE_FAILURE)
        }
    }

    fun updateTask(task: Task): TaskOperationResult<Task> {
        val normalizedId = task.id.trim()
        val normalizedTitle = task.title.trim()

        if (normalizedId.isBlank()) {
            return TaskOperationResult.Failure(TaskError.INVALID_ID)
        }

        if (normalizedTitle.isBlank()) {
            return TaskOperationResult.Failure(TaskError.INVALID_TITLE)
        }

        return try {
            if (repository.getById(normalizedId) == null) {
                return TaskOperationResult.Failure(TaskError.TASK_NOT_FOUND)
            }

            val updatedTask = task.copy(
                id = normalizedId,
                title = normalizedTitle
            )

            repository.updateTask(updatedTask)
            TaskOperationResult.Success(updatedTask)
        } catch (exception: NoSuchElementException) {
            TaskOperationResult.Failure(TaskError.TASK_NOT_FOUND)
        } catch (exception: Exception) {
            TaskOperationResult.Failure(TaskError.STORAGE_FAILURE)
        }
    }

    fun deleteTask(id: String): TaskOperationResult<Unit> {
        val normalizedId = id.trim()

        if (normalizedId.isBlank()) {
            return TaskOperationResult.Failure(TaskError.INVALID_ID)
        }

        return try {
            val task = repository.getById(normalizedId)
                ?: return TaskOperationResult.Failure(TaskError.TASK_NOT_FOUND)

            if (task.status == TaskStatus.COMPLETED) {
                return TaskOperationResult.Failure(
                    TaskError.COMPLETED_TASK_RESTRICTION
                )
            }

            repository.deleteTask(normalizedId)
            TaskOperationResult.Success(Unit)
        } catch (exception: NoSuchElementException) {
            TaskOperationResult.Failure(TaskError.TASK_NOT_FOUND)
        } catch (exception: Exception) {
            TaskOperationResult.Failure(TaskError.STORAGE_FAILURE)
        }
    }

    fun cancelTask(id: String): TaskOperationResult<Task> {
        val normalizedId = id.trim()

        if (normalizedId.isBlank()) {
            return TaskOperationResult.Failure(TaskError.INVALID_ID)
        }

        return try {
            val task = repository.getById(normalizedId)
                ?: return TaskOperationResult.Failure(TaskError.TASK_NOT_FOUND)

            if (task.status == TaskStatus.COMPLETED) {
                return TaskOperationResult.Failure(
                    TaskError.COMPLETED_TASK_RESTRICTION
                )
            }

            val cancelledTask = task.copy(status = TaskStatus.CANCELLED)
            repository.updateTask(cancelledTask)

            TaskOperationResult.Success(cancelledTask)
        } catch (exception: NoSuchElementException) {
            TaskOperationResult.Failure(TaskError.TASK_NOT_FOUND)
        } catch (exception: Exception) {
            TaskOperationResult.Failure(TaskError.STORAGE_FAILURE)
        }
    }
}