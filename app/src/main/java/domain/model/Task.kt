package domain.model

data class Task(
    val id: Long=0,
    val title: String,
    val description: String,
    val completed: Boolean=false
)
