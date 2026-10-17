package cr.ac.utn.taskmanager.util

import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Util {
    companion object {
        fun openActivity(context: Context, classActivity: Class<*>) {
            val objIntent = Intent(context, classActivity)
            context.startActivity(objIntent)
        }

        fun formatDate(date: LocalDate): String {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            return date.format(formatter)
        }
    }
}