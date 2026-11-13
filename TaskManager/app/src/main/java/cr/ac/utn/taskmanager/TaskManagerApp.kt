package cr.ac.utn.taskmanager

import android.app.Application
import cr.ac.utn.taskmanager.data.local.AppDatabase
import cr.ac.utn.taskmanager.data.local.TaskRoomRepository
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository

class TaskManagerApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    /*val repository: ITaskRepository by lazy {
        TaskRoomRepository(database.taskDao())
    }*/
}
