package presentation.task

import androidx.compose.runtime.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun TaskScreen (viewModel: TaskViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn{
        items(
            items= state.tasks,
            key = { task -> task.id }
        ){ task ->
            TaskItem (
                task = task,
                onDelete = { viewModel.deleteTask(task) }
            )

        }
    }

}