package com.example.drawingapplication.Model

import kotlinx.serialization.Serializable

// Request classes (what we send to Google)

/**
 * This data class allows us to send multiple images to Google at once.
 */
@Serializable
data class VisionRequest(
    val requests: List<AnnotateImageRequest>
)

/**
 * Represents a single image and features you want the AI to look for.
 */
@Serializable
data class AnnotateImageRequest(
    val image: VisionImage,
    val features: List<VisionFeature>
)

/**
 * Feature the AI should look for.
 */
@Serializable
data class VisionFeature(
    val type: String = "OBJECT_LOCALIZATION" // Tells Google to look for objects
)

/**
 * Holds an image that must be a base64 string
 */
@Serializable
data class VisionImage(
    val content: String // This will be a base64 string of an image
)

// Response classes (What Google sends back)

/**
 * Contains list of results
 */
@Serializable
data class VisionResponse(
    val responses: List<AnnotateImageResponse>
)

/**
 * Result for single images.
 * Holds the things found by the AI
 */
@Serializable
data class AnnotateImageResponse(
    val localizedObjectAnnotations: List<LocalizedObjectAnnotation> = emptyList(),
    val textAnnotations: List<TextAnnotation> = emptyList(),
    // need this to parse the label/category results from the API
    val labelAnnotations: List<LabelAnnotation> = emptyList()
)

/**
 * Represents a label/category detected in the image (e.g., "Fruit", "Food").
 * Added this so we can grab the category names and confidence from LABEL_DETECTION
 */
@Serializable
data class LabelAnnotation(
    val description: String,
    val score: Float = 0f // confidence score from 0 to 1
)

/**
 * Represents specific text found in the image.
 */
@Serializable
data class TextAnnotation(
    val description: String,
    val boundingPoly: BoundingPoly
)

/**
 * Represents an object found by the AI.
 */
@Serializable
data class LocalizedObjectAnnotation(
    val name: String,
    val score: Float = 0f, // confidence score, was getting ignored before bc of ignoreUnknownKeys
    val boundingPoly: BoundingPoly
)

/**
 *  Polygon that wraps the item that is detected in the image.
 */
@Serializable
data class BoundingPoly(
    val vertices: List<Vertex> = emptyList(),
    val normalizedVertices: List<NormalizedVertex> = emptyList()
)

@Serializable
data class Vertex(val x: Int = 0, val y: Int =0)

@Serializable
data class NormalizedVertex(
    val x: Float = 0f,
    val y: Float = 0f
)