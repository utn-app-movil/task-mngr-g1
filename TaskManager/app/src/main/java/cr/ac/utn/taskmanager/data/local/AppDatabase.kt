package cr.ac.utn.taskmanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TaskEntity::class, TaskIdSequence::class, TaskStatusChange::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val migration1To3 = taskMigration(1)
        private val migration2To3 = taskMigration(2)

        private fun taskMigration(oldVersion: Int) = object : Migration(oldVersion, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = mutableSetOf<String>()
                db.query("PRAGMA table_info(tasks)").use { cursor ->
                    val name = cursor.getColumnIndexOrThrow("name")
                    while (cursor.moveToNext()) {
                        columns.add(cursor.getString(name))
                    }
                }

                if ("parentId" !in columns) {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN parentId TEXT REFERENCES tasks(id) ON DELETE RESTRICT")
                }
                if ("cancellationReason" !in columns) {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN cancellationReason TEXT")
                }

                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_parentId ON tasks(parentId)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_id_sequence (
                        sequenceKey INTEGER NOT NULL PRIMARY KEY,
                        lastId INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO task_id_sequence (sequenceKey, lastId)
                    SELECT 1, COALESCE(MAX(CAST(id AS INTEGER)), 0)
                    FROM tasks WHERE id <> '' AND id NOT GLOB '*[^0-9]*'
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_status_changes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        taskId TEXT NOT NULL,
                        previousStatus TEXT NOT NULL,
                        newStatus TEXT NOT NULL,
                        changedAt TEXT NOT NULL,
                        FOREIGN KEY(taskId) REFERENCES tasks(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_status_changes_taskId ON task_status_changes(taskId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_database"
                ).addMigrations(migration1To3, migration2To3).build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
