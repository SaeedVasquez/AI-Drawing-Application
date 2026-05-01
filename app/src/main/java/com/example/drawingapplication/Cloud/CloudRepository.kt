package com.example.drawingapplication.Cloud

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class CloudRepository {
    private val auth = Firebase.auth
    private val db = Firebase.firestore
    private val storage = Firebase.storage

    companion object {
        private const val COLLECTION_USER_DRAWINGS = "user_drawings"
        private const val STORAGE_ROOT = "drawings"
        private const val TAG = "CloudRepository"
    }

    suspend fun uploadDrawing(bitmap: Bitmap, title: String): Result<String> {
        val userId = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))

        return try {
            // encode the bitmap as a PNG byte array
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val bytes = baos.toByteArray()

            // upload bytes to firebase Storage
            val now = System.currentTimeMillis()
            val path = "$STORAGE_ROOT/$userId/$now.png"
            val ref = storage.reference.child(path)
            ref.putBytes(bytes).await()

            // ask storage for a download URL
            val downloadUrl = ref.downloadUrl.await().toString()

            // write a metadata doc to Firestore
            val meta = CloudDrawing(
                userId = userId,
                imageUrl = downloadUrl,
                title = title,
                timestamp = now
            )
            db.collection(COLLECTION_USER_DRAWINGS).add(meta).await()

            // hand the URL back so the caller can display / share it.
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "uploadDrawing failed", e)
            Result.failure(e)
        }
    }


    suspend fun updateDrawing(docId: String, bitmap: Bitmap, title: String): Result<String> {
        val userId = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))

        return try {
            val docRef = db.collection(COLLECTION_USER_DRAWINGS).document(docId)
            val oldImageUrl = docRef.get().await().getString("imageUrl")

            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val bytes = baos.toByteArray()

            val now = System.currentTimeMillis()
            val path = "$STORAGE_ROOT/$userId/$now.png"
            val ref = storage.reference.child(path)
            ref.putBytes(bytes).await()
            val downloadUrl = ref.downloadUrl.await().toString()

            docRef.update(
                mapOf(
                    "imageUrl" to downloadUrl,
                    "title" to title,
                    "timestamp" to now
                )
            ).await()

            // cleanup old image after updating
            if (!oldImageUrl.isNullOrEmpty()) {
                storage.getReferenceFromUrl(oldImageUrl).delete().await()
            }
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Log.e(TAG, "updateDrawing failed", e)
            Result.failure(e)
        }
    }

    suspend fun downloadBitmap(imageUrl: String): ByteArray? {
        return try {
            val ref = storage.getReferenceFromUrl(imageUrl)
            val maxSize = 10L * 1024 * 1024
            ref.getBytes(maxSize).await()
        } catch (e: Exception) {
            Log.e(TAG, "downloadBitmap failed", e)
            null
        }
    }

    suspend fun getUserDrawings(): List<CloudDrawing> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshot = db.collection(COLLECTION_USER_DRAWINGS)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            snapshot.documents
                .mapNotNull { doc -> doc.toObject(CloudDrawing::class.java)?.copy(id = doc.id)
                }
                .sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "getUserDrawings failed", e)
            emptyList()
        }
    }

    suspend fun shareDrawing(imageUrl: String, receiverEmail: String): Result<Unit> {
        return try {
            val currentUser = auth.currentUser ?: return Result.failure(Exception("Not signed in"))

            val doc = SharedDrawing(
                imageUrl = imageUrl,
                senderId = currentUser.uid,
                senderEmail = currentUser.email ?: "Unknown",
                receiverEmail = receiverEmail,
                timestamp = System.currentTimeMillis()
            )

            val docRef = db.collection("shared_drawings").document()
            docRef.set(doc.copy(id = docRef.id)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unshareDrawing(sharedDrawingId: String): Result<Unit> {
        return try {
            db.collection("shared_drawings")
                .document(sharedDrawingId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Drawings this user shared with others
    suspend fun getSharedByMe(): List<SharedDrawing> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return db.collection("shared_drawings")
            .whereEqualTo("senderId", uid)
            .get().await()
            .toObjects(SharedDrawing::class.java)
    }

    // Drawings shared WITH this user
    suspend fun getSharedWithMe(): List<SharedDrawing> {
        val email = auth.currentUser?.email ?: return emptyList()
        return db.collection("shared_drawings")
            .whereEqualTo("receiverEmail", email)
            .get().await()
            .toObjects(SharedDrawing::class.java)
    }
}
