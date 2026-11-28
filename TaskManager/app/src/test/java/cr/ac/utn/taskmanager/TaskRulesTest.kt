package cr.ac.utn.taskmanager

import cr.ac.utn.taskmanager.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class TaskRulesTest {
    private val today = LocalDate.of(2026, 11, 27)
    private fun task(status: TaskStatus = TaskStatus.PENDING): Task {
        return Task("1", "Proyecto", "Preparar entrega", today, today,
            status, "Juan Pérez", TaskPriority.MEDIUM, "")
    }

    @Test fun everyTransitionFollowsTheRequirements() {
        for (oldStatus in TaskStatus.entries) {
            for (newStatus in TaskStatus.entries) {
                val original = task(oldStatus)
                val changed = original.copy(status = newStatus, cancellationReason = "Cambio de planes")
                val allowed = when (oldStatus) {
                    TaskStatus.PENDING -> newStatus != TaskStatus.COMPLETED
                    TaskStatus.IN_PROGRESS -> newStatus != TaskStatus.PENDING
                    else -> false
                }
                assertEquals("$oldStatus -> $newStatus", allowed,
                    TaskRules.validate(changed, original, today) == null)
            }
        }
    }

    @Test fun onlyPendingCanBeInserted() {
        for (status in TaskStatus.entries) {
            assertEquals(status == TaskStatus.PENDING,
                TaskRules.validate(task(status), null, today) == null)
        }
    }

    @Test fun cancellationRequiresANonBlankReason() {
        val original = task()
        assertEquals(TaskError.CANCELLATION_REASON, TaskRules.validate(
            original.copy(status = TaskStatus.CANCELLED, cancellationReason = "  "),
            original, today))
        assertNull(TaskRules.validate(original.copy(
            status = TaskStatus.CANCELLED, cancellationReason = "Ya no se requiere"),
            original, today))
    }

    @Test fun expiredTaskCannotStartButTodayAndTomorrowCan() {
        val original = task()
        for (offset in -1L..1L) {
            val changed = original.copy(status = TaskStatus.IN_PROGRESS, dueDate = today.plusDays(offset))
            assertEquals(if (offset < 0) TaskError.PAST_DUE_DATE else null,
                TaskRules.validate(changed, original, today))
        }
    }

    @Test fun overdueInProgressTaskCanStillFinishOrCancel() {
        val original = task(TaskStatus.IN_PROGRESS).copy(dueDate = today.minusDays(1))
        assertNull(TaskRules.validate(original.copy(status = TaskStatus.COMPLETED), original, today))
        assertNull(TaskRules.validate(original.copy(status = TaskStatus.CANCELLED,
            cancellationReason = "Se suspendió"), original, today))
    }

    @Test fun closedTasksCannotChangeTheirFields() {
        for (status in listOf(TaskStatus.COMPLETED, TaskStatus.CANCELLED)) {
            val original = task(status)
            assertEquals(TaskError.CLOSED_TASK,
                TaskRules.validate(original.copy(title = "Otro título"), original, today))
        }
    }

    @Test fun titleDescriptionAndPersonAreRequiredButNotesAreOptional() {
        val original = task()
        assertNull(TaskRules.validate(original, null, today))
        for (invalid in listOf(original.copy(title = " "), original.copy(description = ""),
            original.copy(assignedTo = ""))) {
            assertEquals(TaskError.REQUIRED_FIELDS, TaskRules.validate(invalid, null, today))
        }
    }

    @Test fun parentRelationshipCannotBeChangedAfterCreation() {
        val original = task().copy(parentTaskId = "2")
        assertEquals(TaskError.INVALID_PARENT,
            TaskRules.validate(original.copy(parentTaskId = "3"), original, today))
        assertNull(TaskRules.validate(original.copy(notes = "Detalle"), original, today))
    }
}
