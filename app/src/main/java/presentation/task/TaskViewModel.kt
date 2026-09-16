package presentation.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.model.Task
import domain.repository.ITaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: ITaskRepository): ViewModel() {
    private val _uiState = MutableStateFlow(TaskUiState())

    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    init{
        observeTasks()
    }

    private fun observeTasks(){
        viewModelScope.launch {
            repository.observeTasks()
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            errorMessage = exception.message
                        )
                    }
                }
                .collect { tasks ->
                    _uiState.update {
                        it.copy(tasks = tasks, isLoading=false)
                    }
                }
        }
    }

    fun addTask(title: String, description: String){
        if (title.isBlank()){
            _uiState.update {
                it.copy(errorMessage = "El título es obligatorio")
            }
            return
        }
        viewModelScope.launch {
            repository.addTask(
                Task(title= title.trim(), description = description.trim())
            )
        }
    }

    fun deleteTask(task: Task){
        viewModelScope.launch { repository.deleteTask(task) }
    }

    fun updateTask(task: Task){
        viewModelScope.launch { repository.updateTask(task) }
    }
}