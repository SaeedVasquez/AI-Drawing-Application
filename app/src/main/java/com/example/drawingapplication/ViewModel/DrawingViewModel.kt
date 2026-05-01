package com.example.drawingapplication.ViewModel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.drawingapplication.Cloud.CloudDrawing
import com.example.drawingapplication.Cloud.CloudRepository
import com.example.drawingapplication.DrawingRepository
import com.example.drawingapplication.Model.ShapeType
import com.example.drawingapplication.Model.Stroke
import com.example.drawingapplication.Room.Drawing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.core.graphics.createBitmap
import com.example.drawingapplication.Cloud.SharedDrawing
import com.example.drawingapplication.Model.AnnotateImageRequest
import com.example.drawingapplication.Model.LocalizedObjectAnnotation
import com.example.drawingapplication.Model.VisionFeature
import com.example.drawingapplication.Model.VisionImage
import com.example.drawingapplication.Model.VisionRequest
import com.example.drawingapplication.network.VisionApiService
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import com.example.drawingapplication.network.VisionService

/**
 * ViewModel class that will handle receiving data from the UI and feed the UI with data for the
 * drawing screen.
 */
class DrawingViewModel(
    private val repository: DrawingRepository,
    private val cloudRepository: CloudRepository,
    private val visionService: VisionService = VisionApiService
) : ViewModel(){

    private val _cloudDrawings = MutableStateFlow<List<CloudDrawing>>(emptyList())
    val cloudDrawings: StateFlow<List<CloudDrawing>> = _cloudDrawings

    private val _isUploadingToCloud = MutableStateFlow(false)
    val isUploadingToCloud: StateFlow<Boolean> = _isUploadingToCloud

    private val _currentCloudDocId = MutableStateFlow<String?>(null)
    val currentCloudDocId: StateFlow<String?> = _currentCloudDocId

    private val _sharedByMe = MutableStateFlow<List<SharedDrawing>>(emptyList())
    val sharedByMe: StateFlow<List<SharedDrawing>> = _sharedByMe

    private val _sharedWithMe = MutableStateFlow<List<SharedDrawing>>(emptyList())
    val sharedWithMe: StateFlow<List<SharedDrawing>> = _sharedWithMe

    // Observe the Flow from the repository and converts it to a StateFlow for the UI
    val allSavedDrawings: StateFlow<List<Drawing>> = repository.allDrawings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing

    // hold a MutableStateList to hold each stroke object
    private val _strokes = MutableStateFlow(listOf<Stroke>())
    val strokesReadOnly : StateFlow<List<Stroke>> = _strokes

    // Keep track of current stroke
    private val _currentStroke = MutableStateFlow<Stroke?>(null)
    val currentStroke: StateFlow<Stroke?> = _currentStroke

    // Remember canvas settings
    private var _selectedColor = MutableStateFlow(Color.Black)
    val colorReadOnly : StateFlow<Color> = _selectedColor

    private var _selectedWidth = MutableStateFlow(5f)
    val widthReadOnly : StateFlow<Float> = _selectedWidth

    private var _selectedShapeType = MutableStateFlow(ShapeType.LINE)
    val shapeTypeReadOnly : StateFlow<ShapeType> = _selectedShapeType

    // Background bitmap for loaded saved drawings
    private val _backgroundBitmap = MutableStateFlow<Bitmap?>(null)
    val backgroundBitmap: StateFlow<Bitmap?> = _backgroundBitmap

    // Track which drawing is being edited (null = new drawing)
    private val _currentDrawingId = MutableStateFlow<Int?>(null)
    val currentDrawingId: StateFlow<Int?> = _currentDrawingId

    // Track canvas size for bitmap capture
    private var _canvasSize = IntSize.Zero
    fun updateCanvasSize(size: IntSize) { _canvasSize = size }

    // keep the original imported image bytes so we dont have to re-encode
    private var _importedImageBytes: ByteArray? = null

    // Hold the results from performing an image analysis
    private val _detectedObjects = MutableStateFlow<List<LocalizedObjectAnnotation>>(emptyList())
    val detectedObjects: StateFlow<List<LocalizedObjectAnnotation>> = _detectedObjects

    // cloud functions
    fun refreshCloudDrawings() {
        viewModelScope.launch {
            _cloudDrawings.value = cloudRepository.getUserDrawings()
        }
    }

    fun loadCloudDrawing(cloud: CloudDrawing) {
        _strokes.value = emptyList()
        _backgroundBitmap.value = null
        _currentDrawingId.value = null
        _currentCloudDocId.value = cloud.id  // remember which doc we're editing
        _detectedObjects.value = emptyList()
        viewModelScope.launch {
            val bytes = cloudRepository.downloadBitmap(cloud.imageUrl)
            if (bytes != null) {
                _backgroundBitmap.value =
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        }
    }

    fun uploadCurrentDrawingToCloud(title: String, onComplete: (Boolean) -> Unit) {
        if (_canvasSize == IntSize.Zero) {
            onComplete(false)
            return
        }
        val bitmap = captureCanvasAsBitmap()
        val existingDocId = _currentCloudDocId.value
        viewModelScope.launch {
            _isUploadingToCloud.value = true
            val result = if (existingDocId != null) {
                cloudRepository.updateDrawing(existingDocId, bitmap, title)
            } else {
                cloudRepository.uploadDrawing(bitmap, title)
            }
            _isUploadingToCloud.value = false
            if (result.isSuccess) {
                refreshCloudDrawings()
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun shareDrawing(imageUrl: String, receiverEmail: String) {
        viewModelScope.launch {
            cloudRepository.shareDrawing(imageUrl, receiverEmail)
            loadSharedByMe()
        }
    }

    fun unshareDrawing(id: String) {
        viewModelScope.launch {
            cloudRepository.unshareDrawing(id)
            loadSharedByMe()
        }
    }

    fun loadSharedByMe() {
        viewModelScope.launch {
            _sharedByMe.value = cloudRepository.getSharedByMe()
        }
    }

    fun loadSharedWithMe() {
        viewModelScope.launch {
            _sharedWithMe.value = cloudRepository.getSharedWithMe()
        }
    }

    val currentCloudImageUrl: String?
        get() = cloudDrawings.value.find { it.id == _currentCloudDocId.value }?.imageUrl

    // ---------------Functions to set the Stroke properties---------------------
    /**
     * Change pen color
     */
    fun changeColor(newColor: Color){
        _selectedColor.value = newColor
    }

    /**
     * Change pen width
     */
    fun changeWidth(newWidth: Float){
        _selectedWidth.value = newWidth
    }

    /**
     * Change pen tip
     */
    fun changeTip(newShape: ShapeType){
        _selectedShapeType.value = newShape
    }

    // ---------------Action methods------------------------------------

    /**
     * Begins a new stroke at the given starting point
     */
    fun startDrawing(startingPoint: Offset){
        val newPath = Path().apply { moveTo(startingPoint.x, startingPoint.y) }
        // when user starts drawing again, boxes and labels go away
        _detectedObjects.value = emptyList()
        _currentStroke.value = Stroke(
            path = newPath,
            color = _selectedColor.value,
            width = _selectedWidth.value,
            shapeType = _selectedShapeType.value
        )
    }

    /**
     * Update the path of the current stroke
     */
    fun updateDrawing(newPoint: Offset){
        _currentStroke.value?.let { stroke ->

            val size = stroke.width

            when(stroke.shapeType) {
                ShapeType.LINE -> { /* handled below */ }

                ShapeType.RECTANGLE -> {
                    stroke.path.addRect(
                        Rect(
                            newPoint.x - size/2, newPoint.y - size/2,
                            newPoint.x + size/2, newPoint.y + size/2
                        )
                    )
                }

                ShapeType.CIRCLE -> {
                    stroke.path.addOval(
                        Rect(
                            newPoint.x - size/2, newPoint.y - size/2,
                            newPoint.x + size/2, newPoint.y + size/2
                        )
                    )
                }
            }

            val newPath = Path().apply {
                addPath(stroke.path)

                if (stroke.shapeType == ShapeType.LINE) {
                    lineTo(newPoint.x, newPoint.y)
                }
            }
            _currentStroke.value = stroke.copy(path = newPath)
        }
    }

    /**
     * Finishes a stroke at any given point
     */
    fun finishDrawing(){
        _currentStroke.value?.let { strokeToAdd ->
            _strokes.update { history ->
                history + strokeToAdd
            }
        }
        _currentStroke.value = null
    }

    /**
     * Clear the canvas visuals only (keep drawing ID for re-saving)
     */
    fun clearCanvas(){
        _strokes.value = emptyList()
        _backgroundBitmap.value = null
        _detectedObjects.value = emptyList()
        _importedImageBytes = null
    }

    /**
     * Reset everything for a brand new drawing
     */
    fun startNewDrawing(){
        _strokes.value = emptyList()
        _backgroundBitmap.value = null
        _currentDrawingId.value = null
        _currentCloudDocId.value = null
        _detectedObjects.value = emptyList()
        _importedImageBytes = null
    }

    /**
     * Import an image from the device gallery as a background
     */
    fun importImage(context: Context, uri: Uri) {
        // read the raw bytes so we can send the original to the API later
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return
        _importedImageBytes = rawBytes
        val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
        _strokes.value = emptyList()
        _backgroundBitmap.value = bitmap
        _currentDrawingId.value = null
        _currentCloudDocId.value = null
        // clear old bounding boxes when importing a new image
        _detectedObjects.value = emptyList()
        // auto analyze when importing a new image
        performImageAnalysis()
    }

    /**
     * Load a saved drawing from gallery into the canvas
     */
    fun loadDrawing(drawingId: Int, filePath: String) {
        _strokes.value = emptyList()
        _backgroundBitmap.value = BitmapFactory.decodeFile(filePath)
        _currentDrawingId.value = drawingId
        _detectedObjects.value = emptyList()
        _importedImageBytes = null
    }

    /**
     * Capture the current canvas content as a Bitmap
     */
    private fun captureCanvasAsBitmap(): Bitmap {
        val bitmap = createBitmap(_canvasSize.width, _canvasSize.height)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        _backgroundBitmap.value?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        _strokes.value.forEach { stroke ->
            val paint = android.graphics.Paint().apply {
                color = stroke.color.toArgb()
                strokeWidth = stroke.width
                style = if (stroke.shapeType == ShapeType.LINE)
                    android.graphics.Paint.Style.STROKE
                else {
                    android.graphics.Paint.Style.FILL
                }
                isAntiAlias = true
            }
            canvas.drawPath(stroke.path.asAndroidPath(), paint)
        }
        return bitmap
    }

    /**
     * Save a new drawing: capture bitmap to file, insert record into Room
     */
    fun saveNewDrawing(context: Context, title: String): Boolean {
        if (_canvasSize == IntSize.Zero)
            return false
        val bitmap = captureCanvasAsBitmap()
        val fileName = "drawing_${System.currentTimeMillis()}.png"
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        viewModelScope.launch { repository.insert(Drawing(title = title, filePath = file.absolutePath)) }
        return true
    }

    /**
     * Save over an existing drawing: capture bitmap to file, update record in Room
     */
    fun saveExistingDrawing(context: Context): Boolean {
        val existingId = _currentDrawingId.value ?: return false
        if (_canvasSize == IntSize.Zero)
            return false
        val existing = allSavedDrawings.value.find { it.id == existingId }
        val title = existing?.title ?: "Drawing"
        val bitmap = captureCanvasAsBitmap()
        val fileName = "drawing_${System.currentTimeMillis()}.png"
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        viewModelScope.launch { repository.update(Drawing(id = existingId, title = title, filePath = file.absolutePath)) }
        return true
    }

    /**
     * Delete the currently loaded drawing from Room and reset state
     */
    fun deleteCurrentDrawing() {
        val existingId = _currentDrawingId.value ?: return
        val drawing = allSavedDrawings.value.find { it.id == existingId } ?: return
        viewModelScope.launch { repository.delete(drawing) }
        startNewDrawing()
    }

    /**
     * Save drawing to photo gallery
     */
    fun saveToGallery(context: Context): Boolean {
        if (_canvasSize == IntSize.Zero)
            return false

        val bitmap = captureCanvasAsBitmap()
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "drawing_${System.currentTimeMillis()}.png")
            put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures")
        }

        val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
        return uri != null
    }

    /**
     * Export drawing as a shareable URI
     */
    fun getShareFileUri(context: Context): Uri? {
        if (_canvasSize == IntSize.Zero)
            return null
        val bitmap = captureCanvasAsBitmap()
        val shareFile = File(context.cacheDir, "shared_drawing.png")
        FileOutputStream(shareFile).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shareFile)
    }

    /**
     *
     */
    private fun encodeBitmapToBase64(bitmap: Bitmap): String{
        val byteArrayOutputStream = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        // NO_WRAP is important since Google doesn't like new lines in strings
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Sends the canvas/imported image to Google Cloud Vision API for analysis.
     */
    fun performImageAnalysis(){
        viewModelScope.launch {
            _isAnalyzing.value = true
            try{
                // For imported images use original bytes if we have them, otherwise capture canvas
                val base64Image = if (_importedImageBytes != null) {
                    Base64.encodeToString(_importedImageBytes, Base64.NO_WRAP)
                } else {
                    encodeBitmapToBase64(captureCanvasAsBitmap())
                }

                // object json manually built, kotlinx serialization breaks it
                val objectJson = """{"requests":[{"image":{"content":"$base64Image"},"features":[{"type":"OBJECT_LOCALIZATION"}]}]}"""

                // labels and text
                val labelTextRequest = VisionRequest(
                    requests = listOf(AnnotateImageRequest(image = VisionImage(content = base64Image),
                                                           features = listOf(VisionFeature(type = "LABEL_DETECTION"),
                                                                      VisionFeature(type = "TEXT_DETECTION")))
                    )
                )

                // send both requests
                val objectResponse = visionService.analyzeImageRawJson(objectJson)
                val labelTextResponse = visionService.analyzeImage(labelTextRequest)

                val objResult = objectResponse.responses.firstOrNull()
                val ltResult = labelTextResponse.responses.firstOrNull()

                // localized objects with bounding boxes
                val objects = objResult?.localizedObjectAnnotations ?: emptyList()

                // text annotations converted to same format for drawing boxes
                val textAsObjects = ltResult?.textAnnotations?.drop(1)?.map { text ->
                    LocalizedObjectAnnotation(
                        name = text.description,
                        boundingPoly = text.boundingPoly
                    )
                } ?: emptyList()

                _detectedObjects.value = objects + textAsObjects
            }catch (e: Exception){
                Log.e("AI_ERROR", "Analysis failed: ${e.message}", e)
            }
            finally {
                _isAnalyzing.value = false
            }
        }
    }
}

/**
 * Factory for creating DrawingViewModel instances with a DrawingRepository dependency.
 */
class DrawingViewModelFactory(
    private val repository: DrawingRepository,
    private val cloudRepository: CloudRepository
): ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DrawingViewModel::class.java)){
            @Suppress("UNCHECKED_CAST")
            return DrawingViewModel(repository, cloudRepository) as T
        }
        throw IllegalArgumentException()
    }
}

