package com.example.drawingapplication

import com.example.drawingapplication.Room.Drawing
import com.example.drawingapplication.Room.DrawingDao
import kotlinx.coroutines.flow.Flow

class DrawingRepository(
    private val dao: DrawingDao
) {
    val allDrawings: Flow<List<Drawing>> = dao.getAllDrawings()

    suspend fun insert(drawing: Drawing) {
        dao.insert(drawing)
    }

    suspend fun update(drawing: Drawing) {
        dao.update(drawing)
    }

    suspend fun delete(drawing: Drawing) {
        dao.delete(drawing)
    }
}
