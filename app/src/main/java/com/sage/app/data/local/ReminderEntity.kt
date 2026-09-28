package com.sage.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val remindAt: Long,
    val status: String = "pending",
    val createdAt: Long = System.currentTimeMillis(),
)
