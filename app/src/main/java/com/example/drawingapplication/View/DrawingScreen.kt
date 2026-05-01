package com.example.drawingapplication.View

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.LayersClear
import androidx.compose.material.icons.outlined.LineWeight
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.drawingapplication.Model.ShapeType
import com.example.drawingapplication.ViewModel.DrawingViewModel
import com.example.drawingapplication.ui.theme.ToolbarGray
import com.example.drawingapplication.ui.theme.ToolButtonGray

// tracks which popup is currently open above the toolbar
enum class ToolbarPopup { NONE, COLORS, SHAPES, WIDTH, ACTIONS }

/**
 * Main drawing screen with a canvas and toolbar.
 * Supports drawing, shape tools, color/width selection,
 * and Google Cloud Vision AI analysis with bounding box overlays.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrawingScreen(myVM: DrawingViewModel, navController: NavHostController? = null) {

    val context = LocalContext.current

    // which popup is currently showing (only one at a time)
    var activePopup by remember { mutableStateOf(ToolbarPopup.NONE) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showCloudDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var drawingTitle by remember { mutableStateOf("") }

    // collect stroke properties from the viewmodel
    val color by myVM.colorReadOnly.collectAsState()
    val width by myVM.widthReadOnly.collectAsState()
    val shapeType by myVM.shapeTypeReadOnly.collectAsState()

    val strokes by myVM.strokesReadOnly.collectAsState()
    val currentStroke by myVM.currentStroke.collectAsState()
    val backgroundBitmap by myVM.backgroundBitmap.collectAsState()
    val currentDrawingId by myVM.currentDrawingId.collectAsState()
    val currentCloudDocId by myVM.currentCloudDocId.collectAsState()
    val cloudDrawings by myVM.cloudDrawings.collectAsState()
    val objects by myVM.detectedObjects.collectAsState()

    val isAnalyzing by myVM.isAnalyzing.collectAsState()

    // all available colors
    val colorPalette = listOf(
        Color.Black, Color.White, Color.Gray, Color.Blue,
        Color.Cyan, Color.Green, Color.Magenta, Color.Red,
        Color.Yellow,
        Color(0xFFFFA500), // orange
        Color(0xFF006400), // dark green
        Color(0xFF800080)  // purple
    )

    val shapeOptions = listOf(ShapeType.LINE, ShapeType.RECTANGLE, ShapeType.CIRCLE)

    // helper to toggle a popup
    fun togglePopup(popup: ToolbarPopup) {
        activePopup = if (activePopup == popup) ToolbarPopup.NONE else popup
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color.White)
        .statusBarsPadding()
        .navigationBarsPadding()
    ) {

        // canvas
        Canvas(
            modifier = Modifier
                .testTag("drawing_canvas")
                .fillMaxSize()
                .background(Color.White)
                .onSizeChanged { myVM.updateCanvasSize(it) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset -> myVM.startDrawing(offset) },
                        onDrag = { change, _ ->
                            change.consume()
                            myVM.updateDrawing(change.position)
                        },
                        onDragEnd = { myVM.finishDrawing() }
                    )
                }
        ) {
            clipRect(
                left = 0f, top = 0f,
                right = size.width, bottom = size.height
            ) {
                backgroundBitmap?.let { bmp ->
                    drawImage(image = bmp.asImageBitmap(), topLeft = Offset.Zero)
                }

                strokes.forEach { stroke ->
                    drawPath(
                        path = stroke.path,
                        color = stroke.color,
                        style = if (stroke.shapeType == ShapeType.LINE)
                            Stroke(width = stroke.width) else Fill
                    )
                }

                currentStroke?.let { stroke ->
                    drawPath(
                        path = stroke.path,
                        color = stroke.color,
                        style = if (stroke.shapeType == ShapeType.LINE)
                            Stroke(width = stroke.width) else Fill
                    )
                }
            }

            // bounding boxes + labels from AI detection
            objects.forEach { obj ->
                val points = obj.boundingPoly.vertices
                val normPoints = obj.boundingPoly.normalizedVertices

                // figure out the pixel coordinates depending on what the API returned
                // text detection gives us regular vertices (pixel coords)
                // object localization gives us normalizedVertices (0-1 range, need to multiply by canvas size)
                val left: Float
                val top: Float
                val right: Float
                val bottom: Float
                val isObject: Boolean

                if (points.size >= 4) {
                    // regular pixel coordinates from text detection
                    left = points[0].x.toFloat()
                    top = points[0].y.toFloat()
                    right = points[2].x.toFloat()
                    bottom = points[2].y.toFloat()
                    isObject = false

                } else if (normPoints.size >= 4) {
                    // normalized coords from object localization
                    val imgWidth = backgroundBitmap?.width?.toFloat() ?: size.width
                    val imgHeight = backgroundBitmap?.height?.toFloat() ?: size.height
                    left = normPoints[0].x * imgWidth
                    top = normPoints[0].y * imgHeight
                    right = normPoints[2].x * imgWidth
                    bottom = normPoints[2].y * imgHeight
                    isObject = true
                } else {
                    return@forEach // skip if no valid coordinates
                }

                val boxColor = if (isObject) Color.Red else Color.Blue

                // red outline around the detected object
                drawRect(
                    color = boxColor,
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    style = Stroke(width = 4f)
                )

                // label text setup
                val label: String
                if (obj.score > 0f) {
                    label = "${obj.name}: ${"%.2f".format(obj.score)}"
                } else {
                    label = obj.name
                }

                val textPaint = android.graphics.Paint().apply {
                    this.color = android.graphics.Color.WHITE
                    textSize = 36f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                val textWidth = textPaint.measureText(label)
                val textHeight = 40f
                val labelTop = (top - textHeight).coerceAtLeast(0f)

                // colored background behind label
                drawRect(
                    color = boxColor,
                    topLeft = Offset(left, labelTop),
                    size = Size(textWidth + 16f, textHeight),
                    style = Fill
                )

                // draw label text using native canvas
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    left,
                    labelTop + 30f,
                    textPaint
                )
            }
        }

        // loading circle while AI is analyzing
        if (isAnalyzing) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.DarkGray,
                    modifier = Modifier.testTag("loading_indicator"))
            }
        }

        // floating toolbar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // colors popup
            AnimatedVisibility(
                visible = activePopup == ToolbarPopup.COLORS,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    color = ToolbarGray,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        maxItemsInEachRow = 6
                    ) {
                        colorPalette.forEach { paletteColor ->
                            Box(
                                modifier = Modifier
                                    .testTag("color_item_${paletteColor.value}")
                                    .size(44.dp)
                                    .background(paletteColor, RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (paletteColor == color) 3.dp else 1.dp,
                                        color = if (paletteColor == color) Color.Black
                                        else Color.LightGray,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        myVM.changeColor(paletteColor)
                                        activePopup = ToolbarPopup.NONE
                                    }
                            )
                        }
                    }
                }
            }

            // shapes popup
            AnimatedVisibility(
                visible = activePopup == ToolbarPopup.SHAPES,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    color = ToolbarGray,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        shapeOptions.forEach { shape ->
                            val label = shape.name.lowercase()
                                .replaceFirstChar { it.uppercase() }
                            val isSelected = shape == shapeType

                            Surface(
                                color = if (isSelected) Color.Black else ToolButtonGray,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .testTag("tip_item_$label")
                                    .clickable {
                                        myVM.changeTip(shape)
                                        activePopup = ToolbarPopup.NONE
                                    }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else Color.Black,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(
                                        horizontal = 20.dp, vertical = 10.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // width popup
            AnimatedVisibility(
                visible = activePopup == ToolbarPopup.WIDTH,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    color = ToolbarGray,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Width: ${width.toInt()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.DarkGray,
                            modifier = Modifier
                                .testTag("width_text")
                                .padding(bottom = 4.dp)
                        )
                        Slider(
                            modifier = Modifier
                                .testTag("width_slider")
                                .fillMaxWidth(),
                            value = width,
                            onValueChange = { myVM.changeWidth(it) },
                            valueRange = 1f..50f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.Black,
                                activeTrackColor = Color.Black
                            )
                        )
                    }
                }
            }

            // actions popup
            AnimatedVisibility(
                visible = activePopup == ToolbarPopup.ACTIONS,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    color = ToolbarGray,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        maxItemsInEachRow = 4
                    ) {
                        ToolGridIconButton(
                            icon = Icons.Outlined.Save,
                            label = "Save",
                            testTag = "save_button",
                            onClick = {
                                activePopup = ToolbarPopup.NONE
                                if (currentDrawingId != null) {
                                    myVM.saveExistingDrawing(context)
                                    Toast.makeText(context, "Drawing updated!", Toast.LENGTH_SHORT).show()
                                } else {
                                    drawingTitle = ""
                                    showSaveDialog = true
                                }
                            }
                        )

                        ToolGridIconButton(
                            icon = Icons.Outlined.PhotoLibrary,
                            label = "Export",
                            testTag = "export_button",
                            onClick = {
                                activePopup = ToolbarPopup.NONE
                                val saved = myVM.saveToGallery(context)
                                if (saved) {
                                    Toast.makeText(context, "Saved to Photos!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        ToolGridIconButton(
                            icon = Icons.Outlined.IosShare,
                            label = "Share",
                            testTag = "share_button",
                            onClick = {
                                activePopup = ToolbarPopup.NONE
                                showShareDialog = true
                            }
                        )

                        ToolGridIconButton(
                            icon = Icons.Outlined.Psychology,
                            label = "AI",
                            testTag = "ai_button",
                            onClick = {
                                activePopup = ToolbarPopup.NONE
                                myVM.performImageAnalysis()
                            }
                        )

                        ToolGridIconButton(
                            icon = Icons.Outlined.CloudUpload,
                            label = if (currentCloudDocId != null) "Update" else "Cloud",
                            onClick = {
                                activePopup = ToolbarPopup.NONE
                                val existingId = currentCloudDocId
                                if (existingId != null) {
                                    val existingTitle = cloudDrawings
                                        .find { it.id == existingId }?.title
                                        .orEmpty()
                                    Toast.makeText(
                                        context,
                                        "Updating cloud drawing...",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    myVM.uploadCurrentDrawingToCloud(existingTitle) { success ->
                                        val msg = if (success) "Cloud drawing updated!"
                                                  else "Cloud update failed"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    drawingTitle = ""
                                    showCloudDialog = true
                                }
                            }
                        )

                        if (currentDrawingId != null) {
                            ToolGridIconButton(
                                icon = Icons.Outlined.DeleteOutline,
                                label = "Delete",
                                testTag = "delete_button",
                                isDestructive = true,
                                onClick = {
                                    activePopup = ToolbarPopup.NONE
                                    myVM.deleteCurrentDrawing()
                                    Toast.makeText(context, "Drawing deleted!", Toast.LENGTH_SHORT).show()
                                    navController?.popBackStack()
                                }
                            )
                        }
                    }
                }
            }

            // toolbar pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = ToolbarGray,
                    shape = RoundedCornerShape(28.dp),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // home button
                        if (navController != null) {
                            ToolbarIconButton(
                                icon = Icons.Outlined.Home,
                                contentDescription = "Home",
                                onClick = { navController.popBackStack() }
                            )
                        }

                        // color circle
                        Box(
                            modifier = Modifier
                                .testTag("color_box")
                                .size(32.dp)
                                .background(color, CircleShape)
                                .border(2.dp, Color.Black, CircleShape)
                                .clickable { togglePopup(ToolbarPopup.COLORS) }
                        )

                        // shape picker
                        ToolbarIconButton(
                            icon = Icons.Outlined.Category,
                            contentDescription = "Shapes",
                            testTag = "tip_selector",
                            onClick = { togglePopup(ToolbarPopup.SHAPES) }
                        )

                        // width slider
                        ToolbarIconButton(
                            icon = Icons.Outlined.LineWeight,
                            contentDescription = "Width",
                            testTag = "width_button",
                            onClick = { togglePopup(ToolbarPopup.WIDTH) }
                        )

                        // clear canvas
                        ToolbarIconButton(
                            icon = Icons.Outlined.LayersClear,
                            contentDescription = "Clear",
                            testTag = "clear_button",
                            onClick = { myVM.clearCanvas() }
                        )
                    }
                }

                // more actions button
                Surface(
                    color = Color.White,
                    shape = CircleShape,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .testTag("actions_button")
                        .size(48.dp)
                        .clickable { togglePopup(ToolbarPopup.ACTIONS) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (activePopup == ToolbarPopup.ACTIONS)
                                Icons.Default.Close else Icons.Default.MoreHoriz,
                            contentDescription = if (activePopup == ToolbarPopup.ACTIONS)
                                "Close actions" else "More actions",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }

    // save dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            title = { Text("Save As") },
            text = {
                OutlinedTextField(
                    value = drawingTitle,
                    onValueChange = { drawingTitle = it },
                    label = { Text("Title", color = Color.Gray) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = drawingTitle.ifBlank { "Untitled Drawing" }
                        myVM.saveNewDrawing(context, title)
                        Toast.makeText(context, "Drawing saved!", Toast.LENGTH_SHORT).show()
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showSaveDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ToolButtonGray,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // cloud upload / update dialog
    if (showCloudDialog) {
        val isUploading by myVM.isUploadingToCloud.collectAsState()
        val isUpdate = currentCloudDocId != null
        AlertDialog(
            onDismissRequest = { if (!isUploading) showCloudDialog = false },
            containerColor = Color.White,
            titleContentColor = Color.Black,
            textContentColor = Color.Black,
            title = { Text(if (isUpdate) "Update on Cloud" else "Upload to Cloud") },
            text = {
                Column {
                    OutlinedTextField(
                        value = drawingTitle,
                        onValueChange = { drawingTitle = it },
                        label = { Text("Title", color = Color.Gray) },
                        singleLine = true,
                        enabled = !isUploading
                    )
                    if (isUploading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.DarkGray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Uploading...", color = Color.DarkGray)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isUploading,
                    onClick = {
                        val title = drawingTitle.ifBlank { "Untitled Drawing" }
                        myVM.uploadCurrentDrawingToCloud(title) { success ->
                            val msg = when {
                                success && isUpdate -> "Cloud drawing updated!"
                                success -> "Uploaded to cloud!"
                                isUpdate -> "Cloud update failed"
                                else -> "Cloud upload failed"
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) showCloudDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isUpdate) "Update" else "Upload")
                }
            },
            dismissButton = {
                Button(
                    enabled = !isUploading,
                    onClick = { showCloudDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ToolButtonGray,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    if (showShareDialog) {
        ShareDialog(
            onShare = { recipientEmail ->
                val url = myVM.currentCloudImageUrl
                if (url != null) {
                    myVM.shareDrawing(url, recipientEmail)
                    Toast.makeText(context, "Shared!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Upload to cloud first", Toast.LENGTH_SHORT).show()
                }
                showShareDialog = false
            },
            onDismiss = { showShareDialog = false }
        )
    }
}

@Composable
fun ToolbarIconButton(
    icon: ImageVector,
    contentDescription: String,
    testTag: String = "",
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .size(42.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.Black,
            modifier = Modifier.size(24.dp)
        )
    }
}
@Composable
fun ToolGridIconButton(
    icon: ImageVector,
    label: String,
    testTag: String = "",
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        color = if (isDestructive) Color(0xFFFFE5E5) else ToolButtonGray,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .size(72.dp)
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isDestructive) Color.Red else Color.Black,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = if (isDestructive) Color.Red else Color.Black,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
@Composable
fun ShareDialog(onShare: (String) -> Unit, onDismiss: () -> Unit) {
    var email by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        titleContentColor = Color.Black,
        title = { Text("Share Drawing") },
        text = {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Recipient Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onShare(email) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                )
            ) { Text("Share") }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ToolButtonGray,
                    contentColor = Color.Black
                )
            ) { Text("Cancel") }
        }
    )
}
