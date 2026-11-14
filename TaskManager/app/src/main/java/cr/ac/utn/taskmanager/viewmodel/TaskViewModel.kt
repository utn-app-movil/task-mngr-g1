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

class TaskViewModel(application: Application):
    AndroidViewModel(application) {

    private val repository: ITaskRepository =
            (application as TaskManagerApp).taskRepository

    val tasks: StateFlow<List<Task>> = repository.getTasks()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList())


    suspend fun getTaskById(id: String): Task?{
        return repository.getById(id)
    }

    fun saveTask(task: Task, onComplete: () -> Unit){
        viewModelScope.launch {
            repository.addTask(task)
            onComplete?.invoke()
        }
    }

    fun updateTask(task: Task, onComplete: () -> Unit){
        viewModelScope.launch {
            repository.updateTask(task)
            onComplete?.invoke()
        }
    }

    fun removeTask(id: String, onComplete: () -> Unit){
        viewModelScope.launch {
            repository.deleteTask(id)
            onComplete?.invoke()
        }
    }

    class Factory (private val application: Application):
        ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TaskViewModel::class.java)){
                return TaskViewModel(application) as T
            }else{
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

    }
}