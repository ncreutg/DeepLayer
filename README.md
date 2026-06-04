# DeepLayer — Custom Emoji & Sticker Lock Screen for AOSP 14

DeepLayer is an advanced, system-level customization module built for clean AOSP 14. It allows users to design completely custom lock screen backgrounds using full-resolution rich content (stickers, GIFs from Gboard, or standard text emojis), mimicking the aesthetic of modern operating systems but with much deeper structural flexibility.

Unlike basic third-party live wallpaper engines, **DeepLayer operates directly within the system pipeline without modifying your primary launcher wallpaper**.

The project is structured as a Monorepo, separating the configuration app workspace (`DeepLayer_App`) from the autonomous low-level SELinux kernel patch container (`DeepLayer_Root`).

---

## ⚡ Core Features

- **Standalone Parallax Engine:** An independent, toggleable feature that uses real-time hardware sensor fusion data (Gyroscope/Accelerometer) filtered through a Kalman process matrix to dynamically shift rendering coordinates, creating an immersive, multi-layered depth effect when moving the device. Works on top of any layout.
- **Rich Content Support:** Native injection through Android's `OnReceiveContentListener`, processing Gboard stickers (PNG/WebP) and surrogate-pair text emojis flawlessly.
- **SystemUI Injection:** Hooks directly into `NotificationShadeWindowView` via LSPosed, injecting a low-overhead native rendering plane behind the lock screen notifications.
- **Alpha Blending Visibility Filter:** Resolves Heads-Up overlay clipping bugs. The custom viewplanes instantly switch to an un-focusable transparent state (`alpha = 0f`) upon device unlock, ensuring standard system notification pop-ups render smoothly without interface focus blockage.
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
- **Sensors Pipeline:** Android `SensorEventListener` (Low-latency Accelerometer matrix processing via Kalman filters)
- **Graphics Pipeline:** Low-level Android `Canvas` & `Paint` rendering context matrix
- **SELinux Security Bypass:** Autonomous Magisk/KernelSU root module injection pipeline
- **IPC / Config Data Transfer:** Fast `/data/local/tmp` secure configuration bridging

---

## 📂 Repository Structure

- `DeepLayer_App/` — Core Android Jetpack Compose configuration utility and LSPosed system hooking entry points.
- `DeepLayer_Root/` — Autonomous Magisk/KernelSU root module injecting custom policy rules (`sepolicy.rule`).

---

## 🚀 Installation & Setup

1. Make sure your device is rooted via Magisk/KernelSU and the **LSPosed** framework is active.
2. Build and install the DeepLayer APK.
3. Open the **LSPosed Manager**, enable the DeepLayer module, and ensure `System UI` is checked in the scope.
4. **Configure SELinux Security Environment (Pick ONE of the following options):**
   - **Option A (Standalone):** Download `DeepLayer_SELinux_Fix.zip` from the Releases tab, flash it via Magisk/KernelSU manager, and reboot your device. This injects the precise policy rule allowing `SystemUI` to read assets directly from the cache directory.
   - **Option B (Alternative):** If you already use **Iconify**, no additional steps are required. Iconify dynamically patches process contexts, allowing DeepLayer to read assets automatically out of the box without the separate zip module.
5. Open the DeepLayer config app, enable the mode, paste up to 10 of your favorite stickers/emojis, pick a background color, choose a layout style, and toggle the Parallax effect if desired.
6. Tap **Apply Changes** (executes an isolated root command to safely refresh system configurations via `pkill -f com.android.systemui`).

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.

```text
DeepLayer is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
```
