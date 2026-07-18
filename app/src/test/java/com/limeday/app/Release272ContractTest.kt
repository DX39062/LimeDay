package com.limeday.app

import com.limeday.app.data.TodoGroup
import com.limeday.app.data.TodoGroupIconCatalog
import com.limeday.app.ui.LlmProviderMark
import com.limeday.app.ui.llmProviderMark
import com.limeday.app.ui.shouldCollapseTodoSearch
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Release272ContractTest {
    private fun source(path: String): String {
        val candidates = listOf(File(path), File("app/$path"))
        return candidates.firstOrNull(File::isFile)?.readText()
            ?: error("Missing source file: $path")
    }

    @Test
    fun groupCatalogHasSixteenStableMarksAndChoosesUnusedOnesFirst() {
        assertEquals(16, TodoGroupIconCatalog.keys.size)
        assertEquals("daily", TodoGroupIconCatalog.keys.first())
        assertEquals(TodoGroupIconCatalog.keys.size, TodoGroupIconCatalog.keys.distinct().size)

        val groups = listOf(
            TodoGroup(name = "工作", iconKey = "work", sortOrder = "1", deviceId = "test"),
            TodoGroup(name = "学习", iconKey = "study", sortOrder = "2", deviceId = "test")
        )
        assertEquals("home", TodoGroupIconCatalog.nextAvailable(groups))
        assertEquals("daily", TodoGroupIconCatalog.displayKey("folder", isInbox = true))
        assertNotEquals("folder", TodoGroupIconCatalog.displayKey("folder"))

        val legacy = groups + listOf(
            TodoGroup(id = "legacy-a", name = "旧一", iconKey = "folder", sortOrder = "3", deviceId = "test"),
            TodoGroup(id = "legacy-b", name = "旧二", iconKey = "leaf", sortOrder = "4", deviceId = "test")
        )
        val assignments = TodoGroupIconCatalog.legacyAssignments(legacy)
        assertEquals("home", assignments["legacy-a"])
        assertEquals("health", assignments["legacy-b"])
        val normalized = legacy.map { it.copy(iconKey = assignments[it.id] ?: it.iconKey) }
        assertTrue(TodoGroupIconCatalog.legacyAssignments(normalized).isEmpty())
    }

    @Test
    fun everyPresetMapsToItsOwnProviderMarkAndUnknownUsesCustom() {
        val ids = listOf(
            "openai", "anthropic", "gemini", "openrouter", "deepseek", "kimi", "qwen", "zhipu",
            "siliconflow", "minimax", "doubao", "xai", "mistral", "groq", "ollama"
        )
        val marks = ids.map(::llmProviderMark)
        assertEquals(ids.size, marks.distinct().size)
        assertEquals(LlmProviderMark.Custom, llmProviderMark("private-compatible"))
    }

    @Test
    fun applicationSourcesDoNotUseNativeMaterialOrAndroidDrawableIcons() {
        val sourceRoot = listOf(File("src/main"), File("app/src/main")).first(File::isDirectory)
        val kotlinAndXml = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "xml") }
            .joinToString("\n") { it.readText() }

        assertFalse(kotlinAndXml.contains("androidx.compose.material.icons"))
        assertFalse(Regex("\\bIcons\\.").containsMatchIn(kotlinAndXml))
        assertFalse(kotlinAndXml.contains("android.R.drawable"))
        assertFalse(Regex("Text\\(\\s*\"[←→↑↓]").containsMatchIn(kotlinAndXml))
    }

    @Test
    fun searchAndQuickGroupContractsAreWired() {
        val day = source("src/main/java/com/limeday/app/ui/DayScreen.kt")
        assertTrue(day.contains("searchHasFocused"))
        assertTrue(day.contains("shouldCollapseTodoSearch(searchWasFocused"))
        assertTrue(day.contains("quick_add_group"))
        assertTrue(day.contains("onAdd(clean, selectedGroup?.id"))
        assertTrue(day.contains("quick_add_manage_groups"))
    }

    @Test
    fun searchOnlyCollapsesAfterARealFocusLossWithBlankQuery() {
        assertFalse(shouldCollapseTodoSearch(wasFocused = false, hasFocused = false, isFocused = false, query = ""))
        assertFalse(shouldCollapseTodoSearch(wasFocused = false, hasFocused = true, isFocused = true, query = ""))
        assertTrue(shouldCollapseTodoSearch(wasFocused = true, hasFocused = true, isFocused = false, query = ""))
        assertFalse(shouldCollapseTodoSearch(wasFocused = true, hasFocused = true, isFocused = false, query = "有内容"))
    }

    @Test
    fun launcherShowsJournalRowsInsteadOfOneGiantCheck() {
        val foreground = source("src/main/res/drawable/ic_launcher_foreground.xml")
        val monochrome = source("src/main/res/drawable/ic_launcher_monochrome.xml")
        val legacy = source("src/main/res/mipmap/ic_launcher.xml")
        val legacyRound = source("src/main/res/mipmap/ic_launcher_round.xml")
        val adaptive = source("src/main/res/mipmap-anydpi/ic_launcher.xml")
        val adaptiveRound = source("src/main/res/mipmap-anydpi/ic_launcher_round.xml")
        assertTrue(foreground.contains("Three recognisable todo rows"))
        assertTrue(foreground.contains("Left binding"))
        assertTrue(foreground.contains("A lime slice"))
        listOf(monochrome, legacy, legacyRound).forEach { assertTrue(it.contains("M39,39 L43,43 L49,35")) }
        listOf(adaptive, adaptiveRound).forEach {
            assertTrue(it.contains("@drawable/ic_launcher_foreground"))
            assertTrue(it.contains("@drawable/ic_launcher_monochrome"))
        }
    }
}
