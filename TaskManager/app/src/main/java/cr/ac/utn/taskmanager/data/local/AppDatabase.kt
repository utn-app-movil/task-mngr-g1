package cr.ac.utn.taskmanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters

@Database(entities = [TaskEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        // La versión anterior usaba IDs de texto. Se conservan las tareas y se
        // asignan IDs numéricos consecutivos al actualizar la base de datos.
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE tasks_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL, description TEXT NOT NULL,
                        dueDate TEXT NOT NULL, creationDate TEXT NOT NULL,
                        status TEXT NOT NULL, assignedTo TEXT NOT NULL,
                        priority TEXT NOT NULL, notes TEXT NOT NULL,
                        parentTaskId INTEGER, cancellationReason TEXT NOT NULL,
                        statusChangeDate TEXT
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO tasks_new (
                        title, description, dueDate, creationDate, status,
                        assignedTo, priority, notes, parentTaskId,
                        cancellationReason, statusChangeDate
                    )
                    SELECT title, description, dueDate, creationDate, status,
                        assignedTo, priority, notes, NULL, '', NULL
                    FROM tasks ORDER BY CAST(id AS INTEGER), id
                """.trimIndent())
                db.execSQL("DROP TABLE tasks")
                db.execSQL("ALTER TABLE tasks_new RENAME TO tasks")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_database"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
