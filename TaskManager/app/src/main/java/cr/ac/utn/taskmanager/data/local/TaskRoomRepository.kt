package cr.ac.utn.taskmanager.data.local

import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository
import java.time.LocalDate
import java.time.LocalDateTime
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
        return taskDao.getEligibleParentTasks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getById(id: Long): Task? {
        return taskDao.getTaskById(id)?.toDomain()
    }

    override suspend fun addTask(task: Task) {
        require(task.id == 0L) { "El ID debe generarse automaticamente" }
        require(task.status == TaskStatus.PENDING) { "La tarea debe iniciar en PENDING" }
        taskDao.insertTask(task.copy(cancellationReason = null, statusChangedAt = null).toEntity())
    }

    override suspend fun updateTask(task: Task) {
        val current = getById(task.id) ?: error("Tarea no encontrada")
        check(!current.status.isTerminal()) { "La tarea no se puede modificar" }
        require(task.status in current.status.allowedStatuses()) { "Cambio de estado no permitido" }
        val changed = current.status != task.status
        if (changed && task.status == TaskStatus.IN_PROGRESS) {
            require(!task.dueDate.isBefore(LocalDate.now()) && !current.dueDate.isBefore(LocalDate.now())) {
                "La tarea esta vencida"
            }
        }
        val reason = if (task.status == TaskStatus.CANCELLED) {
            require(!task.cancellationReason.isNullOrBlank()) { "Indique la razon de cancelacion" }
            task.cancellationReason.trim()
        } else null
        taskDao.updateTask(task.copy(
            cancellationReason = reason,
            statusChangedAt = if (changed) LocalDateTime.now() else current.statusChangedAt
        ).toEntity())
    }

    override suspend fun deleteTask(id: Long) {
        val current = getById(id) ?: error("Tarea no encontrada")
        check(!current.status.isTerminal()) { "La tarea no se puede eliminar" }
        taskDao.deleteTaskById(id)
    }
}