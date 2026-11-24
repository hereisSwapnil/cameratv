<p align="center">
  <img src="assets/logo.png" alt="CameraTV Logo" width="180" style="border-radius: 24px;">
</p>

<h1 align="center">CameraTV</h1>

<p align="center">
  <strong>Fast, private, multi-stream RTSP security camera monitor for Android TV & Google TV.</strong><br>
  Stream local IP cameras in a real-time grid with low latency, self-healing freeze recovery, and remote config hot-reload — 100% offline with zero cloud dependency.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square" alt="MIT License">
  <img src="https://img.shields.io/badge/Android_TV-8.0+-3DDC84.svg?style=flat-square&logo=android&logoColor=white" alt="Android TV 8.0+">
  <img src="https://img.shields.io/badge/Media3-ExoPlayer-FF6F00.svg?style=flat-square" alt="Media3 ExoPlayer">
  <img src="https://img.shields.io/badge/RTSP-Low_Latency-blueviolet.svg?style=flat-square" alt="RTSP Low Latency">
  <img src="https://img.shields.io/badge/Privacy-100%25_Local-success.svg?style=flat-square" alt="100% Local">
</p>

## ✨ The Story Behind CameraTV

Monitoring local IP security cameras on a living room TV should be straightforward. Instead, it was always a frustrating experience.

Every available option had major headaches:
- **Vendor Apps (Tapo, Reolink, Tuya, Hikvision, Dahua):** Cluttered with paywalled cloud subscriptions, sluggish, and almost impossible to navigate with an Android TV D-pad remote.
- **Generic Players (VLC, TinyCam, Kodi):** Tedious on-screen keyboard typing for long RTSP URLs, clunky multi-stream setups, frequent stream freezes, and heavy memory consumption on modest TV hardware.
- **Third-Party Surveillance Apps:** Laggy playback, intrusive advertisements, or worse, routing private video feeds through unknown third-party cloud servers.

I just wanted to turn on the TV, see all my front gate and perimeter cameras in a clean grid instantly, toggle audio when needed with the remote, and push new camera URLs directly from my laptop over Wi-Fi without having to re-type anything.

So I built **CameraTV**.

---

### What CameraTV Does

**CameraTV** is a lightweight, 100% local Android TV application powered by Jetpack Media3 (ExoPlayer). It streams multiple RTSP feeds simultaneously with minimal latency, monitors connection health, and automatically recovers from frame freezes.

- 🔒 **100% Local & Private:** Connects directly to your local IP cameras over RTSP. Zero cloud tracking, zero accounts, zero analytics.
- ⚡ **Low-Latency Streaming:** Hardware-accelerated ExoPlayer RTSP pipeline tuned for real-time surveillance.
- 🔄 **Live Hot-Reload over Wi-Fi:** Update your camera config on your laptop and push it to the TV via ADB. Cameras update on screen immediately with zero app restarts.
- 🛡️ **Self-Healing Watchdog:** Detects dropped network packets or frozen video frames and silently reconnects in seconds.

---

## 📦 Quick Start & Setup

### 1. Configure Your Cameras
Copy `cameras.sample.json` to `private/cameras.json` (or `cameras.json` at root). These files are ignored by git to protect your credentials:

```bash
cp cameras.sample.json private/cameras.json
```

Add your camera details:

```json
{
  "cameras": [
    {
      "id": "cam1",
      "name": "Outside Main Gate",
      "ip": "192.168.1.100",
      "port": 554,
      "username": "admin",
      "password": "your_password",
      "path": "/stream1",
      "audio": true
    },
    {
      "id": "cam2",
      "name": "Front Porch",
      "url": "rtsp://admin:your_password@192.168.1.101:554/stream1",
      "audio": false
    }
  ]
}
```

> **Tip:** You can define cameras using discrete fields (`ip`, `port`, `username`, `password`, `path`) or a direct RTSP `url`. Special characters in passwords (`@`, `#`, etc.) are automatically handled and normalized.

---

### 2. Connect to Your TV via ADB
Enable **Developer Options** and **ADB Debugging** on your Android TV / Google TV, then connect over Wi-Fi:

```bash
./scripts/connect.sh 192.168.1.50
```

---

### 3. Deploy & Run
Build the APK, install it to your TV, push your camera configuration, and launch CameraTV in one command:

```bash
./scripts/deploy.sh
```

---

## 🔄 Instant Config Hot-Reload

Whenever you update camera IP addresses, passwords, or add new feeds, simply run:

```bash
./scripts/push_cameras.sh
```

The script transfers `cameras.json` to the TV and triggers a live reload broadcast. The grid refreshes instantly without closing the app.

---

## 🔥 Key Features

- 📺 **Dynamic Grid Layout** — Auto-scales layout seamlessly for 1, 2, 4, or more camera feeds.
- ⚡ **Ultra Low-Latency** — Custom Media3 RTSP buffer controls minimize lag for live viewing.
- 🔄 **ADB Hot-Reload** — Push config changes over Wi-Fi without rebuilding or relaunching.
- 🛡️ **Active Freeze Watchdog** — Monitors playback timestamps to catch and recover from silent RTSP freezes.
- ⏳ **Resilient Auto-Retry** — Graceful reconnection with a countdown pill during temporary network dropouts.
- 🔊 **Remote Audio Toggle** — D-pad navigate to any camera tile and press `OK` / `Enter` to unmute.
- 🎯 **URL Auto-Encoding** — Automatic normalization for credentials containing `@`, `#`, or special symbols.
- 📺 **Leanback Native** — Includes full Android TV banner, launcher support, and focus indicators.

---

## 🏗️ How It Works

```
[cameras.json / private/cameras.json]
         │
         ├───► ./scripts/push_cameras.sh (over Wi-Fi ADB)
         │
         ▼
[/sdcard/cameras.json]
         │
         ▼
[MainActivity / BroadcastReceiver]
         │
         ├── Parse Config (Discrete Fields or RTSP URLs)
         │
         ▼
[Dynamic Camera Grid]
         │
         ├───► [ExoPlayer RTSP Instance per Stream]
         │         │
         │         ├── Hardware-Accelerated Video Rendering
         │         │
         │         └── [Health & Stall Watchdog (2s Interval)]
         │                   │
         │                   ├── Frozen Frame?  ──► Auto-Restart Player Pipeline
         │                   └── Stream Drop?   ──► Countdown Pill & Auto-Reconnect
         │
         └───► [TV Remote Navigation]
                   └── Click OK / Enter ──► Toggle Audio Mute / Unmute
```

---

## 🛠️ Developer Scripts

All helper scripts in [`scripts/`](scripts/) are executable and self-contained:

| Script | Description |
| :--- | :--- |
| `./scripts/connect.sh <ip>` | Connect to Android TV via ADB over Wi-Fi |
| `./scripts/build.sh` | Build the debug APK (`./gradlew assembleDebug`) |
| `./scripts/install.sh` | Install APK to TV with permissions auto-granted |
| `./scripts/push_cameras.sh` | Push camera config to TV and reload in real-time |
| `./scripts/deploy.sh` | Complete workflow: build, install, push config & launch |
| `./scripts/start.sh` | Start CameraTV on TV |
| `./scripts/stop.sh` | Force-stop CameraTV on TV |
| `./scripts/restart.sh` | Restart CameraTV on TV |
| `./scripts/logs.sh` | Stream real-time filtered logcat logs |
| `./scripts/devices.sh` | List connected ADB devices |
| `./scripts/clean.sh` | Clean Gradle build cache |

> If you use [`just`](https://github.com/casey/just), shorthand commands like `just connect <ip>`, `just deploy`, `just push`, and `just logs` are ready to use.

---

## 🎮 TV Remote Controls

- **D-Pad Directional Keys**: Move focus between camera tiles.
- **Center / OK Key**: Toggle audio mute / unmute on the focused camera tile.
- **Back Key**: Exit CameraTV.

---

## 📜 License

Distributed under the **MIT License**.

---

<p align="center">
  Crafted with care by <strong><a href="https://github.com/hereisSwapnil">Swapnil</a></strong>
</p>
