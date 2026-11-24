package cr.ac.utn.taskmanager.data.remote

data class ApiResponse<T>(
    val data: T?,
    val responseCode: String?,
    val message: String?
)
