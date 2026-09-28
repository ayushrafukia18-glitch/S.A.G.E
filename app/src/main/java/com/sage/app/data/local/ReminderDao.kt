package com.sage.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ReminderDao {
    @Insert suspend fun insert(reminder: ReminderEntity): Long
    @Query("UPDATE reminders SET status = :status WHERE id = :id") suspend fun setStatus(id: Long, status: String)
    @Query("SELECT * FROM reminders WHERE id = :id") suspend fun getById(id: Long): ReminderEntity?
    @Query("SELECT * FROM reminders WHERE status = 'pending' ORDER BY remindAt ASC") suspend fun getPending(): List<ReminderEntity>
}
