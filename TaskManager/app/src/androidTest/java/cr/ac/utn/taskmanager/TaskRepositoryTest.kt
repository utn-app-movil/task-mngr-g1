package cr.ac.utn.taskmanager

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cr.ac.utn.taskmanager.data.local.AppDatabase
import cr.ac.utn.taskmanager.data.local.TaskRoomRepository
import cr.ac.utn.taskmanager.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TaskRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: TaskRoomRepository
    private lateinit var context: Context
    private val databaseName = "project1_repository_test"

    private fun openDatabase() {
        database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(AppDatabase.MIGRATION_1_2).build()
        repository = TaskRoomRepository(database)
    }

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(databaseName)
        openDatabase()
    }

    @After fun cleanup() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    private fun task(parentId: String? = null): Task {
        val today = LocalDate.now()
        return Task("", "Proyecto", "Preparar entrega", today, today,
            TaskStatus.PENDING, "Juan Pérez", TaskPriority.MEDIUM, "", parentId)
    }

    @Test fun idsIncreaseAfterDeletingTheHighestIdAndReopening() = runBlocking {
        repeat(3) { assertNull(repository.addTask(task())) }
        assertEquals(listOf("1", "2", "3"), repository.getTasks().first().map { it.id })
        assertNull(repository.deleteTask("3"))
        database.close()
        openDatabase()
        assertNull(repository.addTask(task()))
        assertNotNull(repository.getById("4"))
        assertNull(repository.getById("3"))
    }

    @Test fun subtasksRequireAnActiveParentAndBlockParentDeletion() = runBlocking {
        assertNull(repository.addTask(task()))
        assertNull(repository.addTask(task("1")))
        assertNull(repository.addTask(task("1")))
        assertEquals(2, database.taskDao().countSubtasks(1))
        assertEquals(TaskError.HAS_SUBTASKS, repository.deleteTask("1"))
        val parent = repository.getById("1")!!
        assertNull(repository.updateTask(parent.copy(
            status = TaskStatus.CANCELLED, cancellationReason = "Se suspendió")))
        assertEquals(TaskError.INVALID_PARENT, repository.addTask(task("1")))
        assertEquals(TaskError.INVALID_PARENT, repository.addTask(task("999")))
        assertEquals(3, repository.getTasks().first().size)
    }

    @Test fun stateAndCancellationDataSurviveReopeningAndClosedTasksStayLocked() = runBlocking {
        assertNull(repository.addTask(task()))
        val pending = repository.getById("1")!!
        assertEquals(TaskStatus.PENDING, pending.status)
        assertNotNull(pending.statusChangeDate)
        assertNull(repository.updateTask(pending.copy(status = TaskStatus.IN_PROGRESS)))
        val started = repository.getById("1")!!
        assertTrue(!started.statusChangeDate!!.isBefore(pending.statusChangeDate))
        assertEquals(TaskError.INVALID_STATUS, repository.updateTask(started.copy(status = TaskStatus.PENDING)))
        assertNull(repository.updateTask(started.copy(notes = "Una observación")))
        assertEquals(started.statusChangeDate, repository.getById("1")!!.statusChangeDate)
        assertEquals(TaskError.CANCELLATION_REASON,
            repository.updateTask(started.copy(status = TaskStatus.CANCELLED)))
        assertNull(repository.updateTask(started.copy(
            status = TaskStatus.CANCELLED, cancellationReason = "Sin presupuesto")))
        database.close()
        openDatabase()
        val cancelled = repository.getById("1")!!
        assertEquals("Sin presupuesto", cancelled.cancellationReason)
        assertNotNull(cancelled.statusChangeDate)
        assertEquals(TaskError.CLOSED_TASK, repository.updateTask(cancelled.copy(title = "Otro")))
        assertEquals(TaskError.CLOSED_TASK, repository.deleteTask("1"))
    }

    @Test fun completedTasksStayLockedAndExpiredTasksCannotStart() = runBlocking {
        assertNull(repository.addTask(task().copy(dueDate = LocalDate.now().minusDays(1))))
        val pending = repository.getById("1")!!
        assertEquals(TaskError.PAST_DUE_DATE,
            repository.updateTask(pending.copy(status = TaskStatus.IN_PROGRESS)))
        assertNull(repository.updateTask(pending.copy(
            status = TaskStatus.IN_PROGRESS, dueDate = LocalDate.now())))
        val started = repository.getById("1")!!
        assertNull(repository.updateTask(started.copy(status = TaskStatus.COMPLETED)))
        val completed = repository.getById("1")!!
        assertEquals(TaskError.CLOSED_TASK, repository.updateTask(completed.copy(notes = "Otro")))
        assertEquals(TaskError.CLOSED_TASK, repository.deleteTask("1"))
    }

    @Test fun migrationKeepsOldTasksAndEnablesAutomaticIds() = runBlocking {
        database.close()
        context.deleteDatabase(databaseName)
        val old = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        old.execSQL("""CREATE TABLE tasks (
            id TEXT NOT NULL PRIMARY KEY, title TEXT NOT NULL, description TEXT NOT NULL,
            dueDate TEXT NOT NULL, creationDate TEXT NOT NULL, status TEXT NOT NULL,
            assignedTo TEXT NOT NULL, priority TEXT NOT NULL, notes TEXT NOT NULL)""")
        old.execSQL("""INSERT INTO tasks VALUES ('old-id', 'Anterior', 'Descripción',
            '2026-11-27', '2026-11-27', 'PENDING', 'Juan Pérez', 'LOW', '')""")
        old.version = 1
        old.close()
        openDatabase()
        val migrated = repository.getTasks().first().single()
        assertEquals("Anterior", migrated.title)
        assertEquals("1", migrated.id)
        assertNull(repository.addTask(task()))
        assertNotNull(repository.getById("2"))
    }
}
