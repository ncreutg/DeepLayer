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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPreviewScreen(
    bgUri: Uri?,
    fgUri: Uri?,
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current

    var currentSens by remember { mutableStateOf(260f) }
    var currentOffsetY by remember { mutableStateOf(0f) }
    var currentOffsetX by remember { mutableStateOf(0f) }

    // Переменные состояния для вывода отфильтрованных координат Калмана в UI
    var kalmanX by remember { mutableStateOf(0f) }
    var kalmanY by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val sensFile = File("/data/local/tmp/config_sensitivity.txt")
            val offsetYFile = File("/data/local/tmp/config_fg_offset.txt")
            val offsetXFile = File("/data/local/tmp/config_fg_offset_x.txt")

            if (sensFile.exists()) currentSens = sensFile.readText().trim().toFloatOrNull() ?: 260f
            if (offsetYFile.exists()) currentOffsetY = offsetYFile.readText().trim().toFloatOrNull() ?: 0f
            if (offsetXFile.exists()) currentOffsetX = offsetXFile.readText().trim().toFloatOrNull() ?: 0f
        }
    }

    DisposableEffect(currentSens) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            private var gravityX = 0f
            private var gravityY = 0f
            private val LIMIT = 16f

            // --- МАТЕМАТИКА КАЛМАНА ОДИН В ОДИН ИЗ ТВОЕГО ХУКА ---
            private var x_est = 0f
            private var p_x = 1f

            private var y_est = 0f
            private var p_y = 1f

            private val Q = 0.045f
            private val R_noise = 0.65f

            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

                // Прямой доступ к оконному менеджеру для детекции поворота экрана в Compose
                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    context.display?.rotation ?: Surface.ROTATION_0
                } else {
                    @Suppress("DEPRECATION") windowManager.defaultDisplay.rotation
                }

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

                val forceFactor = (currentSens / 350f) * 1.8f
                val targetX = (devX * forceFactor).coerceIn(-LIMIT, LIMIT)
                val targetY = (devY * forceFactor * 0.4f).coerceIn(-LIMIT, LIMIT)

                // --- KALMAN PROCESS RUNTIME (X-AXIS) ---
                p_x += Q
                val k_gain_x = p_x / (p_x + R_noise)
                x_est += k_gain_x * (targetX - x_est)
                p_x *= (1f - k_gain_x)

                // --- KALMAN PROCESS RUNTIME (Y-AXIS) ---
                p_y += Q
                val k_gain_y = p_y / (p_y + R_noise)
                y_est += k_gain_y * (targetY - y_est)
                p_y *= (1f - k_gain_y)

                // Передаем отфильтрованные оси в Compose UI поток
                kalmanX = x_est
                kalmanY = y_est
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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // ЗАДНИЙ ПЛАН: Полное пиксельное совпадение трансформаций по осям и масштабу
        if (bgUri != null) {
            AsyncImage(
                model = bgUri,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.55f // Твой точный системный масштаб из хука
                        scaleY = 1.55f
                        rotationY = kalmanX * 2.2f
                        rotationX = kalmanY * 2.2f
                        translationX = kalmanX * (currentSens / 100f)
                        translationY = -kalmanY * (currentSens / 100f)
                        cameraDistance = 1200f
                    },
                contentScale = ContentScale.Crop
            )
        }

        // ПЕРЕДНИЙ ПЛАН: Статичное позиционирование на холсте, вынесенное из датчиков
        if (fgUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        bottom = currentOffsetY.dp, // Чистое PX-смещение из твоей post-оптимизации
                        start = currentOffsetX.dp,
                        end = (-currentOffsetX).dp
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                AsyncImage(
                    model = fgUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.None // Отключаем внутренние фильтры Compose для точности Matrix
                )
            }
        }

        // Эмуляция системных часов поверх слоев
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("10:00", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Light, color = Color.White.copy(alpha = 0.95f))
            Text("Friday, May 15", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.75f))
        }

        // Навигационная кнопка выхода
        IconButton(
            onClick = onBackClicked,
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp)
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Close Preview", tint = Color.White)
        }
    }
}