package com.limeday.app.ui

import com.limeday.app.data.TodoItem
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DayUiStateTest {
    @Test
    fun `progress is zero when day has no todos`() {
        val state = DayUiState(selectedDate = LocalDate.of(2026, 7, 16), isLoading = false)

        assertEquals(0, state.completedCount)
        assertEquals(0, state.progressPercent)
    }

    @Test
    fun `one of three completed todos gives thirty three percent`() {
        val date = "2026-07-16"
        val state = DayUiState(
            selectedDate = LocalDate.parse(date),
            todos = listOf(
                todo("1", date, "A", completed = true),
                todo("2", date, "B"),
                todo("3", date, "C")
            ),
            isLoading = false
        )

        assertEquals(1, state.completedCount)
        assertEquals(33, state.progressPercent)
    }

    private fun todo(id: String, date: String, title: String, completed: Boolean = false) = TodoItem(
        id = id,
        date = date,
        title = title,
        isCompleted = completed,
        sortOrder = id,
        deviceId = "device-a"
    )
}
