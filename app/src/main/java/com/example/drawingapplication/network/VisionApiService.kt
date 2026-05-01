package com.example.drawingapplication.network

import android.util.Log
import com.example.drawingapplication.BuildConfig
import com.example.drawingapplication.Model.VisionRequest
import com.example.drawingapplication.Model.VisionResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object VisionApiService : VisionService {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    val client = HttpClient(Android){
        install(ContentNegotiation){
            json(jsonParser)
        }
    }

    /**
     * Sends a VisionRequest to Google Cloud Vision API and returns a JSON response.
     * For text localization, this one uses Ktor's serialization/deserialization for
     * the request and response and is sent/received separately.
     */
    override suspend fun analyzeImage(request: VisionRequest): VisionResponse{
        val httpResponse = client.post("https://vision.googleapis.com/v1/images:annotate") {
            // Add API to the URL
            url{
                parameters.append("key", BuildConfig.VISION_API_KEY)
            }
            // Tell Google we are sending JSON
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (httpResponse.status.isSuccess()){
            return httpResponse.body()
        }else{
            Log.e("AI_ERROR", "API call failed with status: ${httpResponse.status}")
            return VisionResponse(responses = emptyList())
        }
    }

    /**
     * Sends a prebuilt JSON string to Vision API.
     * Used for object localization because kotlinx serialization
     * breaks the base64 string during encoding, resulting
     * in empty results. Building the JSON manually fixes this.
     */
    override suspend fun analyzeImageRawJson(rawJsonBody: String): VisionResponse {
        val httpResponse = client.post("https://vision.googleapis.com/v1/images:annotate") {
            url {
                parameters.append("key", BuildConfig.VISION_API_KEY)
            }
            contentType(ContentType.Application.Json)
            // send the raw JSON string directly instead of a serializable object
            setBody(rawJsonBody)
        }

        val rawJson = httpResponse.bodyAsText()

        return if (httpResponse.status.isSuccess()) {
            jsonParser.decodeFromString<VisionResponse>(rawJson)
        } else {
            VisionResponse(responses = emptyList())
        }
    }
}