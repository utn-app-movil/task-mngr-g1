package data.local
import androidx.room.Entity
import androidx.room.PrimaryKey
import domain.model.Task

@Entity(tableName = "tasks")
data class TaskEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long =0,
    val title: String,
    val description: String,
    val completed: Boolean=false
){
    fun TaskEntity.toDomain(): Task =
        Task (id= id,
            title= title,
            description= description,
            completed= completed)

    fun Task.toEntity(): TaskEntity =
        TaskEntity (id= id,
            title= title,
            description= description,
            completed= completed)
}
