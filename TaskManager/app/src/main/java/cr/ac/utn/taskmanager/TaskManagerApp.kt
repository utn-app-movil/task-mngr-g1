package cr.ac.utn.taskmanager

import android.app.Application
import cr.ac.utn.taskmanager.data.local.AppDatabase
import cr.ac.utn.taskmanager.data.local.TaskRoomRepository
import cr.ac.utn.taskmanager.data.remote.RetrofitClient
import cr.ac.utn.taskmanager.data.repository.ProductRepositoryImpl
import cr.ac.utn.taskmanager.domain.repository.IProductRepository
import cr.ac.utn.taskmanager.domain.repository.ITaskRepository

class TaskManagerApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getDatabase(this)
    }

    val taskRepository: ITaskRepository by lazy {
        TaskRoomRepository(database.taskDao())
    }

    val productRepository: IProductRepository by lazy {
        ProductRepositoryImpl(RetrofitClient.apiService)
    }
}
