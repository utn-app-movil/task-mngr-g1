package cr.ac.utn.taskmanager.`data`.local

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import cr.ac.utn.taskmanager.domain.model.TaskPriority
import cr.ac.utn.taskmanager.domain.model.TaskStatus
import java.time.LocalDate
import java.time.LocalDateTime
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TaskDao_Impl(
  __db: RoomDatabase,
) : TaskDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTaskEntity: EntityInsertAdapter<TaskEntity>

  private val __converters: Converters = Converters()

  private val __updateAdapterOfTaskEntity: EntityDeleteOrUpdateAdapter<TaskEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfTaskEntity = object : EntityInsertAdapter<TaskEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `tasks` (`id`,`title`,`description`,`dueDate`,`creationDate`,`status`,`assignedTo`,`priority`,`notes`,`parentTaskId`,`cancellationReason`,`statusChangeDateTime`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TaskEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.description)
        val _tmp: String? = __converters.fromLocalDate(entity.dueDate)
        if (_tmp == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmp)
        }
        val _tmp_1: String? = __converters.fromLocalDate(entity.creationDate)
        if (_tmp_1 == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmp_1)
        }
        val _tmp_2: String? = __converters.fromTaskStatus(entity.status)
        if (_tmp_2 == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmp_2)
        }
        statement.bindText(7, entity.assignedTo)
        val _tmp_3: String? = __converters.fromTaskPriority(entity.priority)
        if (_tmp_3 == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmp_3)
        }
        statement.bindText(9, entity.notes)
        val _tmpParentTaskId: String? = entity.parentTaskId
        if (_tmpParentTaskId == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpParentTaskId)
        }
        val _tmpCancellationReason: String? = entity.cancellationReason
        if (_tmpCancellationReason == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpCancellationReason)
        }
        val _tmpStatusChangeDateTime: LocalDateTime? = entity.statusChangeDateTime
        val _tmp_4: String? = __converters.fromLocalDateTime(_tmpStatusChangeDateTime)
        if (_tmp_4 == null) {
          statement.bindNull(12)
        } else {
          statement.bindText(12, _tmp_4)
        }
      }
    }
    this.__updateAdapterOfTaskEntity = object : EntityDeleteOrUpdateAdapter<TaskEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `tasks` SET `id` = ?,`title` = ?,`description` = ?,`dueDate` = ?,`creationDate` = ?,`status` = ?,`assignedTo` = ?,`priority` = ?,`notes` = ?,`parentTaskId` = ?,`cancellationReason` = ?,`statusChangeDateTime` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: TaskEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.description)
        val _tmp: String? = __converters.fromLocalDate(entity.dueDate)
        if (_tmp == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmp)
        }
        val _tmp_1: String? = __converters.fromLocalDate(entity.creationDate)
        if (_tmp_1 == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmp_1)
        }
        val _tmp_2: String? = __converters.fromTaskStatus(entity.status)
        if (_tmp_2 == null) {
          statement.bindNull(6)
        } else {
          statement.bindText(6, _tmp_2)
        }
        statement.bindText(7, entity.assignedTo)
        val _tmp_3: String? = __converters.fromTaskPriority(entity.priority)
        if (_tmp_3 == null) {
          statement.bindNull(8)
        } else {
          statement.bindText(8, _tmp_3)
        }
        statement.bindText(9, entity.notes)
        val _tmpParentTaskId: String? = entity.parentTaskId
        if (_tmpParentTaskId == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpParentTaskId)
        }
        val _tmpCancellationReason: String? = entity.cancellationReason
        if (_tmpCancellationReason == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpCancellationReason)
        }
        val _tmpStatusChangeDateTime: LocalDateTime? = entity.statusChangeDateTime
        val _tmp_4: String? = __converters.fromLocalDateTime(_tmpStatusChangeDateTime)
        if (_tmp_4 == null) {
          statement.bindNull(12)
        } else {
          statement.bindText(12, _tmp_4)
        }
        statement.bindText(13, entity.id)
      }
    }
  }

  public override suspend fun insertTask(task: TaskEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTaskEntity.insert(_connection, task)
  }

  public override suspend fun updateTask(task: TaskEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfTaskEntity.handle(_connection, task)
  }

  public override fun getAllTasks(): Flow<List<TaskEntity>> {
    val _sql: String = "SELECT * FROM tasks"
    return createFlow(__db, false, arrayOf("tasks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfDueDate: Int = getColumnIndexOrThrow(_stmt, "dueDate")
        val _columnIndexOfCreationDate: Int = getColumnIndexOrThrow(_stmt, "creationDate")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfAssignedTo: Int = getColumnIndexOrThrow(_stmt, "assignedTo")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfParentTaskId: Int = getColumnIndexOrThrow(_stmt, "parentTaskId")
        val _columnIndexOfCancellationReason: Int = getColumnIndexOrThrow(_stmt, "cancellationReason")
        val _columnIndexOfStatusChangeDateTime: Int = getColumnIndexOrThrow(_stmt, "statusChangeDateTime")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpDueDate: LocalDate
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfDueDate)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfDueDate)
          }
          val _tmp_1: LocalDate? = __converters.toLocalDate(_tmp)
          if (_tmp_1 == null) {
            error("Expected NON-NULL 'java.time.LocalDate', but it was NULL.")
          } else {
            _tmpDueDate = _tmp_1
          }
          val _tmpCreationDate: LocalDate
          val _tmp_2: String?
          if (_stmt.isNull(_columnIndexOfCreationDate)) {
            _tmp_2 = null
          } else {
            _tmp_2 = _stmt.getText(_columnIndexOfCreationDate)
          }
          val _tmp_3: LocalDate? = __converters.toLocalDate(_tmp_2)
          if (_tmp_3 == null) {
            error("Expected NON-NULL 'java.time.LocalDate', but it was NULL.")
          } else {
            _tmpCreationDate = _tmp_3
          }
          val _tmpStatus: TaskStatus
          val _tmp_4: String?
          if (_stmt.isNull(_columnIndexOfStatus)) {
            _tmp_4 = null
          } else {
            _tmp_4 = _stmt.getText(_columnIndexOfStatus)
          }
          val _tmp_5: TaskStatus? = __converters.toTaskStatus(_tmp_4)
          if (_tmp_5 == null) {
            error("Expected NON-NULL 'cr.ac.utn.taskmanager.domain.model.TaskStatus', but it was NULL.")
          } else {
            _tmpStatus = _tmp_5
          }
          val _tmpAssignedTo: String
          _tmpAssignedTo = _stmt.getText(_columnIndexOfAssignedTo)
          val _tmpPriority: TaskPriority
          val _tmp_6: String?
          if (_stmt.isNull(_columnIndexOfPriority)) {
            _tmp_6 = null
          } else {
            _tmp_6 = _stmt.getText(_columnIndexOfPriority)
          }
          val _tmp_7: TaskPriority? = __converters.toTaskPriority(_tmp_6)
          if (_tmp_7 == null) {
            error("Expected NON-NULL 'cr.ac.utn.taskmanager.domain.model.TaskPriority', but it was NULL.")
          } else {
            _tmpPriority = _tmp_7
          }
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          val _tmpParentTaskId: String?
          if (_stmt.isNull(_columnIndexOfParentTaskId)) {
            _tmpParentTaskId = null
          } else {
            _tmpParentTaskId = _stmt.getText(_columnIndexOfParentTaskId)
          }
          val _tmpCancellationReason: String?
          if (_stmt.isNull(_columnIndexOfCancellationReason)) {
            _tmpCancellationReason = null
          } else {
            _tmpCancellationReason = _stmt.getText(_columnIndexOfCancellationReason)
          }
          val _tmpStatusChangeDateTime: LocalDateTime?
          val _tmp_8: String?
          if (_stmt.isNull(_columnIndexOfStatusChangeDateTime)) {
            _tmp_8 = null
          } else {
            _tmp_8 = _stmt.getText(_columnIndexOfStatusChangeDateTime)
          }
          _tmpStatusChangeDateTime = __converters.toLocalDateTime(_tmp_8)
          _item = TaskEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpDueDate,_tmpCreationDate,_tmpStatus,_tmpAssignedTo,_tmpPriority,_tmpNotes,_tmpParentTaskId,_tmpCancellationReason,_tmpStatusChangeDateTime)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getTaskById(idTask: String): TaskEntity? {
    val _sql: String = "SELECT * FROM tasks WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, idTask)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _columnIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _columnIndexOfDueDate: Int = getColumnIndexOrThrow(_stmt, "dueDate")
        val _columnIndexOfCreationDate: Int = getColumnIndexOrThrow(_stmt, "creationDate")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfAssignedTo: Int = getColumnIndexOrThrow(_stmt, "assignedTo")
        val _columnIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfParentTaskId: Int = getColumnIndexOrThrow(_stmt, "parentTaskId")
        val _columnIndexOfCancellationReason: Int = getColumnIndexOrThrow(_stmt, "cancellationReason")
        val _columnIndexOfStatusChangeDateTime: Int = getColumnIndexOrThrow(_stmt, "statusChangeDateTime")
        val _result: TaskEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_columnIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_columnIndexOfDescription)
          val _tmpDueDate: LocalDate
          val _tmp: String?
          if (_stmt.isNull(_columnIndexOfDueDate)) {
            _tmp = null
          } else {
            _tmp = _stmt.getText(_columnIndexOfDueDate)
          }
          val _tmp_1: LocalDate? = __converters.toLocalDate(_tmp)
          if (_tmp_1 == null) {
            error("Expected NON-NULL 'java.time.LocalDate', but it was NULL.")
          } else {
            _tmpDueDate = _tmp_1
          }
          val _tmpCreationDate: LocalDate
          val _tmp_2: String?
          if (_stmt.isNull(_columnIndexOfCreationDate)) {
            _tmp_2 = null
          } else {
            _tmp_2 = _stmt.getText(_columnIndexOfCreationDate)
          }
          val _tmp_3: LocalDate? = __converters.toLocalDate(_tmp_2)
          if (_tmp_3 == null) {
            error("Expected NON-NULL 'java.time.LocalDate', but it was NULL.")
          } else {
            _tmpCreationDate = _tmp_3
          }
          val _tmpStatus: TaskStatus
          val _tmp_4: String?
          if (_stmt.isNull(_columnIndexOfStatus)) {
            _tmp_4 = null
          } else {
            _tmp_4 = _stmt.getText(_columnIndexOfStatus)
          }
          val _tmp_5: TaskStatus? = __converters.toTaskStatus(_tmp_4)
          if (_tmp_5 == null) {
            error("Expected NON-NULL 'cr.ac.utn.taskmanager.domain.model.TaskStatus', but it was NULL.")
          } else {
            _tmpStatus = _tmp_5
          }
          val _tmpAssignedTo: String
          _tmpAssignedTo = _stmt.getText(_columnIndexOfAssignedTo)
          val _tmpPriority: TaskPriority
          val _tmp_6: String?
          if (_stmt.isNull(_columnIndexOfPriority)) {
            _tmp_6 = null
          } else {
            _tmp_6 = _stmt.getText(_columnIndexOfPriority)
          }
          val _tmp_7: TaskPriority? = __converters.toTaskPriority(_tmp_6)
          if (_tmp_7 == null) {
            error("Expected NON-NULL 'cr.ac.utn.taskmanager.domain.model.TaskPriority', but it was NULL.")
          } else {
            _tmpPriority = _tmp_7
          }
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          val _tmpParentTaskId: String?
          if (_stmt.isNull(_columnIndexOfParentTaskId)) {
            _tmpParentTaskId = null
          } else {
            _tmpParentTaskId = _stmt.getText(_columnIndexOfParentTaskId)
          }
          val _tmpCancellationReason: String?
          if (_stmt.isNull(_columnIndexOfCancellationReason)) {
            _tmpCancellationReason = null
          } else {
            _tmpCancellationReason = _stmt.getText(_columnIndexOfCancellationReason)
          }
          val _tmpStatusChangeDateTime: LocalDateTime?
          val _tmp_8: String?
          if (_stmt.isNull(_columnIndexOfStatusChangeDateTime)) {
            _tmp_8 = null
          } else {
            _tmp_8 = _stmt.getText(_columnIndexOfStatusChangeDateTime)
          }
          _tmpStatusChangeDateTime = __converters.toLocalDateTime(_tmp_8)
          _result = TaskEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpDueDate,_tmpCreationDate,_tmpStatus,_tmpAssignedTo,_tmpPriority,_tmpNotes,_tmpParentTaskId,_tmpCancellationReason,_tmpStatusChangeDateTime)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getMaxId(): Int? {
    val _sql: String = "SELECT MAX(CAST(id AS INTEGER)) FROM tasks"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int?
        if (_stmt.step()) {
          val _tmp: Int?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(0).toInt()
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteTaskById(idTask: String) {
    val _sql: String = "DELETE FROM tasks WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, idTask)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
