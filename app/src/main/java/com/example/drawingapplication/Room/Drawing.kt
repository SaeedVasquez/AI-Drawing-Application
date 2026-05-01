package com.example.drawingapplication.Room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity that represents a saved drawing.
 */
@Entity(tableName = "drawings")
data class Drawing(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String = "",
    val filePath: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
