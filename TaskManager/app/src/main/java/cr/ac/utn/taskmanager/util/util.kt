package cr.ac.utn.taskmanager.util

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import cr.ac.utn.taskmanager.R
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
            val intent = Intent(context, objClass).apply { putExtra(extraName, value) }
            context.startActivity(intent)
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

        fun showDialogCondition(context: Context, questionText: String, callback: () -> Unit) {
            val dialogBuilder = AlertDialog.Builder(context)
            dialogBuilder.setMessage(questionText)
                .setCancelable(false)
                .setPositiveButton(context.getString(R.string.TextYes)) { _, _ -> callback() }
                .setNegativeButton(context.getString(R.string.TextNo)) { dialog, _ -> dialog.cancel() }

            val alert = dialogBuilder.create()
            alert.setTitle(context.getString(R.string.TextTitleDialogQuestion))
            alert.show()
        }

        fun showProgressDialog(context: Context, message: String): AlertDialog {
            val progressBar = ProgressBar(context).apply {
                isIndeterminate = true
                setPadding(40, 40, 40, 40)
            }
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(50, 40, 50, 40)
                gravity = Gravity.CENTER_VERTICAL
                addView(
                    progressBar,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )
                val tvMessage = TextView(context).apply {
                    text = message
                    textSize = 16f
                    setPadding(30, 0, 0, 0)
                }
                addView(tvMessage)
            }

            val builder = AlertDialog.Builder(context)
                .setView(container)
                .setCancelable(false)

            val dialog = builder.create()
            dialog.show()
            return dialog
        }
    }
}
