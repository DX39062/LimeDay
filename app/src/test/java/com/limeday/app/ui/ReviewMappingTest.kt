package com.limeday.app.ui

import com.limeday.app.data.DailyReview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewMappingTest {
    @Test
    fun `new free writing is displayed without a legacy label`() {
        val review = review(highlight = "今天随手记了两笔")

        assertEquals("今天随手记了两笔", review.freeWriteText())
    }

    @Test
    fun `legacy review fields are merged with their original labels`() {
        val review = review(
            highlight = "完成发布",
            learning = "先验证再发布",
            tomorrowFocus = "观察反馈"
        )

        val merged = review.freeWriteText()

        assertTrue(merged.contains("今日亮点：完成发布"))
        assertTrue(merged.contains("今日收获：先验证再发布"))
        assertTrue(merged.contains("明日重点：观察反馈"))
    }

    @Test
    fun `editing merged free writing clears legacy duplicate fields`() {
        val updated = review(learning = "旧收获", tomorrowFocus = "旧重点")
            .withFreeWrite("合并后的完整内容")

        assertEquals("合并后的完整内容", updated.highlight)
        assertEquals("", updated.learning)
        assertEquals("", updated.tomorrowFocus)
    }

    private fun review(
        highlight: String = "",
        learning: String = "",
        tomorrowFocus: String = ""
    ) = DailyReview(
        date = "2026-07-17",
        highlight = highlight,
        learning = learning,
        tomorrowFocus = tomorrowFocus,
        deviceId = "device-test"
    )
}
