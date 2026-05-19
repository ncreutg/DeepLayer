/*
 * Copyright (C) 2026 ncreutg
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */


package io.ncreutg.deeplayer.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import io.ncreutg.deeplayer.R
import io.ncreutg.deeplayer.utils.saveValueConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(bgUri: Uri?, fgUri: Uri?, onMenuPreferencesClicked: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }

    // Core hardware tracking and offset parameters
    var sensitivity by remember { mutableStateOf(260f) }
    var fgOffsetY by remember { mutableStateOf(0f) }
    var fgOffsetX by remember { mutableStateOf(0f) }

    // Live parallax preview bounds synchronized via local motion architecture
    var liveX by remember { mutableStateOf(0f) }
    var liveY by remember { mutableStateOf(0f) }

    // State parameters for real-time background mode polling configuration
    var isPhotoModeActive by remember { mutableStateOf(true) }
    var emojiBgColor by remember { mutableStateOf("#121214") }
    var emojiSet by remember { mutableStateOf("👾🔥🚀") }
    var useStickerImage by remember { mutableStateOf(false) }

    // Initialize state metrics from secure localized file configurations
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val sensFile = File("/data/local/tmp/config_sensitivity.txt")
            val offsetYFile = File("/data/local/tmp/config_fg_offset.txt")
            val offsetXFile = File("/data/local/tmp/config_fg_offset_x.txt")

            if (sensFile.exists()) sensitivity = sensFile.readText().trim().toFloatOrNull() ?: 260f
            if (offsetYFile.exists()) fgOffsetY = offsetYFile.readText().trim().toFloatOrNull() ?: 0f
            if (offsetXFile.exists()) fgOffsetX = offsetXFile.readText().trim().toFloatOrNull() ?: 0f
        }
    }

    // Dynamic background continuous module loop listener preventing rendering conflicts
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            while (true) {
                val modeFile = File("/data/local/tmp/config_mode.txt")
                val activeMode = if (modeFile.exists()) modeFile.readText().trim() else "photo"
                val shouldBeActive = activeMode == "photo"

                if (isPhotoModeActive != shouldBeActive) isPhotoModeActive = shouldBeActive

                val bgFile = File("/data/local/tmp/config_emoji_bg.txt")
                if (bgFile.exists()) emojiBgColor = bgFile.readText().trim()

                val emojiFile = File("/data/local/tmp/config_emoji_set.txt")
                if (emojiFile.exists()) emojiSet = emojiFile.readText().trim()

                val stickerToggleFile = File("/data/local/tmp/config_emoji_use_sticker.txt")
                if (stickerToggleFile.exists()) useStickerImage = stickerToggleFile.readText().trim() == "true"

                delay(300)
            }
        }
    }

    // Process local device movement constraints directly inside preferences UI sandbox container
    DisposableEffect(sensitivity) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            private var gravityX = 0f
            private var gravityY = 0f
            private var curX = 0f
            private var curY = 0f
            private val LIMIT = 16f

            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context.display?.rotation ?: Surface.ROTATION_0
                } else {
                    @Suppress("DEPRECATION") windowManager.defaultDisplay.rotation
                }

                // Extract index coordinates safely from current layout bounds array elements
                var rawX = event.values[0]
                var rawY = event.values[1]

                when (rotation) {
                    Surface.ROTATION_90 -> { val temp = rawX; rawX = -rawY; rawY = temp }
                    Surface.ROTATION_270 -> { val temp = rawX; rawX = rawY; rawY = -temp }
                    Surface.ROTATION_180 -> { rawX = -rawX; rawY = -rawY }
                }

                gravityX = 0.90f * gravityX + 0.10f * rawX
                gravityY = 0.90f * gravityY + 0.10f * rawY

                val devX = rawX - gravityX
                val devY = rawY - gravityY

                val forceFactor = (sensitivity / 350f) * 1.8f
                val lerp = 0.18f

                val targetX = (devX * forceFactor).coerceIn(-LIMIT, LIMIT)
                val targetY = (devY * forceFactor * 0.4f).coerceIn(-LIMIT, LIMIT)

                curX += (targetX - curX) * lerp
                curY += (targetY - curY) * lerp

                liveX = curX
                liveY = curY
            }
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
        }

        if (accel != null) {
            sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose {
            sm.unregisterListener(listener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        LargeTopAppBar(
            title = { Text(stringResource(R.string.tab_settings), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) },
            actions = {
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu Options")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dialog_title)) },
                            onClick = {
                                menuExpanded = false
                                onMenuPreferencesClicked()
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Transparent)
        )

        Text(
            text = stringResource(R.string.preview_header),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Standard 3D Photo Parallax Simulation Sandbox Container Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.9f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (isPhotoModeActive) {
                // 1. CHOSEN STANDARD PHOTO MODE RENDERING BOUNDS
                if (bgUri != null) {
                    AsyncImage(
                        model = bgUri,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 1.35f
                                scaleY = 1.35f
                                translationX = liveX * (sensitivity / 25f)
                                translationY = -liveY * (sensitivity / 25f)
                                rotationY = liveX * 2.2f
                                rotationX = liveY * 2.2f
                                cameraDistance = 1200f
                            },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(stringResource(R.string.preview_empty_bg), color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }

                if (fgUri != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                bottom = (fgOffsetY / 12f).dp,
                                start = (fgOffsetX / 12f).dp,
                                end = (-fgOffsetX / 12f).dp
                            ),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        AsyncImage(model = fgUri, contentDescription = null, modifier = Modifier.fillMaxHeight(0.7f), contentScale = ContentScale.Fit)
                    }
                }
            } else {
                // 2. DYNAMIC LIVE EMOJI & STICKER COLLAGE SANDBOX PREVIEW GRID LAYER
                val parsedBgColor = remember(emojiBgColor) {
                    try { Color(android.graphics.Color.parseColor(emojiBgColor)) } catch (e: Exception) { Color(0xFF121214) }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(parsedBgColor)
                        .graphicsLayer {
                            translationX = liveX * (sensitivity / 25f)
                            translationY = -liveY * (sensitivity / 25f)
                        }
                ) {
                    val targetCellsCount = 10
                    val parsedEmojisList = mutableListOf<String>()
                    var charIdx = 0
                    while (charIdx < emojiSet.length) {
                        val cp = emojiSet.codePointAt(charIdx)
                        parsedEmojisList.add(String(Character.toChars(cp)))
                        charIdx += Character.charCount(cp)
                    }

                    val previewPool = mutableListOf<Any>()
                    var assetIndex = 0
                    while (previewPool.size < targetCellsCount) {
                        if (useStickerImage && bgUri != null) {
                            previewPool.add(bgUri)
                        } else if (parsedEmojisList.isNotEmpty()) {
                            previewPool.add(parsedEmojisList[assetIndex % parsedEmojisList.size])
                        } else {
                            previewPool.add("👾")
                        }
                        assetIndex++
                    }
                    previewPool.shuffle(kotlin.random.Random(42))

                    Column(modifier = Modifier.fillMaxSize()) {
                        for (row in 0 until 4) {
                            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                for (col in 0 until 3) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                                        val flatCellIndex = (row * 3) + col
                                        if (flatCellIndex < targetCellsCount && flatCellIndex < previewPool.size) {
                                            val element = previewPool[flatCellIndex]
                                            if (element is Uri) {
                                                AsyncImage(model = element, contentDescription = null, modifier = Modifier.size(44.dp), contentScale = ContentScale.Fit)
                                            } else if (element is String) {
                                                Text(text = element, fontSize = 32.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("10:00", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Light, color = Color.White.copy(alpha = 0.9f))
                Text("Friday, May 15", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Hardware tracking sensitivity and dimensional offset cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Sensitivity hardware mapping slider control
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.slider_sensitivity), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (isPhotoModeActive) Color.Unspecified else Color.Gray)
                        Text("${sensitivity.toInt()}", style = MaterialTheme.typography.bodyMedium, color = if (isPhotoModeActive) MaterialTheme.colorScheme.primary else Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = sensitivity,
                        enabled = isPhotoModeActive,
                        onValueChange = {
                            sensitivity = it
                            scope.launch(Dispatchers.IO) { saveValueConfig("config_sensitivity.txt", it.toString(), context) }
                        },
                        valueRange = 50f..500f
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // Vertical spatial axis translation matrix control slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.slider_offset_y), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (isPhotoModeActive) Color.Unspecified else Color.Gray)
                        Text("${fgOffsetY.toInt()} ${stringResource(R.string.unit_px)}", style = MaterialTheme.typography.bodyMedium, color = if (isPhotoModeActive) MaterialTheme.colorScheme.primary else Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = fgOffsetY,
                        enabled = isPhotoModeActive,
                        onValueChange = {
                            fgOffsetY = it
                            scope.launch(Dispatchers.IO) { saveValueConfig("config_fg_offset.txt", it.toString(), context) }
                        },
                        valueRange = 0f..600f
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // Horizontal alignment axis correction slider control interface
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.slider_offset_x), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (isPhotoModeActive) Color.Unspecified else Color.Gray)
                        Text("${fgOffsetX.toInt()} ${stringResource(R.string.unit_px)}", style = MaterialTheme.typography.bodyMedium, color = if (isPhotoModeActive) MaterialTheme.colorScheme.primary else Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = fgOffsetX,
                        enabled = isPhotoModeActive,
                        onValueChange = {
                            fgOffsetX = it
                            scope.launch(Dispatchers.IO) { saveValueConfig("config_fg_offset_x.txt", it.toString(), context) }
                        },
                        valueRange = -300f..300f
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Complete android core layout restart execution controller pipeline
        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    try {
                        val process = Runtime.getRuntime().exec("su")
                        process.outputStream.use { os ->
                            os.write("setprop ctl.restart zygote\n".toByteArray())
                            os.flush()
                        }
                        process.waitFor()
                    } catch (e: Exception) { e.printStackTrace() }
                }
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(28.dp),
            enabled = isPhotoModeActive,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.btn_zygote), fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
