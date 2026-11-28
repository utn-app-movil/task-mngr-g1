package cr.ac.utn.taskmanager.data.local

import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime

class TaskRoomRepository(
    private val taskDao: TaskDao
) : ITaskRepository {

    private val mutex = Mutex()

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

    override suspend fun getById(id: String): Task? {
        return taskDao.getTaskById(id.trim())?.toDomain()
    }

    override suspend fun addTask(task: Task) {
        mutex.withLock {
            val nextId = taskDao.getMaxId() + 1

            val newTask = task.copy(
                id = nextId.toString(),
                status = TaskStatus.PENDING,
                creationDate = LocalDate.now(),
                cancellationReason = null,
                statusChangeDate = null
            )

            taskDao.insertTask(newTask.toEntity())
        }
    }

    override suspend fun updateTask(task: Task) {
        mutex.withLock {
            val currentTask = getById(task.id)
                ?: return@withLock

            val updatedTask = task.copy(
                creationDate = currentTask.creationDate,
                statusChangeDate =
                    if (currentTask.status != task.status) {
                        LocalDateTime.now()
                    } else {
                        currentTask.statusChangeDate
                    }
            )

            taskDao.updateTask(updatedTask.toEntity())
        }
    }

    override suspend fun deleteTask(id: String) {
        taskDao.deleteTaskById(id.trim())
    }
}