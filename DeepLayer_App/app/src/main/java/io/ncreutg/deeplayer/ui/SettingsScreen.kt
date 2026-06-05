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

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.style.TextAlign
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onLanguageChanged: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .verticalScroll(rememberScrollState())
    ) {
        LargeTopAppBar(
            title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) },
            colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Transparent)
        )

        Spacer(modifier = Modifier.height(16.dp))

        val sharedPref = remember { context.getSharedPreferences("deeplayer_prefs", Context.MODE_PRIVATE) }
        val pm = remember { context.packageManager }

        var selectedLanguage by remember {
            val systemLang = java.util.Locale.getDefault().language
            val defaultLang = if (systemLang == "ru") "ru" else "en"
            mutableStateOf(sharedPref.getString("app_lang", defaultLang) ?: defaultLang)
        }

        var isMonetEnabled by remember {
            val componentName = ComponentName(context, "io.ncreutg.deeplayer.MainActivityMonet")
            val currentSetting = pm.getComponentEnabledSetting(componentName)
            mutableStateOf(currentSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || currentSetting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
        }

        val getLocalizedText = { id: Int, lang: String ->
            val locale = java.util.Locale(lang)
            val config = android.content.res.Configuration(context.resources.configuration).apply { setLocale(locale) }
            context.createConfigurationContext(config).resources.getString(id)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // Section 1: Application language initialization bounds
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = getLocalizedText(R.string.dialog_lang_header, selectedLanguage),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        RadioButton(
                            selected = selectedLanguage == "en",
                            onClick = {
                                selectedLanguage = "en"
                                sharedPref.edit().putString("app_lang", "en").apply()
                                onLanguageChanged()
                            }
                        )
                        Text("English", modifier = Modifier.clickable {
                            selectedLanguage = "en"
                            sharedPref.edit().putString("app_lang", "en").apply()
                            onLanguageChanged()
                        }.padding(start = 4.dp))

                        Spacer(modifier = Modifier.width(32.dp))

                        RadioButton(
                            selected = selectedLanguage == "ru",
                            onClick = {
                                selectedLanguage = "ru"
                                sharedPref.edit().putString("app_lang", "ru").apply()
                                onLanguageChanged()
                            }
                        )
                        Text("Русский", modifier = Modifier.clickable {
                            selectedLanguage = "ru"
                            sharedPref.edit().putString("app_lang", "ru").apply()
                            onLanguageChanged()
                        }.padding(start = 4.dp))
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // Section 2: Material You dynamic theme dynamic toggle engine
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = getLocalizedText(R.string.dialog_theme_header, selectedLanguage),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isMonetEnabled) getLocalizedText(R.string.theme_monet, selectedLanguage) else getLocalizedText(R.string.theme_custom, selectedLanguage),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isMonetEnabled,
                        onCheckedChange = { checked ->
                            isMonetEnabled = checked

                            val customAlias = ComponentName(context, "io.ncreutg.deeplayer.MainActivityCustom")
                            val monetAlias = ComponentName(context, "io.ncreutg.deeplayer.MainActivityMonet")

                            if (checked) {
                                pm.setComponentEnabledSetting(customAlias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                                pm.setComponentEnabledSetting(monetAlias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                            } else {
                                pm.setComponentEnabledSetting(monetAlias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
                                pm.setComponentEnabledSetting(customAlias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                val shortcutManager = context.getSystemService(android.content.pm.ShortcutManager::class.java)
                                if (shortcutManager != null) {
                                    try {
                                        val dummyShortcut = android.content.pm.ShortcutInfo.Builder(context, "icon_cache_refresh_node")
                                            .setShortLabel("DeepLayer")
                                            .setIcon(android.graphics.drawable.Icon.createWithResource(context, R.mipmap.ic_launcher_monet))
                                            .setIntent(Intent(context, context.javaClass).apply { action = Intent.ACTION_MAIN })
                                            .build()
                                        shortcutManager.dynamicShortcuts = listOf(dummyShortcut)
                                        shortcutManager.reportShortcutUsed("icon_cache_refresh_node")
                                        shortcutManager.removeDynamicShortcuts(listOf("icon_cache_refresh_node"))
                                    } catch (e: Exception) { e.printStackTrace() }
                                }
                            }

                            try {
                                val currentRes = context.resources
                                val currentConfig = currentRes.configuration
                                val savedFontScale = currentConfig.fontScale
                                currentConfig.fontScale = savedFontScale + 0.001f
                                currentRes.updateConfiguration(currentConfig, currentRes.displayMetrics)
                                currentConfig.fontScale = savedFontScale
                                currentRes.updateConfiguration(currentConfig, currentRes.displayMetrics)
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // Section 3: Open-source legal metadata configuration index
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DeepLayer v1.0.3",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Copyright © 2026 ncreutg",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Licensed under GNU GPL v3\n(View Source on GitHub)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clickable {
                                try {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com"))
                                    context.startActivity(browserIntent)
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                            .padding(vertical = 4.dp)
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
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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
