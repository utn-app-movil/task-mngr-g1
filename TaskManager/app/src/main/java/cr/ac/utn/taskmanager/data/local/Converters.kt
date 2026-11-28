package cr.ac.utn.taskmanager.data.local

import androidx.room.TypeConverter
import cr.ac.utn.taskmanager.domain.model.TaskPriority
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import java.time.LocalDate
import java.time.LocalDateTime

class Converters {
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it) }
    }

    @TypeConverter
    fun fromTaskStatus(value: TaskStatus?): String? {
        return value?.name
    }

    @TypeConverter
    fun toTaskStatus(value: String?): TaskStatus? {
        return value?.let { TaskStatus.valueOf(it) }
    }

    @TypeConverter
    fun fromTaskPriority(value: TaskPriority?): String? {
        return value?.name
    }

    @TypeConverter
    fun toTaskPriority(value: String?): TaskPriority? {
        return value?.let { TaskPriority.valueOf(it) }
    }
}