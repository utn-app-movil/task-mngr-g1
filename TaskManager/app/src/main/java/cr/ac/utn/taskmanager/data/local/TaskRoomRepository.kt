package cr.ac.utn.taskmanager.data.local

import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository
import cr.ac.utn.taskmanager.util.TaskValidation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TaskRoomRepository(private val taskDao: TaskDao):
    ITaskRepository {

    override fun getTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getEligibleParentTasks(): Flow<List<Task>> {
        return getTasks().map { tasks ->
            tasks.filter { task ->
                task.status == TaskStatus.PENDING ||
                        task.status == TaskStatus.IN_PROGRESS
            }
        }
    }

    override suspend fun getById(id: String): Task? {
        return taskDao.getTaskById(id.trim())?.toDomain()
    }

    override suspend fun addTask(task: Task) {

        val newTask = task.copy(
            status = TaskStatus.PENDING,
            cancellationReason = ""
        )

        taskDao.insertTaskWithGeneratedId(newTask.toEntity())
    }

    override suspend fun updateTask(task: Task) {

        val existingTask = getById(task.id)
            ?: throw IllegalArgumentException("Task not found.")

        val validationError =
            TaskValidation.validateStatusChange(
                existingTask.status,
                task.status,
                task.dueDate
            )

        if (validationError != null) {
            throw IllegalArgumentException(validationError)
        }

        val statusChanged =
            existingTask.status != task.status

        if (statusChanged &&
            task.status == TaskStatus.CANCELLED &&
            task.cancellationReason.isBlank()
        ) {
            throw IllegalArgumentException(
                "Cancellation reason is required."
            )
        }

        val timestamp = System.currentTimeMillis()

        val updatedTask = task.copy(
            creationDate = existingTask.creationDate,

            cancellationReason =
                if (task.status == TaskStatus.CANCELLED) {
                    task.cancellationReason.trim()
                } else {
                    ""
                },

            statusChangedAt =
                if (statusChanged) {
                    timestamp
                } else {
                    existingTask.statusChangedAt
                }
        )

        val history = if (statusChanged) {
            TaskStatusHistory(
                taskId = task.id,
                previousStatus = existingTask.status,
                newStatus = task.status,
                changedAt = timestamp,
                cancellationReason =
                    updatedTask.cancellationReason
            )
        } else {
            null
        }

        taskDao.updateTaskWithHistory(
            updatedTask.toEntity(),
            history
        )
    }

    override suspend fun deleteTask(id: String) {

        val existingTask = getById(id)
            ?: throw IllegalArgumentException("Task not found.")

        if (TaskValidation.isFinalStatus(existingTask.status)) {
            throw IllegalArgumentException(
                "A completed or cancelled task cannot be deleted."
            )
        }

        taskDao.deleteTaskById(id.trim())
    }
}