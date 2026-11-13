package cr.ac.utn.taskmanager.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cr.ac.utn.taskmanager.TaskManagerApp
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: ITaskRepository = (application as TaskManagerApp).repository

    val tasks: StateFlow<List<Task>> = repository.getTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    suspend fun getTaskById(id: String): Task? {
        return repository.getById(id)
    }

    fun saveTask(task: Task, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.addTask(task)
            onComplete?.invoke()
        }
    }

    fun updateTask(task: Task, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.updateTask(task)
            onComplete?.invoke()
        }
    }

    fun removeTask(id: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.deleteTask(id)
            onComplete?.invoke()
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
                return TaskViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
