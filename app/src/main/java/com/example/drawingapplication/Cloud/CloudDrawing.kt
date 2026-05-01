package com.example.drawingapplication.Cloud

/**
 * Document stored in "user_drawings" for firestore.
 *
 */
data class CloudDrawing(
    val id: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val title: String = "",
    val timestamp: Long = 0L
)
