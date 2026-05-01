package com.example.drawingapplication

import android.app.Application
import androidx.room.Room
import com.example.drawingapplication.Cloud.CloudRepository
import com.example.drawingapplication.Room.DrawingDatabase

class DrawingApplication : Application() {
    val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            DrawingDatabase::class.java,
            "drawing_database"
        ).build()
    }

    val repository by lazy {
        DrawingRepository(database.drawingDao())
    }
    val cloudRepository by lazy { CloudRepository() }
}
