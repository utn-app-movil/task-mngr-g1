package cr.ac.utn.taskmanager.data.local

import androidx.room.withTransaction
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskError
import cr.ac.utn.taskmanager.domain.model.TaskRules
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime

class TaskRoomRepository(private val database: AppDatabase) : ITaskRepository {
    private val taskDao = database.taskDao()

    override fun getTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities -> entities.map { it.toDomain() } }
    }

    override fun getEligibleParentTasks(): Flow<List<Task>> {
        return taskDao.getEligibleParentTasks().map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getById(id: String): Task? {
        val taskId = id.trim().toLongOrNull() ?: return null
        return taskDao.getTaskById(taskId)?.toDomain()
    }

    override suspend fun addTask(task: Task): TaskError? {
        return database.withTransaction {
            val error = TaskRules.validate(task, null, LocalDate.now())
            if (error != null) return@withTransaction error

            if (task.parentTaskId != null) {
                val parent = getById(task.parentTaskId)
                if (parent == null || TaskRules.isClosed(parent)) {
                    return@withTransaction TaskError.INVALID_PARENT
                }
            }
            // Room asigna el siguiente ID, incluso después de eliminar tareas.
            val newTask = task.copy(
                id = "",
                status = TaskStatus.PENDING,
                creationDate = LocalDate.now(),
                cancellationReason = "",
                statusChangeDate = LocalDateTime.now()
            )
            taskDao.insertTask(newTask.toEntity())
            null
        }
    }

    override suspend fun updateTask(task: Task): TaskError? {
        return database.withTransaction {
            val original = getById(task.id) ?: return@withTransaction TaskError.TASK_NOT_FOUND
            val error = TaskRules.validate(task, original, LocalDate.now())
            if (error != null) return@withTransaction error

            val updatedTask = task.copy(
                creationDate = original.creationDate,
                statusChangeDate = if (task.status != original.status) {
                    LocalDateTime.now()
                } else {
                    original.statusChangeDate
                },
                cancellationReason = if (task.status == TaskStatus.CANCELLED) {
                    task.cancellationReason.trim()
                } else {
                    ""
                }
            )
            taskDao.updateTask(updatedTask.toEntity())
            null
        }
    }

    override suspend fun deleteTask(id: String): TaskError? {
        return database.withTransaction {
            val task = getById(id) ?: return@withTransaction TaskError.TASK_NOT_FOUND
            if (TaskRules.isClosed(task)) return@withTransaction TaskError.CLOSED_TASK
            if (taskDao.countSubtasks(task.id.toLong()) > 0) {
                return@withTransaction TaskError.HAS_SUBTASKS
            }
            taskDao.deleteTaskById(task.id.toLong())
            null
        }
    }
}
