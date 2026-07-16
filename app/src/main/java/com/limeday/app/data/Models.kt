package com.limeday.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "todos", indices = [Index("date")])
data class TodoItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val title: String,
    val note: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_reviews")
data class DailyReview(
    @PrimaryKey val date: String,
    val highlight: String = "",
    val challenge: String = "",
    val learning: String = "",
    val tomorrowFocus: String = "",
    val mood: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_summaries")
data class DailySummary(
    @PrimaryKey val date: String,
    val content: String,
    val provider: String,
    val model: String,
    val generatedAt: Long = System.currentTimeMillis()
)
