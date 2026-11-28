package cr.ac.utn.taskmanager.data.local

import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository
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
        taskDao.insertTask(task.toEntity())
    }

    override suspend fun updateTask(task: Task) {
        val current = getById(task.id) ?: error("Tarea no encontrada")
        check(!current.status.isTerminal()) { "La tarea no se puede modificar" }
        require(task.status in current.status.allowedStatuses()) { "Cambio de estado no permitido" }
        taskDao.updateTask(task.toEntity())
    }

    override suspend fun deleteTask(id: Long) {
        val current = getById(id) ?: error("Tarea no encontrada")
        check(!current.status.isTerminal()) { "La tarea no se puede eliminar" }
        taskDao.deleteTaskById(id)
    }
}