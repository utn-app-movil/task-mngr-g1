package cr.ac.utn.taskmanager.database.AppDatabase

import androidx.room.Database
import androidx.room.RoomDatabase
import cr.ac.utn.taskmanager.database.dao.TaskDao
import cr.ac.utn.taskmanager.database.entity.TaskEntity

@Database(
    entities = [TaskEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase: RoomDatabase() {
    abstract fun taskDao(): TaskDao
}