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


package io.ncreutg.deeplayer.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import kotlinx.coroutines.withContext
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream

@Composable
fun LayerPickerCard(modifier: Modifier = Modifier, title: String, subtitle: String, imageUri: Uri?, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
            .border(width = 2.dp, color = if (imageUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), shape = RoundedCornerShape(28.dp))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (imageUri != null) {
            AsyncImage(model = imageUri, contentDescription = title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)), startY = 200f)))
        }

        Column(
            modifier = Modifier.padding(18.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.Start
        ) {
            if (imageUri == null) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp).align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp).weight(1f))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (imageUri != null) Color.White else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = if (imageUri != null) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

suspend fun saveValueConfig(fileName: String, value: String, context: Context) {
    try {
        val tempFile = File(context.cacheDir, fileName)
        tempFile.writeText(value)
        val publicPath = "/data/local/tmp/$fileName"
        val process = Runtime.getRuntime().exec("su")
        process.outputStream.use { os ->
            os.write("cp ${tempFile.absolutePath} $publicPath\n".toByteArray())
            os.write("chmod 777 $publicPath\n".toByteArray())
            os.flush()
        }
        process.waitFor()
        tempFile.delete()
    } catch (e: Exception) { e.printStackTrace() }
}

suspend fun saveLayer(context: Context, bitmap: Bitmap, fileName: String): Boolean = withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val tempFile = File(context.cacheDir, fileName)
        FileOutputStream(tempFile).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        val publicPath = "/data/local/tmp/$fileName"
        val process = Runtime.getRuntime().exec("su")
        process.outputStream.use { os ->
            os.write("cp ${tempFile.absolutePath} $publicPath\n".toByteArray())
            os.write("chmod 777 $publicPath\n".toByteArray())
            os.write("chown system:system $publicPath\n".toByteArray())
            os.write("chcon u:object_r:shell_data_file:s0 $publicPath\n".toByteArray())
            os.flush()
        }
        process.waitFor()
        tempFile.delete()
        true
    } catch (e: Exception) { false }
}

fun uriToBitmap(context: Context, uri: Uri): Bitmap {
    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { d, _, _ -> d.isMutableRequired = true }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
    return bitmap.copy(Bitmap.Config.ARGB_8888, true)
}
