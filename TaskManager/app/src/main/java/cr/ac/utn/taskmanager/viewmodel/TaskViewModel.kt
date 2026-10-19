package cr.ac.utn.taskmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(
    private val repository: ITaskRepository
): ViewModel() {
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    val tasks = repository
        .getTasks()
        .catch { ex ->
            _error.value = ex.message ?: "Error loading tasks"
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedTask =
        MutableStateFlow<Task?>(null)

    val selectedTask =
        _selectedTask.asStateFlow()

    /*fun getTasks(): List<Task>{
        return repository.getTasks()
    }*/

    fun getTaskById(id: String){
        viewModelScope.launch {
            try {
                _selectedTask.value = repository.getById(id)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun saveTask(task: Task){
        viewModelScope.launch {
            try {
                repository.addTask(task)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun updateTask(task: Task){
        viewModelScope.launch {
            try {
                repository.updateTask(task)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun removeTask(id: String){
        viewModelScope.launch {
            try {
                repository.deleteTask(id)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}