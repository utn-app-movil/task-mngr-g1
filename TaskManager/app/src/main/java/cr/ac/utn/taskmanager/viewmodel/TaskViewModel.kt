package cr.ac.utn.taskmanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cr.ac.utn.taskmanager.data.local.AppDatabase
import cr.ac.utn.taskmanager.data.local.TaskRoomRepository
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ITaskRepository

    val tasks: StateFlow<List<Task>>
    val eligibleParentTasks: StateFlow<List<Task>>

    init {
        val dao = AppDatabase.getDatabase(application).taskDao()
        repository = TaskRoomRepository(dao)

        tasks = repository.getTasks().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        eligibleParentTasks = repository.getEligibleParentTasks().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    suspend fun getTaskById(id: String): Task? {
        return repository.getById(id)
    }

    suspend fun getNextTaskId(): String {
        return repository.getNextTaskId()
    }

    fun saveTask(task: Task, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.addTask(task)
            onComplete()
        }
    }

    fun updateTask(task: Task, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateTask(task)
            onComplete()
        }
    }

    fun removeTask(id: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteTask(id)
            onComplete()
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TaskViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}