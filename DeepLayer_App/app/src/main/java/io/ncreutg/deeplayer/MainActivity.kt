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

import io.ncreutg.deeplayer.ui.RootCheckScreen
import io.ncreutg.deeplayer.utils.RootManager
import android.content.Context
import android.content.res.Configuration
import kotlinx.coroutines.withContext
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    // 1. Ставим дефолтное значение true, чтобы приложение не фризило, пока идет проверка
    var isRootGranted by remember { mutableStateOf(true) }

    // 2. Асинхронно проверяем рут в фоновом потоке сразу при старте
    LaunchedEffect(Unit) {
        val hasRoot = withContext(kotlinx.coroutines.Dispatchers.IO) {
            RootManager.isRootAvailable()
        }
        isRootGranted = hasRoot
    }

    var selectedTab by remember { mutableStateOf(0) }
    var bgUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var fgUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var stickerUri by remember { mutableStateOf<android.net.Uri?>(null) }

    if (isRootGranted) {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = stringResource(R.string.tab_home)
                            )
                        },
                        label = { Text(stringResource(R.string.tab_home)) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                Icons.Default.Face,
                                contentDescription = stringResource(R.string.tab_emoji)
                            )
                        },
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
                        icon = {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = stringResource(R.string.tab_settings)
                            )
                        },
                        label = { Text(stringResource(R.string.tab_settings)) }
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (selectedTab) {
                    0 -> MainScreen(
                        bgUri = bgUri, fgUri = fgUri,
                        onBgSelected = { bgUri = it }, onFgSelected = { fgUri = it }
                    )

                    1 -> EmojiScreen(
                        stickerUri = stickerUri,
                        onStickerSelected = { uri -> stickerUri = uri }
                    )

                    2 -> SettingsScreen(
                        onLanguageChanged = onLanguageChanged
                    )
                }
            }
        }
    } else {
        RootCheckScreen(onRootGranted = { isRootGranted = true })
    }
}
