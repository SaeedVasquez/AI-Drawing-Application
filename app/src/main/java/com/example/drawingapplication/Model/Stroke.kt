package com.example.drawingapplication.Model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * Stroke class, each stroke object will keep track of its
 * path, color, width, and shape of the tip.
 */
data class Stroke(
    val path: Path = Path(),
    val color: Color = Color.Black,
    val width: Float = 5f,
    val shapeType: ShapeType = ShapeType.LINE
)

/**
 * Enum class to help determine what the pens tip shape is.
 */
enum class ShapeType{
    LINE, CIRCLE, RECTANGLE
}