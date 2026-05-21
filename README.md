# DeepLayer — Custom Emoji & Sticker Lock Screen for AOSP 14

DeepLayer is an advanced, system-level customization module built for clean AOSP 14. It allows users to design completely custom lock screen backgrounds using full-resolution rich content (stickers, GIFs from Gboard, or standard text emojis), mimicking the aesthetic of modern operating systems but with much deeper structural flexibility.

Unlike basic third-party live wallpaper engines, **DeepLayer operates directly within the system pipeline without modifying your primary launcher wallpaper**.

---

## ⚡ Core Features

- **Standalone Parallax Engine:** An independent, toggleable feature that uses real-time hardware sensor fusion data (Gyroscope/Accelerometer) to dynamically shift rendering coordinates, creating an immersive, multi-layered depth effect when moving the device. Works on top of any layout.
- **Rich Content Support:** Native injection through Android's `OnReceiveContentListener`, processing Gboard stickers (PNG/WebP) and surrogate-pair text emojis flawlessly.
- **SystemUI Injection:** Hooks directly into `NotificationShadeWindowView` via LSPosed, injecting a low-overhead native rendering plane behind the lock screen notifications.
- **No Performance Drop:** All background images are read asynchronously and cached in a background thread to prevent `SystemUI` micro-stutters and main thread blockage.
- **8 Dynamic Geometric Layouts:**
    - `dense_grid` – Full-screen ultra-dense mosaic tiling (auto-populates empty space).
    - `mosaic` – Elegant staggered asymmetric rows inspired by modern mobile lock screens.
    - `matrix` – Immersive 3D perspective layout featuring 3 distinct planes of depth and alpha-transparency.
    - `chaos` – A random scatter collage featuring dynamic element rotation matrix limits.
    - `grid` – Standard clean structural viewport layout.
    - `spiral` – Orbital positioning rotating around a single centered core item.
    - `diamond` – Futuristic diamond geometric nodes distribution.
    - `wave` – Symmetrical sine-wave patterns flowing down the viewport without overlap.

---

## 🛠️ Architecture Stack

- **Framework:** Jetpack Compose (Material Design 3 frontend config UI)
- **Runtime:** LSPosed Framework (Dynamic Java/Kotlin method hooking)
- **Sensors Pipeline:** Android `SensorEventListener` (Low-latency Gyroscope matrix processing)
- **Graphics Pipeline:** Low-level Android `Canvas` & `Paint` rendering context matrix
- **IPC / Config Data Transfer:** Fast `/data/local/tmp` secure configuration bridging

---

## 🚀 Installation & Setup

1. Make sure your device is rooted via Magisk/KernelSU and the **LSPosed** framework is active.
2. Build and install the DeepLayer APK.
3. Open the **LSPosed Manager**, enable the DeepLayer module, and ensure `System UI` is checked in the scope.
4. Open the DeepLayer config app, enable the mode, paste up to 10 of your favorite stickers/emojis, pick a background color, choose a layout style, and toggle the Parallax effect if desired.
5. Tap **Apply Changes** (executes an isolated root command to refresh system configurations and reset permissions).

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.

```text
DeepLayer is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
```

---
*Developed by ncreutg — Powered by open-source synergy.*
