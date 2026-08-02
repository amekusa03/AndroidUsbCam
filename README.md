# UsbCAM - Android Camera as USB Webcam for Ubuntu

Androidデバイスのカメラ映像を、USB（ADB）経由でUbuntuの仮想ビデオデバイス（Webcam）として認識させるプロジェクトです。

## プロジェクト構成

1.  **Android App**: CameraXを使用してカメラ映像をキャプチャし, MJPEGストリームをTCPポート 8080で配信します。
2.  **Ubuntu Integration**: ADBポート転送, `v4l2loopback`, および`ffmpeg`を使用して、AndroidのストリームをPCの仮想カメラデバイスに変換します。

---

## 使い方

### 1. Androidアプリの準備
1. Android Studioでプロジェクトをビルドし、デバイスにインストールします。
2. アプリを起動し、カメラ権限を許可します。
3. 「Start Server」をタップして配信を開始します。

### 2. Ubuntu側のセットアップ

#### 自動セットアップ（推奨）
プロジェクトに含まれる `setup_webcam.sh` を実行することで、デバイス作成からポート転送までを一括で行えます。
```bash
./setup_webcam.sh
```

#### 手動で行う場合
**1. 必要なツールのインストール**
```bash
sudo apt update
sudo apt install v4l2loopback-dkms v4l2loopback-utils ffmpeg adb v4l-utils
```

**2. 仮想カメラデバイスの作成**
以下のコマンドで、仮想ビデオデバイスを作成します。ここでは既存のカメラと衝突しないよう `/dev/video10` を指定しています。
```bash
# 一旦既存のモジュールを削除
sudo modprobe -r v4l2loopback

# 設定を指定してロード
sudo modprobe v4l2loopback exclusive_caps=1 video_nr=10 card_label="AndroidCam"

# デバイスのアクセス権限を付与
sudo chmod 666 /dev/video10
```

**3. ADBポート転送**
AndroidデバイスをUSBで接続し、PCの8080ポートをAndroidの8080ポートへ転送します。
```bash
# デバイスが1台の場合
adb forward tcp:8080 tcp:8080

# 複数デバイスがある場合は -s で指定
# adb -s <SERIAL_NUMBER> forward tcp:8080 tcp:8080
```

#### 映像のパイプ（ffmpeg）
AndroidのMJPEGストリームを仮想デバイスに流し込みます。
```bash
ffmpeg -i http://localhost:8080 -pix_fmt yuv420p -f v4l2 /dev/video10
```
※ `frame=...` と表示されれば成功です。このターミナルは閉じないでください。

### 3. 動作確認
別のターミナルまたはアプリで映像を確認します。

- **ffplayで確認**: `ffplay -f v4l2 /dev/video10`
- **Web会議アプリ**: カメラ設定から「AndroidCam」を選択してください。

---

## トラブルシューティング

- **Permission Denied**: `sudo chmod 666 /dev/video10` を実行して権限を与えてください。
- **Not a video capture device**: `v4l2loopback` ロード時に `exclusive_caps=1` を指定しているか確認してください。また、ffmpegによる書き込みが開始されるまでキャプチャデバイスとして認識されない場合があります。
- **Pixel format error**: ffmpegのコマンドに `-pix_fmt yuv420p` が含まれているか確認してください。
- **複数デバイスエラー**: `adb devices` でシリアル番号を確認し、`adb -s <SERIAL> forward ...` を使用してください。

## ライセンス
MIT
