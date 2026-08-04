# UsbCAM - Android Camera & Mic as USB Webcam for Ubuntu

Androidデバイスのカメラ映像とマイク音声を、USB（ADB）経由でUbuntuの仮想デバイス（Webcam & Microphone）として認識させるプロジェクトです。

## プロジェクト構成

1.  **Android App**:
    - **Video**: CameraXを使用してMJPEGストリームをTCP 8080で配信。
    - **Audio**: AudioRecordを使用して生音声（PCM 16bit, 44.1kHz, Mono）をTCP 8081で配信。
2.  **Ubuntu Integration**: ADBポート転送、`v4l2loopback`、PulseAudioを使用して、AndroidのストリームをPCの仮想デバイスに変換します。

---

## 使い方

### 1. Androidアプリの準備
1. Android Studioでプロジェクトをビルドし、デバイスにインストールします。
2. アプリを起動し、カメラおよびマイクの権限を許可します。
3. 「Start Server」をタップして配信を開始します。

### 2. Ubuntu側のセットアップ

#### 自動セットアップ（推奨）
プロジェクトに含まれる `setup_webcam.sh` を実行することで、ビデオ・オーディオの両方の仮想デバイス作成とポート転送を一括で行えます。
```bash
./setup_webcam.sh
```

#### 映像の開始
別のターミナルで実行してください：
```bash
ffmpeg -i http://localhost:8080 -pix_fmt yuv420p -f v4l2 /dev/video10
```

#### 音声の開始
別のターミナルで実行してください：
```bash
ffmpeg -f s16le -ar 44100 -ac 1 -i tcp://localhost:8081 -f pulse AndroidMic
```

### 3. 動作確認
- **映像**: カメラ設定から「AndroidCam」を選択。
- **音声**: サウンド設定の「入力デバイス」から「AndroidMicSource」を選択。

---

## ライセンス
MIT
