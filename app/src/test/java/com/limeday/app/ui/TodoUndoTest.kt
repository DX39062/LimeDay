package com.limeday.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TodoUndoTest {
    @Test
    fun `single deletion uses compact message`() {
        assertEquals("已移入回收站", todoUndoMessage(1))
    }

    @Test
    fun `multiple deletions use one batch count`() {
        assertEquals("已移入回收站 3 项", todoUndoMessage(3))
    }
}
