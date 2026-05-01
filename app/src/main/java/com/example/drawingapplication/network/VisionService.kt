package com.example.drawingapplication.network

import com.example.drawingapplication.Model.VisionRequest
import com.example.drawingapplication.Model.VisionResponse

interface VisionService {
    suspend fun analyzeImage(request: VisionRequest): VisionResponse
    suspend fun analyzeImageRawJson(rawJsonBody: String): VisionResponse
}