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


package io.ncreutg.deeplayer.lsposed

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import java.io.File

class SystemHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        if (lpparam.packageName != "com.android.systemui") return

        XposedHelpers.findAndHookMethod(
            "android.view.View",
            lpparam.classLoader,
            "onAttachedToWindow",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val view = param.thisObject as View
                    if (!view.javaClass.name.contains("ScrimView")) return

                    val container = view as ViewGroup
                    if (container.findViewWithTag<View>("deeplayer_root_layer") != null) return

                    val context = container.context

                    val modeFile = File("/data/local/tmp/config_mode.txt")
                    val currentMode = if (modeFile.exists()) modeFile.readText().trim() else "photo"
                    if (currentMode != "photo") return

                    val rootLayer = FrameLayout(context).apply {
                        tag = "deeplayer_root_layer"
                        layoutParams = ViewGroup.LayoutParams(-1, -1)
                        translationZ = -50f
                    }

                    val bgView = createLayer(context, "background.png", "layer_bg", true)
                    val fgView = createLayer(context, "foreground.png", "layer_fg", false)

                    rootLayer.addView(bgView)
                    rootLayer.addView(fgView)
                    container.addView(rootLayer, 0)

                    val density = context.resources.displayMetrics.density
                    bgView.cameraDistance = density * 1200f

                    val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                    val accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return

                    val sensFile = File("/data/local/tmp/config_sensitivity.txt")
                    val offsetYFile = File("/data/local/tmp/config_fg_offset.txt")
                    val offsetXFile = File("/data/local/tmp/config_fg_offset_x.txt")

                    val currentSens = if (sensFile.exists()) sensFile.readText().trim().toFloatOrNull() ?: 260f else 260f
                    val currentOffsetY = if (offsetYFile.exists()) offsetYFile.readText().trim().toFloatOrNull() ?: 0f else 0f
                    val currentOffsetX = if (offsetXFile.exists()) offsetXFile.readText().trim().toFloatOrNull() ?: 0f else 0f

                    val listener = object : SensorEventListener {
                        private var gravityX = 0f
                        private var gravityY = 0f
                        private val LIMIT = 16f

                        // --- KALMAN FILTER CORE VARIABLES PROFILE ---
                        private var x_est = 0f
                        private var p_x = 1f

                        private var y_est = 0f
                        private var p_y = 1f

                        private val Q = 0.045f
                        private val R_noise = 0.65f

                        private val reusableMatrix = Matrix()

                        override fun onSensorChanged(event: SensorEvent) {
                            if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

                            val display = view.display
                            val rotation = display?.rotation ?: Surface.ROTATION_0

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

                            bgView.rotationY = x_est * 2.2f
                            bgView.rotationX = y_est * 2.2f
                            bgView.translationX = x_est * (currentSens / 100f)
                            bgView.translationY = -y_est * (currentSens / 100f)
                        }

                        override fun onAccuracyChanged(s: Sensor?, a: Int) {}
                    }

                    sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)

                    fgView.post {
                        val fgDrawable = fgView.drawable
                        if (fgDrawable != null) {
                            val displayMetrics = context.resources.displayMetrics
                            val staticMatrix = Matrix()
                            val dx = ((displayMetrics.widthPixels.toFloat() - fgDrawable.intrinsicWidth) / 2f) + currentOffsetX
                            val dy = displayMetrics.heightPixels.toFloat() - fgDrawable.intrinsicHeight - currentOffsetY
                            staticMatrix.postTranslate(dx, dy)
                            fgView.imageMatrix = staticMatrix
                        }
                    }

                    sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)

                    XposedHelpers.findAndHookMethod(
                        "com.android.systemui.statusbar.phone.NotificationShadeWindowView",
                        lpparam.classLoader,
                        "onDetachedFromWindow",
                        object : XC_MethodHook() {
                            override fun afterHookedMethod(param: MethodHookParam) {
                                if (param.thisObject === view) {
                                    sm.unregisterListener(listener)
                                }
                            }
                        }
                    )
                }
            }
        )
    }

    private fun createLayer(ctx: Context, name: String, tag: String, isBackground: Boolean): ImageView {
        return ImageView(ctx).apply {
            this.tag = tag
            layoutParams = ViewGroup.LayoutParams(-1, -1)
            if (isBackground) {
                scaleType = ImageView.ScaleType.CENTER_CROP
                scaleX = 1.55f
                scaleY = 1.55f
            } else {
                scaleType = ImageView.ScaleType.MATRIX
            }

            val file = File("/data/local/tmp/$name")
            if (file.exists() && file.canRead()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                setImageBitmap(bitmap)
            }
        }
    }
}
