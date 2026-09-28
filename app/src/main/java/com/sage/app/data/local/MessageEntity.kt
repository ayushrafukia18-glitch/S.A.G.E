package com.sage.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String, // "user" or "sage"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isUser: Boolean
        get() = sender.equals("user", ignoreCase = true)
}
