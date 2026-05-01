package com.example.drawingapplication.Room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Drawing::class], version = 1)
abstract class DrawingDatabase : RoomDatabase() {
    abstract fun drawingDao(): DrawingDao
}
