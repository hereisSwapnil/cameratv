# CameraTV Development Command Runner
# Requires `just` (https://github.com/casey/just)

# List all available commands
default:
    @just --list

# Connect to Android TV via ADB over Wi-Fi (e.g. `just connect 192.168.1.50`)
connect ip:
    @./scripts/connect.sh {{ip}}

# List connected ADB devices
devices:
    @./scripts/devices.sh

# Push camera config to TV and reload instantly without stopping the app
push:
    @./scripts/push_cameras.sh

# Build debug APK
build:
    @./scripts/build.sh

# Install APK to TV (with runtime permissions automatically granted)
install:
    @./scripts/install.sh

# Start CameraTV on TV
start:
    @./scripts/start.sh

# Stop CameraTV on TV
stop:
    @./scripts/stop.sh

# Restart CameraTV on TV
restart:
    @./scripts/restart.sh

# Full deploy: build, install, push config, and launch on TV
deploy:
    @./scripts/deploy.sh

# Stream live TV logcat filtered to CameraTV
logs:
    @./scripts/logs.sh

# Clean build cache and temporary files
clean:
    @./scripts/clean.sh
