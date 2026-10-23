package com.bheeshamks.freeflow.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a single recorded and transcribed dictation entry.
 */
@Entity(tableName = "dictation_history")
data class DictationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val text: String,
    val durationMs: Long = 0,
    val latencyMs: Long = 0,
    val wordCount: Int = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size,
    val targetPackage: String? = null
)
