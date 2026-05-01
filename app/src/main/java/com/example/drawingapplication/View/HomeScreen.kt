package com.example.drawingapplication.View

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.drawingapplication.Cloud.CloudDrawing
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.drawingapplication.Room.Drawing
import com.example.drawingapplication.ViewModel.DrawingViewModel
import com.example.drawingapplication.ui.theme.ToolbarGray
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Home screen that acts as a gallery and displays all saved drawings.
 * Shows drawing thumbnails.
 * Ability to start new drawing or import an image.
 */
@Composable
fun HomeScreen(navController: NavHostController, viewModel: DrawingViewModel,
               onSignOut: () -> Unit = {}) {
    val context = LocalContext.current
    val savedDrawings by viewModel.allSavedDrawings.collectAsState()
    val cloudDrawings by viewModel.cloudDrawings.collectAsState()
    val sharedWithMe by viewModel.sharedWithMe.collectAsState()
    val sharedByMe by viewModel.sharedByMe.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSharedWithMe()
        viewModel.loadSharedByMe()
    }

    // pull user's cloud drawings from Firestore.
    LaunchedEffect(Firebase.auth.currentUser?.uid) {
        viewModel.refreshCloudDrawings()
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importImage(context, uri)
            navController.navigate("draw")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.testTag("gallery_title"),
                    text = "My Gallery",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                TextButton(onClick = onSignOut) {
                    Text("Sign Out", color = Color.Gray)
                }
            }
            Firebase.auth.currentUser?.email?.let { email ->
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            // import image button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Surface(
                    color = ToolbarGray,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .testTag("import_image_button")
                        .clickable {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                ) {
                    Text(
                        text = "Import Image",
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Black
                    )
                }
            }

            if (savedDrawings.isEmpty() && cloudDrawings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modifier = Modifier.testTag("empty_gallery_text"),
                        text = "No drawings saved yet.",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("gallery_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (savedDrawings.isNotEmpty()) {
                        item { SectionHeader("On this device") }
                        items(savedDrawings) { drawing ->
                            DrawingItem(drawing, onClick = {
                                navController.navigate("draw/${drawing.id}")
                            })
                        }
                    }
                    if (cloudDrawings.isNotEmpty()) {
                        item { SectionHeader("Cloud") }
                        items(cloudDrawings) { cloud ->
                            CloudDrawingItem(cloud, onClick = {
                                viewModel.loadCloudDrawing(cloud)
                                navController.navigate("draw")
                            })
                        }
                    }

                    // Shows items shared with the current user
                    if(sharedWithMe.isNotEmpty()){
                        item { SectionHeader("Shared with Me") }
                        items(sharedWithMe) { shared ->
                            CloudDrawingItem(
                                CloudDrawing(
                                    imageUrl = shared.imageUrl,
                                    title = "From ${shared.senderEmail}"
                                ),
                                onClick = {
                                    viewModel.loadCloudDrawing(CloudDrawing(imageUrl = shared.imageUrl))
                                    navController.navigate("draw")
                                }
                            )
                        }
                    }

                    // Shows items current user has shared
                    if(sharedByMe.isNotEmpty()){
                        item {SectionHeader("Shared by Me")}
                        items(sharedByMe) { shared ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ){
                                Box(modifier = Modifier.weight(1f)){
                                    CloudDrawingItem(
                                        CloudDrawing(imageUrl = shared.imageUrl, title = "To ${shared.receiverEmail}"),
                                        onClick = {
                                            viewModel.loadCloudDrawing(CloudDrawing(imageUrl = shared.imageUrl))
                                            navController.navigate("draw")
                                        }
                                    )
                                }

                                IconButton(onClick = {
                                    viewModel.unshareDrawing(shared.id)
                                    Toast.makeText(context, "Drawing Unshared", Toast.LENGTH_SHORT).show()

                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Unshare",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    }

                    // Spacer so the red x to unshare does not get lost behind the + to make a new drawing
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }

                }
            }
        }

        Surface(
            color = Color(0xFFF5C518),
            shape = CircleShape,
            shadowElevation = 6.dp,
            modifier = Modifier
                .testTag("new_drawing_fab")
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp)
                .clickable {
                    viewModel.startNewDrawing()
                    navController.navigate("draw")
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Drawing",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = Color.Gray,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun CloudDrawingItem(cloud: CloudDrawing, onClick: () -> Unit = {}) {
    val context = LocalContext.current
    Surface(
        color = ToolbarGray,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 6.dp,
        modifier = Modifier
            .testTag("cloud_drawing_item_${cloud.id}")
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(15.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cloud.title.ifEmpty { "Untitled Drawing" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                        .format(cloud.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            // Coil fetches and caches the PNG from firebase storage
            AsyncImage(
                model = ImageRequest.Builder(context).data(cloud.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Cloud drawing preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(60.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp))
            )
        }
    }
}

@Composable
fun DrawingItem(drawing: Drawing, onClick: () -> Unit = {}) {
    Surface(
        color = ToolbarGray,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 6.dp,
        modifier = Modifier
            .testTag("drawing_item_${drawing.id}")
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(15.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = drawing.title.ifEmpty { "Untitled Drawing" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                        .format(drawing.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            val thumbnail = remember(drawing.filePath) {
                BitmapFactory.decodeFile(drawing.filePath)?.asImageBitmap()
            }
            if (thumbnail != null) {
                Spacer(modifier = Modifier.width(12.dp))
                Image(
                    bitmap = thumbnail,
                    contentDescription = "Preview",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
