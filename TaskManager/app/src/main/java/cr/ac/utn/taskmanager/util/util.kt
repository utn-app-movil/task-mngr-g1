package cr.ac.utn.taskmanager.util

import android.content.Context
import android.content.Intent
import cr.ac.utn.taskmanager.TaskManagerApp
import cr.ac.utn.taskmanager.viewmodel.TaskViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.app.ProgressDialog
import androidx.appcompat.app.AlertDialog
import cr.ac.utn.taskmanager.R

const val EXTRA_MESSAGE_ID = "cr.ac.utn.task.Id"
class util {
    companion object{
        fun openActivity(context: Context
                         , objClass: Class<*>, extraName: String="", value: String?=null){
            val intent= Intent(context
                , objClass).apply { putExtra(extraName, value)}
            context.startActivity(intent)
        }

        fun OpenActivity(context: Context, classActivity: Class<*>){
            val objIntent = Intent(context, classActivity)
            context.startActivity(objIntent)
        }

        fun parseStringToDateModern(dateString: String, pattern: String): LocalDate? {
            return try {
                val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
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

        fun buildPersonViewModel(context: Context): TaskViewModel {
            val app = context.applicationContext as TaskManagerApp
            return TaskViewModel(app)
        }

        fun showDialogCondition(
            context: Context,
            message: String,
            onConfirm: () -> Unit
        ) {
            AlertDialog.Builder(context)
                .setTitle(R.string.TextTitleDialogQuestion)
                .setMessage(message)
                .setPositiveButton(R.string.TextYes) { _, _ ->
                    onConfirm()
                }
                .setNegativeButton(R.string.TextNo, null)
                .show()
        }

        @Suppress("DEPRECATION")
        fun showProgressDialog(
            context: Context,
            message: String
        ): ProgressDialog {
            return ProgressDialog(context).apply {
                setMessage(message)
                setCancelable(false)
                show()
            }
        }
    }
}
