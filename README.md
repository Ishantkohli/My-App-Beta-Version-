# IK System Optimizer & Bootloader Suite (Beta)

🛡️ **An advanced Android optimization suite built in Kotlin and Jetpack Compose featuring real-time diagnostic telemetry, security scanning, and Over-the-Air (OTA) update simulation.**

DOWNLOAD APP:- https://github.com/Ishantkohli/My-App-Beta-Version-/releases/tag/Android
---

## 🌟 Key Features

*   📊 **System Performance Tuner**: Real-time CPU, GPU, and hardware diagnostic tracking.
*   ⚡ **RAM Sweeper**: Real-time active memory optimizer with visual garbage collection monitors.
*   📁 **App Workspace**: Manage background processes and clean application footprints.
*   💻 **Console Terminal**: Fully interactive system-level terminal simulation supporting standard terminal queries.
*   🌙 **Stealth Sleep**: Optimize power states and toggle lower power standby flags.
*   📈 **FPS & Stats Gauge**: Floating diagnostic overlay with active system frame measurement.
*   🛡️ **System Integrity Shield**: Audit internal OS kernel partition tables, block devices, and track cryptographic sector verification.
*   📡 **OTA Update Broadcaster**: Simulates enterprise-grade Over-the-Air package delivery. Update clients automatically from checking `v1.5.4` to `v1.5.6`.

---

## 🚀 How the OTA Broadcaster Works

This suite contains an embedded **OTA Deployment console** to showcase live version changes:
1.  **Version Code & Name Lifecycle**: Under the `ABOUT` tab, the active app version is dynamically tracked (`v1.5.6` with base build codes).
2.  **OTA Broadcast**: Using the administration dashboard card in the main layout, you can trigger a simulated multicast broadcast.
3.  **Real-Time Seeding Logs**: Track handshakes, transport security tunnels with FCM Hubs, receiver peers (e.g. Pixel 8, Galaxy S24, and your rig), and frame packet delivery.
4.  **Automatic In-App Upgrade**: On success, the in-memory version upgrades instantly to the target version (defaulting from `v1.5.6` to `v1.5.8`), completing logs inside the system console.

---

## 📦 Building and Shrinking the APK

To solve common email or host upload limits (e.g. keeping files as small as possible), we have enabled standard ProGuard shrink guards:
*   **Minification Enabled**: Strips unused classes and methods via R8 (`isMinifyEnabled = true`).
*   **Resource Shrinking**: Cleans out duplicate or unreferenced vector graphics and string XML elements (`isShrinkResources = true`).
*   **Asset Compression**: Automatically crunches heavy PNG assets to reduce the install package footprint (`isCrunchPngs = true`).

---

## 📄 License & Terms

Licensed under the [MIT License](LICENSE). Designed and built with ❤️ by Ishant Kohli.
