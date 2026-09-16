package presentation.task

import domain.model.Task

data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean= false,
    val errorMessage: String? = null
)
