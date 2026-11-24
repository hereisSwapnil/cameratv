# CameraTV

**CameraTV** is a lightweight, high-performance Android TV / Google TV application designed for monitoring multiple RTSP IP security camera streams simultaneously in a real-time grid.

Built with modern Android standards and powered by **Jetpack Media3 (ExoPlayer)**, CameraTV delivers low-latency playback, resilient auto-recovery, and seamless remote configuration hot-reloading over ADB without restarting the app.

---

## Features

- **Multi-Camera Grid Layout**: Dynamic grid automatically adjusts for 1, 2, 4, or more camera feeds.
- **Low-Latency RTSP Streaming**: Leverages Media3 ExoPlayer RTSP with custom buffer sizing optimized for real-time video surveillance.
- **Live Configuration Hot-Reload**: Update `cameras.json` on the TV over ADB at any time; CameraTV reloads streams immediately via broadcast intent with zero app restarts.
- **Resilient Auto-Reconnection**:
  - Automatically handles stream dropouts, socket timeouts, and network changes with a visible retry pill and countdown.
  - Periodic watchdog detects frozen/stalled video frames and restarts the pipeline automatically.
- **Audio Control**: Select any camera tile with the D-pad and click `OK` / `Enter` on the TV remote to toggle audio mute/unmute.
- **Credential Handling**: Supports both discrete camera fields (IP, port, user, password, stream path) and full RTSP URLs. Automatically normalizes and percent-encodes special characters (such as `@`, `#`, `%`, `:`) in credentials.
- **Leanback & D-pad First**: Full support for Android TV Leanback launcher, TV banners, and remote control focus borders.

---

## Project Structure

```text
cameratv/
├── app/
│   ├── libs/
│   │   └── media3-exoplayer-rtsp-1.3.1.aar   # Vendored Media3 RTSP module
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/app/hereisswapnil/cameratv/
│       │   ├── MainActivity.java             # Main TV Activity & broadcast receiver
│       │   ├── CameraStreamController.java   # Player lifecycle, retry & watchdog logic
│       │   ├── ConfigManager.java            # JSON loader & parser
│       │   └── CameraItem.java               # Camera model & URL normalizer
│       └── res/                              # TV layouts, drawables & styles
├── scripts/                                  # Helper scripts for development & deployment
│   ├── build.sh                              # Compile debug APK
│   ├── install.sh                            # Install APK to TV
│   ├── push_cameras.sh                       # Push camera config & live-reload
│   ├── connect.sh                            # Connect to Android TV via ADB Wi-Fi
│   ├── deploy.sh                             # Full build + install + push + launch
│   ├── start.sh                              # Launch CameraTV on TV
│   ├── stop.sh                               # Stop CameraTV
│   ├── restart.sh                            # Restart CameraTV
│   ├── logs.sh                               # Stream filtered TV logcat
│   ├── devices.sh                            # List connected ADB devices
│   └── clean.sh                              # Clean Gradle build cache
├── cameras.sample.json                       # Sample camera configuration template
├── Justfile                                  # Command runner definitions (optional)
└── push_cameras.sh                           # Root shortcut for scripts/push_cameras.sh
```

---

## Prerequisites

- **Java Development Kit (JDK)**: Version 17
- **Android SDK** (API 34) & **Platform Tools** (`adb`)
- Android TV or Google TV connected to the same local network, with **Developer Options** and **ADB Debugging** enabled

---

## Getting Started

### 1. Configure Cameras

Copy `cameras.sample.json` to `private/cameras.json` (or `cameras.json` at repo root). The `private/` directory and `cameras.json` are git-ignored to prevent credential leaks.

```bash
cp cameras.sample.json private/cameras.json
```

Edit `private/cameras.json` with your camera credentials and streams:

```json
{
  "cameras": [
    {
      "id": "cam1",
      "name": "Front Porch",
      "ip": "192.168.1.100",
      "port": 554,
      "username": "admin",
      "password": "your_password",
      "path": "/stream1",
      "audio": true
    },
    {
      "id": "cam2",
      "name": "Backyard",
      "url": "rtsp://admin:your_password@192.168.1.101:554/h264Preview_01_main",
      "audio": false
    }
  ]
}
```

> **Tip:** You can specify cameras using discrete parameters (`ip`, `port`, `username`, `password`, `path`) or direct RTSP URLs (`url`). Special characters in passwords are automatically escaped.

---

### 2. Connect to Android TV

Find your Android TV's IP address under **Settings > Network & Internet**. Then connect:

```bash
./scripts/connect.sh 192.168.1.50
```

Verify the connection:

```bash
./scripts/devices.sh
```

---

### 3. Deploy to TV

Run the automated deploy script to build the APK, install it to the TV, push your `cameras.json` config, and launch CameraTV:

```bash
./scripts/deploy.sh
```

---

## Hot-Reloading Camera Configs

Whenever you modify camera URLs, add new streams, or change credentials in `private/cameras.json`:

```bash
./scripts/push_cameras.sh
```

This immediately pushes the configuration to `/sdcard/cameras.json` and sends a `RELOAD_CONFIG` broadcast intent. The app re-reads the configuration and restarts streams on the fly without exiting.

---

## Utility Scripts

All scripts in `scripts/` are standalone bash scripts and can be executed directly:

| Script | Description |
| :--- | :--- |
| `./scripts/connect.sh <ip>` | Connect to TV via ADB over Wi-Fi (`adb connect <ip>:5555`) |
| `./scripts/build.sh` | Compile the debug APK using Gradle (`./gradlew assembleDebug`) |
| `./scripts/install.sh` | Install debug APK to TV with permissions pre-granted (`adb install -r -g`) |
| `./scripts/push_cameras.sh` | Push config to TV and reload camera streams in real time |
| `./scripts/deploy.sh` | Full workflow: build, install, push config, and start app |
| `./scripts/start.sh` | Start CameraTV on TV |
| `./scripts/stop.sh` | Stop CameraTV on TV |
| `./scripts/restart.sh` | Restart CameraTV on TV |
| `./scripts/logs.sh` | Stream real-time filtered TV logcat logs |
| `./scripts/devices.sh` | List connected ADB devices |
| `./scripts/clean.sh` | Clean Gradle build artifacts |

---

## Justfile (Optional)

If you have [`just`](https://github.com/casey/just) installed, shortcuts are available:

```bash
just connect 192.168.1.50   # Connect to TV
just deploy                 # Build, install, push config & launch
just push                   # Push cameras.json & hot-reload
just logs                   # View live filtered logs
just clean                  # Clean build cache
```

---

## TV Remote Controls

- **D-Pad Directional Keys**: Move focus between camera tiles.
- **Center / OK Key**: Toggle audio mute / unmute for the focused camera. An audio badge indicates which camera feed currently has sound enabled.
- **Back Key**: Exit CameraTV.

---

## License

Open source.
