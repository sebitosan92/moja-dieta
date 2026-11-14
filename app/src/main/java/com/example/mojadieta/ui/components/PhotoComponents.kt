package com.example.mojadieta.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.example.mojadieta.data.images.ImageStorage
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/** Proporcje zdjęć w aplikacji. */
object PhotoAspect {
    const val RECIPE = 4f / 3f
    const val MEAL = 1f
}

/** Zdjęcie z pliku w pamięci aplikacji (przycięte). */
@Composable
fun StoredPhoto(path: String, modifier: Modifier = Modifier, contentDescription: String? = null) {
    AsyncImage(
        model = File(path),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

/**
 * Pole zdjęcia: podgląd + „Z galerii” / „Aparat” / „Usuń”.
 * Po wybraniu zdjęcia otwiera kadrowanie i zwraca ścieżkę zapisanego pliku.
 */
@Composable
fun PhotoField(
    imagePath: String?,
    aspect: Float,
    imageStorage: ImageStorage,
    onImageChanged: (String?) -> Unit,
    modifier: Modifier = Modifier,
    /** Szerokość podglądu względem pola (przyciski zawsze na całą szerokość). */
    imageWidthFraction: Float = 1f
) {
    val context = LocalContext.current
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pendingUri = uri
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraUri
        if (success && uri != null) pendingUri = Uri.parse(uri)
    }
    fun launchCamera() {
        val file = imageStorage.newCameraFile()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraUri = uri.toString()
        cameraLauncher.launch(uri)
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth(imageWidthFraction)
                .aspectRatio(aspect)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (imagePath != null) {
                StoredPhoto(imagePath, Modifier.fillMaxSize(), contentDescription = "Zdjęcie")
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.AddPhotoAlternate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        "Dodaj zdjęcie lub zrzut ekranu",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Galeria")
            }
            OutlinedButton(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        launchCamera()
                    } else {
                        cameraPermission.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Aparat")
            }
            if (imagePath != null) {
                OutlinedButton(onClick = { onImageChanged(null) }) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Usuń zdjęcie", modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    pendingUri?.let { uri ->
        PhotoCropDialog(
            uri = uri,
            aspect = aspect,
            imageStorage = imageStorage,
            onDismiss = { pendingUri = null },
            onSaved = { path ->
                pendingUri = null
                onImageChanged(path)
            }
        )
    }
}

/**
 * Kadrowanie: zdjęcie wypełnia ramkę o zadanych proporcjach, użytkownik przesuwa
 * i powiększa je palcami. Zapisywany jest tylko widoczny fragment.
 */
@Composable
fun PhotoCropDialog(
    uri: Uri,
    aspect: Float,
    imageStorage: ImageStorage,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(uri) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    LaunchedEffect(uri) {
        val loaded = imageStorage.loadBitmap(uri)
        if (loaded == null) failed = true else bitmap = loaded
    }

    var frame by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember(uri) { mutableFloatStateOf(1f) }
    var offset by remember(uri) { mutableStateOf(Offset.Zero) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .systemBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Dopasuj kadr",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Przesuń zdjęcie palcem, a dwoma palcami je powiększ.",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspect)
                    .clip(MaterialTheme.shapes.medium)
                    .border(2.dp, Color.White, MaterialTheme.shapes.medium)
                    .onSizeChanged { frame = it },
                contentAlignment = Alignment.Center
            ) {
                val bmp = bitmap
                when {
                    failed -> Text("Nie udało się wczytać zdjęcia.", color = Color.White)
                    bmp == null || frame == IntSize.Zero -> CircularProgressIndicator(color = Color.White)
                    else -> {
                        val image = remember(bmp) { bmp.asImageBitmap() }
                        val baseScale = max(frame.width / bmp.width.toFloat(), frame.height / bmp.height.toFloat())
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(bmp, frame) {
                                    detectTransformGestures { _, pan, gestureZoom, _ ->
                                        zoom = (zoom * gestureZoom).coerceIn(1f, 5f)
                                        offset = clampOffset(offset + pan, bmp, frame, baseScale * zoom)
                                    }
                                }
                        ) {
                            val scale = baseScale * zoom
                            val drawnW = bmp.width * scale
                            val drawnH = bmp.height * scale
                            val clamped = clampOffset(offset, bmp, frame, scale)
                            val left = frame.width / 2f + clamped.x - drawnW / 2f
                            val top = frame.height / 2f + clamped.y - drawnH / 2f
                            drawImage(
                                image = image,
                                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                                dstSize = IntSize(drawnW.roundToInt(), drawnH.roundToInt())
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Anuluj", color = Color.White)
                }
                Button(
                    enabled = bitmap != null && !saving && frame != IntSize.Zero,
                    onClick = {
                        val bmp = bitmap ?: return@Button
                        saving = true
                        val scale = max(frame.width / bmp.width.toFloat(), frame.height / bmp.height.toFloat()) * zoom
                        val clamped = clampOffset(offset, bmp, frame, scale)
                        val left = frame.width / 2f + clamped.x - bmp.width * scale / 2f
                        val top = frame.height / 2f + clamped.y - bmp.height * scale / 2f
                        val cropLeft = (-left / scale).roundToInt()
                        val cropTop = (-top / scale).roundToInt()
                        val cropWidth = (frame.width / scale).roundToInt()
                        val cropHeight = (frame.height / scale).roundToInt()
                        scope.launch {
                            val path = imageStorage.saveCropped(bmp, cropLeft, cropTop, cropWidth, cropHeight)
                            onSaved(path)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (saving) "Zapisywanie…" else "Zapisz")
                }
            }
        }
    }
}

/** Ogranicza przesunięcie tak, żeby zdjęcie zawsze wypełniało całą ramkę. */
private fun clampOffset(offset: Offset, bmp: Bitmap, frame: IntSize, scale: Float): Offset {
    val maxX = ((bmp.width * scale - frame.width) / 2f).coerceAtLeast(0f)
    val maxY = ((bmp.height * scale - frame.height) / 2f).coerceAtLeast(0f)
    return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
}
