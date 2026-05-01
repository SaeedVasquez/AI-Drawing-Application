package com.example.drawingapplication.Room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DrawingDao {
    @Insert
    suspend fun insert(drawing: Drawing)

    @Update
    suspend fun update(drawing: Drawing)

    @Delete
    suspend fun delete(drawing: Drawing)

    @Query("SELECT * FROM drawings ORDER BY createdAt DESC")
    fun getAllDrawings(): Flow<List<Drawing>>
}
