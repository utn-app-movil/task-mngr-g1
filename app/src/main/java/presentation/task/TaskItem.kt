package presentation.task

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dps
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import domain.model.Task

@Composable
fun TaskItem (task: Task, onDelete: () -> Unit) {
    Row(
        modifier= Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ){
        Column(
            modifier = Modifier.weight(1f)
        ){
            Text(text= task.title)
            Text(text = task.description)
        }

        IconButton(onClick = onDelete){
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Eliminar tarea"
            )
        }
    }
}