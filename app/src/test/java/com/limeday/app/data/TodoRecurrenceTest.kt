package com.limeday.app.data

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodoRecurrenceTest {
    private val friday = LocalDate.of(2026, 7, 17)

    @Test
    fun `weekday recurrence skips weekend`() {
        assertEquals(LocalDate.of(2026, 7, 20), TodoRecurrence.nextDate(TodoRecurrence.WEEKDAYS, friday))
    }

    @Test
    fun `custom intervals advance by declared unit`() {
        assertEquals(LocalDate.of(2026, 7, 31), TodoRecurrence.nextDate("interval:2:WEEKS", friday))
        assertEquals(LocalDate.of(2026, 10, 17), TodoRecurrence.nextDate("interval:3:MONTHS", friday))
    }

    @Test
    fun `unknown recurrence is safely normalized to none`() {
        assertEquals(TodoRecurrence.NONE, TodoRecurrence.normalize("yearly"))
        assertNull(TodoRecurrence.nextDate("yearly", friday))
    }
}
