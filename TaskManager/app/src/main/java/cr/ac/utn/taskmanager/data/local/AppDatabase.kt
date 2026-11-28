package cr.ac.utn.taskmanager.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TaskEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {

            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL("""
                    CREATE TABLE tasks_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        dueDate TEXT NOT NULL,
                        creationDate TEXT NOT NULL,
                        status TEXT NOT NULL,
                        assignedTo TEXT NOT NULL,
                        priority TEXT NOT NULL,
                        notes TEXT NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO tasks_new (
                        title,
                        description,
                        dueDate,
                        creationDate,
                        status,
                        assignedTo,
                        priority,
                        notes
                    )
                    SELECT
                        title,
                        description,
                        dueDate,
                        creationDate,
                        status,
                        assignedTo,
                        priority,
                        notes
                    FROM tasks
                    ORDER BY rowid ASC
                """.trimIndent())

                db.execSQL("DROP TABLE tasks")

                db.execSQL(
                    "ALTER TABLE tasks_new RENAME TO tasks"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN cancellationReason TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN statusChangeDate TEXT")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "task_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }        }
    }
}