package cr.ac.utn.taskmanager.data.local

import androidx.room.withTransaction
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime

class TaskRoomRepository(
    private val database: AppDatabase
) : ITaskRepository {

    private val taskDao = database.taskDao()

    override fun getTasks(): Flow<List<Task>> {
        return taskDao.getAllTasks().map { entities ->
            entities.map { it.toDomain() }        }
    }

    override fun getEligibleParentTasks(): Flow<List<Task>> {
        return taskDao.getEligibleParentTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getById(id: Long): Task? {
        return taskDao.getTaskById(id)?.toDomain()
    }

    override suspend fun addTask(task: Task) {
        val newTask = task.copy(
            id = 0L,
            status = TaskStatus.PENDING,
            cancellationReason = null,
            statusChangeDate = null
        )
        taskDao.insertTask(newTask.toEntity())
    }

    override suspend fun updateTask(task: Task) {
        database.withTransaction {
            val current = taskDao.getTaskById(task.id)?.toDomain()
                ?: throw IllegalArgumentException("La tarea no existe")

            require(!current.status.isTerminal) {
                "Una tarea finalizada no se puede modificar"
            }
            require(task.status in current.status.allowedStatuses()) {
                "Cambio de estado no permitido"
            }

            val changed = current.status != task.status
            if (changed && task.status == TaskStatus.IN_PROGRESS) {
                require(!task.dueDate.isBefore(LocalDate.now())) {
                    "No se puede iniciar una tarea vencida"
                }
            }

            val reason = if (task.status == TaskStatus.CANCELLED) {
                val value = task.cancellationReason?.trim().orEmpty()
                require(value.isNotBlank()) {
                    "Indique la razón de cancelación"
                }
                value
            } else {
                null
            }

            val updated = task.copy(
                creationDate = current.creationDate,
                cancellationReason = reason,
                statusChangeDate = if (changed) LocalDateTime.now()
                    else current.statusChangeDate
            )
            taskDao.updateTask(updated.toEntity())
        }
    }

    override suspend fun deleteTask(id: Long) {
        database.withTransaction {
            val current = taskDao.getTaskById(id)?.toDomain()
                ?: throw IllegalArgumentException("La tarea no existe")
            require(!current.status.isTerminal) {
                "Una tarea finalizada no se puede eliminar"
            }
            taskDao.deleteTaskById(id)
        }
    }
}