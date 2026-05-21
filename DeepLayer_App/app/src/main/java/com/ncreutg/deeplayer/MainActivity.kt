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

package io.ncreutg.deeplayer

import android.content.ComponentName
import android.content.Context

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.ncreutg.deeplayer.ui.EmojiScreen
import io.ncreutg.deeplayer.ui.MainScreen
import io.ncreutg.deeplayer.ui.SettingsScreen
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var languageTrigger by mutableStateOf(0)

    override fun attachBaseContext(newBase: Context) {
        val sharedPref = newBase.getSharedPreferences("deeplayer_prefs", Context.MODE_PRIVATE)
        val systemLang = Locale.getDefault().language
        val defaultLang = if (systemLang == "ru") "ru" else "en"
        val lang = sharedPref.getString("app_lang", defaultLang) ?: defaultLang

        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration).apply { setLocale(locale) }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            @Suppress("UNUSED_VARIABLE") val trigger = languageTrigger
            val context = LocalContext.current

            val expressiveColorScheme = remember {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    dynamicDarkColorScheme(context)
                } else {
                    darkColorScheme()
                }
            }
            MaterialTheme(colorScheme = expressiveColorScheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation(onLanguageChanged = {
                        languageTrigger++
                        recreate()
                    })
                }
            }
        }
    }
}

@Composable
fun AppNavigation(onLanguageChanged: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    var bgUri by remember { mutableStateOf<Uri?>(null) }
    var fgUri by remember { mutableStateOf<Uri?>(null) }
    var stickerUri by remember { mutableStateOf<Uri?>(null) }
    var showPreferencesDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.tab_home)) },
                    label = { Text(stringResource(R.string.tab_home)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Face, contentDescription = stringResource(R.string.tab_emoji)) },
                    label = {
                        Text(
                            text = stringResource(R.string.tab_emoji),
                            textAlign = TextAlign.Center
                        )
                    }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.tab_settings)) },
                    label = { Text(stringResource(R.string.tab_settings)) }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> MainScreen(
                    bgUri = bgUri, fgUri = fgUri,
                    onBgSelected = { bgUri = it }, onFgSelected = { fgUri = it },
                    onMenuPreferencesClicked = { showPreferencesDialog = true }
                )
                1 -> EmojiScreen(
                    stickerUri = stickerUri,
                    onStickerSelected = { uri -> stickerUri = uri },
                    onMenuPreferencesClicked = { showPreferencesDialog = true }
                )
                2 -> SettingsScreen(
                    bgUri = bgUri, fgUri = fgUri,
                    onMenuPreferencesClicked = { showPreferencesDialog = true }
                )
            }
        }

        if (showPreferencesDialog) {
            PreferencesDialog(
                onDismiss = { showPreferencesDialog = false },
                onLanguageChanged = onLanguageChanged
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesDialog(onDismiss: () -> Unit, onLanguageChanged: () -> Unit) {
    val context = LocalContext.current
    val sharedPref = remember { context.getSharedPreferences("deeplayer_prefs", Context.MODE_PRIVATE) }
    val pm = remember { context.packageManager }

    var selectedLanguage by remember {
        val systemLang = Locale.getDefault().language
        val defaultLang = if (systemLang == "ru") "ru" else "en"
        mutableStateOf(sharedPref.getString("app_lang", defaultLang) ?: defaultLang)
    }

    // FIX: Properly handle COMPONENT_ENABLED_STATE_DEFAULT to reflect manifest configuration initialization bounds
    var isMonetEnabled by remember {
        val componentName = ComponentName(context, "io.ncreutg.deeplayer.MainActivityMonet")
        val currentSetting = pm.getComponentEnabledSetting(componentName)

        val isEnabled = if (currentSetting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
            true
        } else {
            currentSetting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }

        mutableStateOf(isEnabled)
    }

    val getLocalizedText = { id: Int, lang: String ->
        val locale = Locale(lang)
        val config = Configuration(context.resources.configuration).apply { setLocale(locale) }
        context.createConfigurationContext(config).resources.getString(id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = getLocalizedText(R.string.dialog_title, selectedLanguage),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = getLocalizedText(R.string.dialog_lang_header, selectedLanguage),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        RadioButton(selected = selectedLanguage == "en", onClick = { selectedLanguage = "en" })
                        Text("English", modifier = Modifier.clickable { selectedLanguage = "en" }.padding(start = 4.dp))
                        Spacer(modifier = Modifier.width(24.dp))
                        RadioButton(selected = selectedLanguage == "ru", onClick = { selectedLanguage = "ru" })
                        Text("Русский", modifier = Modifier.clickable { selectedLanguage = "ru" }.padding(start = 4.dp))
                    }
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(getLocalizedText(R.string.dialog_theme_header, selectedLanguage), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = if (isMonetEnabled) getLocalizedText(R.string.theme_monet, selectedLanguage) else getLocalizedText(R.string.theme_custom, selectedLanguage),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isMonetEnabled,
                        onCheckedChange = { isMonetEnabled = it }
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // BLOCK: Open Source Licensing, Copyright & GitHub Link Component Matrix
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DeepLayer v1.0.0",
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

                    // FIX: Turned static text into an interactive system browser redirection trigger link
                    Text(
                        text = "Licensed under GNU GPL v3\n(View Source on GitHub)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clickable {
                                try {
                                    val browserIntent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://github.com")
                                    )
                                    context.startActivity(browserIntent)
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                sharedPref.edit().putString("app_lang", selectedLanguage).apply()

                val customAlias = ComponentName(context, "io.ncreutg.deeplayer.MainActivityCustom")
                val monetAlias = ComponentName(context, "io.ncreutg.deeplayer.MainActivityMonet")

                if (isMonetEnabled) {
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
                                .setIntent(android.content.Intent(context, MainActivity::class.java).apply { action = android.content.Intent.ACTION_MAIN })
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

                onDismiss()
                onLanguageChanged()
            }) {
                Text(getLocalizedText(R.string.dialog_btn_save, selectedLanguage), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(getLocalizedText(R.string.dialog_btn_cancel, selectedLanguage))
            }
        }
    )
}
