package cr.ac.utn.taskmanager.data.mapper

import cr.ac.utn.taskmanager.database.entity.TaskEntity
import cr.ac.utn.taskmanager.domain.model.Task
import cr.ac.utn.taskmanager.domain.model.TaskPriority
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import java.time.LocalDate

fun TaskEntity.toDomain(): Task {
    return Task(
        id = id,
        title = title,
        description = description,
        dueDate = LocalDate.parse(dueDate),
        creationDate = LocalDate.parse(creationDate),
        status = TaskStatus.valueOf(status),
        priority = TaskPriority.valueOf(priority),
        assignedTo = assignTo,
        notes = observation
    )
}
fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate.toString(),
        creationDate = creationDate.toString(),
        status = status.name,
        assignTo = assignedTo,
        priority = priority.name,
        observation = notes
    )
}
