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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
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
import io.ncreutg.deeplayer.utils.LayerPickerCard
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
    bgUri: Uri?,
    fgUri: Uri?,
    onBgSelected: (Uri) -> Unit,
    onFgSelected: (Uri) -> Unit,
    onMenuPreferencesClicked: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var menuExpanded by remember { mutableStateOf(false) }

    val statusReady = stringResource(R.string.status_ready)
    val statusBgSuccess = stringResource(R.string.status_bg_success)
    val statusPageSuccess = stringResource(R.string.status_fg_success)
    val statusRootError = stringResource(R.string.status_root_error)

    var status by remember { mutableStateOf(statusReady) }
    var isProcessing by remember { mutableStateOf(false) }

    var isPhotoModeActive by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val modeFile = File("/data/local/tmp/config_mode.txt")
            if (modeFile.exists()) {
                isPhotoModeActive = modeFile.readText().trim() == "photo"
            }
        }
    }

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
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LargeTopAppBar(
            title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) },
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(stringResource(R.string.status_title), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(status, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                AnimatedVisibility(visible = isProcessing, enter = fadeIn(tween(300)), exit = fadeOut(tween(300))) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
                }
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
                Text("Enable 3D Photo Parallax", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = isPhotoModeActive,
                    onCheckedChange = { active ->
                        isPhotoModeActive = active
                        val targetMode = if (active) "photo" else "emoji"
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

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    try {
                        val process = Runtime.getRuntime().exec("su")
                        process.outputStream.use { os ->
                            os.write("pkill -f com.android.systemui\n".toByteArray())
                            os.flush()
                        }
                        process.waitFor()
                    } catch (e: Exception) { e.printStackTrace() }
                }
            },
            modifier = Modifier.fillMaxWidth().height(58.dp),
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
