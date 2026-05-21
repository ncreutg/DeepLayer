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

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    // Core photo parameter metrics
    var sensitivity by remember { mutableStateOf(260f) }
    var fgOffsetY by remember { mutableStateOf(0f) }
    var fgOffsetX by remember { mutableStateOf(0f) }

    var isPhotoModeActive by remember { mutableStateOf(true) }
    var showFullscreenPreview by remember { mutableStateOf(false) }

    // Read saved metrics from local profile configs
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

    // Live controller protecting sliders from active layout state conflicts
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            while (true) {
                val modeFile = File("/data/local/tmp/config_mode.txt")
                val activeMode = if (modeFile.exists()) modeFile.readText().trim() else "photo"
                val shouldBeActive = activeMode == "photo"
                if (isPhotoModeActive != shouldBeActive) isPhotoModeActive = shouldBeActive
                delay(300)
            }
        }
    }

    if (showFullscreenPreview) {
        PhotoPreviewScreen(
            bgUri = bgUri,
            fgUri = fgUri,
            onBackClicked = { showFullscreenPreview = false }
        )
    } else {
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

            // TOP STATUS BANNER COMPRISING THE IMMERSIVE 3D KALMAN PREVIEW ACTION BUTTON
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Photo Engine Monitor",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isPhotoModeActive) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isPhotoModeActive) "Parallax configuration ready" else "Photo configurations frozen",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(
                        onClick = { showFullscreenPreview = true },
                        enabled = isPhotoModeActive,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Launch Fullscreen Preview", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SLIDERS CONTROLLER FRAME
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

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
}
