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

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.net.Uri
import androidx.compose.foundation.background
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.ncreutg.deeplayer.R
import io.ncreutg.deeplayer.utils.saveLayer
import io.ncreutg.deeplayer.utils.saveValueConfig
import io.ncreutg.deeplayer.utils.uriToBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    bgUri: android.net.Uri?,
    fgUri: android.net.Uri?,
    onBgSelected: (android.net.Uri) -> Unit,
    onFgSelected: (android.net.Uri) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }

    var sensitivity by remember { mutableStateOf(260f) }
    var fgOffsetY by remember { mutableStateOf(0f) }
    var fgOffsetX by remember { mutableStateOf(0f) }

    val statusReady = stringResource(R.string.status_ready)
    val statusBgSuccess = stringResource(R.string.status_bg_success)
    val statusPageSuccess = stringResource(R.string.status_fg_success)
    val statusRootError = stringResource(R.string.status_root_error)

    var status by remember { mutableStateOf(statusReady) }
    var isProcessing by remember { mutableStateOf(false) }
    var isPhotoModeActive by remember { mutableStateOf(true) }

    var showFullscreenPreview by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val modeFile = File("/data/local/tmp/config_mode.txt")
            val sensFile = File("/data/local/tmp/config_sensitivity.txt")
            val offsetYFile = File("/data/local/tmp/config_fg_offset.txt")
            val offsetXFile = File("/data/local/tmp/config_fg_offset_x.txt")

            if (modeFile.exists()) {
                isPhotoModeActive = modeFile.readText().trim() == "photo"
            }
            if (sensFile.exists()) sensitivity = sensFile.readText().trim().toFloatOrNull() ?: 260f
            if (offsetYFile.exists()) fgOffsetY = offsetYFile.readText().trim().toFloatOrNull() ?: 0f
            if (offsetXFile.exists()) fgOffsetX = offsetXFile.readText().trim().toFloatOrNull() ?: 0f
        }
    }

    if (showFullscreenPreview) {
        PhotoPreviewScreen(
            bgUri = bgUri,
            fgUri = fgUri,
            onBackClicked = { showFullscreenPreview = false }
        )
    } else {
        val bgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                onBgSelected(it)
                isProcessing = true
                status = context.getString(R.string.status_bg_import)
                scope.launch {
                    val bitmap = uriToBitmap(context, uri)
                    val saved = saveLayer(context, bitmap, "background.png")
                    status = if (saved) statusBgSuccess else statusRootError
                    isProcessing = false
                }
            }
        }

        val fgLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                onFgSelected(it)
                isProcessing = true
                status = context.getString(R.string.status_fg_import)
                scope.launch {
                    val bitmap = uriToBitmap(context, uri)
                    val saved = saveLayer(context, bitmap, "foreground.png")
                    status = if (saved) statusPageSuccess else statusRootError
                    isProcessing = false
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) },
                colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Transparent)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.status_title), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(status, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    }

                    IconButton(
                        onClick = { showFullscreenPreview = true },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "View Live Preview", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                AnimatedVisibility(visible = isProcessing, enter = fadeIn(tween(300)), exit = fadeOut(tween(300))) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp).clip(RoundedCornerShape(4.dp)))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = stringResource(R.string.enable_Parallax), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = isPhotoModeActive,
                        onCheckedChange = { active ->
                            isPhotoModeActive = active
                            // If disabled, we fall back to 'none' instead of forcing emoji mode
                            val targetMode = if (active) "photo" else "none"
                            scope.launch(Dispatchers.IO) {
                                saveValueConfig("config_mode.txt", targetMode, context)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LayerPickerCard(modifier = Modifier.weight(1f), title = stringResource(R.string.layer_background), subtitle = stringResource(R.string.layer_bg_sub), imageUri = bgUri, enabled = !isProcessing && isPhotoModeActive, onClick = { bgLauncher.launch("image/*") })
                LayerPickerCard(modifier = Modifier.weight(1f), title = stringResource(R.string.layer_foreground), subtitle = stringResource(R.string.layer_fg_sub), imageUri = fgUri, enabled = !isProcessing && isPhotoModeActive, onClick = { fgLauncher.launch("image/*") })
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp) // Чёткий шаг между блоками
                ) {
                    Text(
                        text = stringResource(R.string.settings_parallax),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPhotoModeActive) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // 1. БЛОК: Чувствительность фона
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.slider_sensitivity), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (isPhotoModeActive) Color.Unspecified else Color.Gray)
                            Text("${sensitivity.toInt()}", style = MaterialTheme.typography.bodyMedium, color = if (isPhotoModeActive) MaterialTheme.colorScheme.primary else Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = sensitivity,
                            enabled = isPhotoModeActive,
                            onValueChange = { sensitivity = it },
                            onValueChangeFinished = {
                                scope.launch(Dispatchers.IO) { saveValueConfig("config_sensitivity.txt", sensitivity.toString(), context) }
                            },
                            valueRange = 50f..500f
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.slider_offset_y), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (isPhotoModeActive) Color.Unspecified else Color.Gray)
                            Text("${fgOffsetY.toInt()} ${stringResource(R.string.unit_px)}", style = MaterialTheme.typography.bodyMedium, color = if (isPhotoModeActive) MaterialTheme.colorScheme.primary else Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = fgOffsetY,
                            enabled = isPhotoModeActive,
                            onValueChange = { fgOffsetY = it },
                            onValueChangeFinished = {
                                scope.launch(Dispatchers.IO) { saveValueConfig("config_fg_offset.txt", fgOffsetY.toString(), context) }
                            },
                            valueRange = 0f..600f
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                    // 3. БЛОК: Положение по горизонтали (X)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.slider_offset_x), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (isPhotoModeActive) Color.Unspecified else Color.Gray)
                            Text("${fgOffsetX.toInt()} ${stringResource(R.string.unit_px)}", style = MaterialTheme.typography.bodyMedium, color = if (isPhotoModeActive) MaterialTheme.colorScheme.primary else Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = fgOffsetX,
                            enabled = isPhotoModeActive,
                            onValueChange = { fgOffsetX = it },
                            onValueChangeFinished = {
                                scope.launch(Dispatchers.IO) { saveValueConfig("config_fg_offset_x.txt", fgOffsetX.toString(), context) }
                            },
                            valueRange = -300f..300f
                        )
                    }
                }
            }

            // FIX: Fill parameter is set to false. This allows the spacer to collapse completely
            // if screen height constraints are tight, preventing the button below from crunching.
            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val process = Runtime.getRuntime().exec("su")
                            process.outputStream.use { os ->
                                os.write("chmod 666 /data/local/tmp/background.png\n".toByteArray())
                                os.write("chmod 666 /data/local/tmp/foreground.png\n".toByteArray())
                                os.write("pkill -f com.android.systemui\n".toByteArray())
                                os.flush()
                            }
                            process.waitFor()
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .navigationBarsPadding(),
                shape = RoundedCornerShape(28.dp),
                enabled = isPhotoModeActive
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_apply), fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayerPickerCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    imageUri: Uri?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp) else MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp).copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(24.dp),
        onClick = { if (enabled) onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) Color.Unspecified else Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    Text(
                        text = stringResource(R.string.img_selected),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = stringResource(R.string.empty),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
