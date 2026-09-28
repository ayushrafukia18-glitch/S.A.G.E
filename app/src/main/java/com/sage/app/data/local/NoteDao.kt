package com.sage.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface NoteDao {
    @Insert suspend fun insert(note: NoteEntity): Long
    @Query("SELECT * FROM notes ORDER BY createdAt DESC") suspend fun getAll(): List<NoteEntity>
}
