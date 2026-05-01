package com.example.drawingapplication.Cloud

data class SharedDrawing(
    val id: String = "",
    val imageUrl: String = "",
    val senderId: String = "",
    val senderEmail: String = "",
    val receiverEmail: String = "",
    val timestamp: Long = System.currentTimeMillis()
)