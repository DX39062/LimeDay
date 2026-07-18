package com.limeday.app

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Release271ContractTest {
    private fun source(path: String): String {
        val candidates = listOf(File(path), File("app/$path"))
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Missing source file: $path")
    }

    @Test
    fun mainActivityOptsOutOfPredictiveBackPreview() {
        val manifest = source("src/main/AndroidManifest.xml")
        assertTrue(manifest.contains("android:name=\".MainActivity\""))
        assertTrue(manifest.contains("android:enableOnBackInvokedCallback=\"false\""))
    }

    @Test
    fun todoMenuUsesDistinctHandDrawnIcons() {
        val components = source("src/main/java/com/limeday/app/ui/TodoComponents.kt")
        assertTrue(components.contains("TodoDetailsIcon()"))
        assertTrue(components.contains("TodoMoveDateIcon()"))
        assertFalse(components.contains("TodoOptionRow(\"移动到其他日期\", icon = { TodoCalendarIcon() }"))
    }

    @Test
    fun progressAndSearchUseCompactConditionalContracts() {
        val day = source("src/main/java/com/limeday/app/ui/DayScreen.kt")
        assertTrue(day.contains("daily_progress_compact"))
        assertTrue(day.contains("todo_search_button"))
        assertTrue(day.contains("state.todoViewMode == TodoViewMode.DAY && state.todoSearchQuery.isBlank()"))
        assertTrue(day.contains("Modifier.fillMaxWidth().height(48.dp)"))
    }
}
