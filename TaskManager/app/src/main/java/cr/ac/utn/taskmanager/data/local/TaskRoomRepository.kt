package cr.ac.utn.taskmanager.data.local

import androidx.room.withTransaction
import cr.ac.utn.taskmanager.domain.model.StatusChange
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

class TaskRoomRepository(
    private val database: AppDatabase,
    private val people: Set<String>
) : ITaskRepository {
    private val taskDao = database.taskDao()

    override fun getTasks(): Flow<List<Task>> =
        taskDao.getAllTasks().map { tasks -> tasks.map { it.toDomain() } }

    override fun getEligibleParentTasks(): Flow<List<Task>> {
        return getTasks().map { tasks ->
            tasks.filter { task -> task.status.isActive }
        }
    }

    override suspend fun getById(id: String): Task? =
        taskDao.getTaskById(id.trim())?.toDomain()

    override suspend fun getStatusChanges(id: String): List<StatusChange> =
        taskDao.getStatusChanges(id).map {
            StatusChange(it.previousStatus, it.newStatus, it.changedAt)
        }

    override suspend fun addTask(task: Task): String = database.withTransaction {
        require(task.assignedTo in people) { "Seleccione una persona de la lista." }
        task.parentId?.let { parentId ->
            require(taskDao.getTaskById(parentId)?.status?.isActive == true) {
                "La tarea padre debe estar pendiente o en progreso."
            }
        }

        val lastId = taskDao.readLastId() ?: 0L
        check(lastId < Long.MAX_VALUE) { "Se agotaron los identificadores de tarea." }
        val nextId = lastId + 1
        taskDao.insertTask(task.copy(
            id = nextId.toString(),
            status = TaskStatus.PENDING,
            cancellationReason = null
        ).toEntity())
        taskDao.saveLastId(TaskIdSequence(sequenceKey = 1, lastId = nextId))
        nextId.toString()
    }

    override suspend fun updateTask(task: Task) = database.withTransaction {
        val previous = requireNotNull(taskDao.getTaskById(task.id)) { "La tarea ya no existe." }
        require(previous.status.isActive) { "La tarea está cerrada y no se puede modificar." }
        require(task.assignedTo in people) { "Seleccione una persona de la lista." }
        require(task.status in previous.status.availableStatuses()) {
            "El cambio de estado no está permitido."
        }

        val now = LocalDateTime.now()
        val changed = task.status != previous.status
        if (changed && task.status == TaskStatus.IN_PROGRESS) {
            val today = now.toLocalDate()
            require(!task.dueDate.isBefore(today)) {
                "Una tarea vencida no puede pasar a IN_PROGRESS."
            }
        }

        val isCancelled = task.status == TaskStatus.CANCELLED
        val reason = task.cancellationReason.orEmpty().trim()
        if (isCancelled) {
            require(reason.isNotEmpty()) { "Indique el motivo de la cancelación." }
        }

        taskDao.updateTask(task.copy(
            creationDate = previous.creationDate,
            parentId = previous.parentId,
            cancellationReason = if (isCancelled) reason else null
        ).toEntity())
        if (changed) {
            taskDao.insertStatusChange(TaskStatusChange(
                taskId = task.id,
                previousStatus = previous.status,
                newStatus = task.status,
                changedAt = now
            ))
        }
    }

    override suspend fun deleteTask(id: String) = database.withTransaction {
        val task = taskDao.getTaskById(id.trim()) ?: return@withTransaction
        require(task.status.isActive) { "No se puede eliminar una tarea cerrada." }
        require(!taskDao.hasSubtasks(task.id)) { "No se puede eliminar una tarea que tiene subtareas." }
        taskDao.deleteTaskById(task.id)
    }
}
