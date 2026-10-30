package cr.ac.utn.taskmanager.util

import android.content.Context
import android.content.Intent
import cr.ac.utn.taskmanager.data.memory.TaskMemoryManager
import cr.ac.utn.taskmanager.viewmodel.TaskViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

const val EXTRA_MESSAGE_ID = "cr.ac.utn.task.Id"

class util {
    companion object {
        fun openActivity(
            context: Context,
            objClass: Class<*>,
            extraName: String = "",
            value: String? = null
        ) {
            val intent = Intent(context, objClass).apply {
                if (extraName.isNotBlank()) putExtra(extraName, value)
            }
            context.startActivity(intent)
        }

        fun parseStringToDateModern(dateString: String, pattern: String): LocalDate? {
            return try {
                // BUG INTENCIONAL #3: Patrón de fecha invertido (MM/dd/yyyy en lugar de usar la variable 'pattern').
                // Provoca DateTimeParseException cuando el día seleccionado es mayor a 12 (ej. 15/03/2025).
                val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.getDefault())
                LocalDate.parse(dateString, formatter)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun parseStringToDateTimeModern(dateTimeString: String, pattern: String): LocalDateTime? {
            return try {
                val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
                LocalDateTime.parse(dateTimeString, formatter)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun buildPersonViewModel(): TaskViewModel {
            val repository = TaskMemoryManager()
            return TaskViewModel(repository)
        }
    }
}
