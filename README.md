# UsbCAM - Android Camera & Mic as USB Webcam for Ubuntu

[日本語版 (Japanese)](README.ja.md)

UsbCAM is a project that allows you to use your Android device's camera and microphone as a virtual webcam and microphone in Ubuntu via USB (ADB).

## Project Architecture

1. **Android App**:
   - **Video**: Uses CameraX to stream MJPEG over TCP port 8080.
   - **Audio**: Uses AudioRecord to stream raw audio (PCM 16-bit, 44.1kHz, Mono) over TCP port 8081.
2. **Ubuntu Integration**: Uses ADB port forwarding, `v4l2loopback`, and PulseAudio/PipeWire to convert Android streams into system virtual devices on PC.

---

## Usage Guide

### 1. Android App Setup
1. Build and install the app on your Android device using Android Studio.
2. Launch the app and grant Camera and Microphone permissions.
3. Tap **"Start Server"** to begin streaming.

### 2. Ubuntu PC Setup

#### Automatic Setup (Recommended)
Run `setup_webcam.sh` included in the project to automatically set up video/audio virtual devices and configure ADB port forwarding.
```bash
./setup_webcam.sh
```

#### Starting Video Stream
Run this command in a separate terminal:
```bash
ffmpeg -i http://localhost:8080 -pix_fmt yuv420p -f v4l2 /dev/video10
```

#### Starting Audio Stream
Run this command in another terminal:
```bash
ffmpeg -f s16le -ar 44100 -ac 1 -i tcp://localhost:8081 -f pulse AndroidMic
```

### 3. Verification
- **Video**: Select **"AndroidCam"** in your camera/video settings.
- **Audio**: Select **"AndroidMicSource"** under Input Devices in Sound settings.

---

## License
MIT
