package cr.ac.utn.taskmanager

import android.app.Application
import cr.ac.utn.taskmanager.data.local.AppDatabase
import cr.ac.utn.taskmanager.data.local.TaskRoomRepository
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository
import cr.ac.utn.taskmanager.util.util

class TaskManagerApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    val people: List<String> by lazy {
        util.loadPeopleFromAssets(this).distinct()
    }

    val taskRepository: ITaskRepository by lazy {
        TaskRoomRepository(database, people.toSet())
    }

}
