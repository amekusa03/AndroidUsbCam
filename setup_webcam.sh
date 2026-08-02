#!/bin/bash

# 設定
VIDEO_NR=10
CARD_LABEL="AndroidCam"
PORT=8080

echo "--- Android USB Webcam Setup ---"

# 1. 既存のモジュールを削除
echo "[1/3] Removing existing v4l2loopback module..."
sudo modprobe -r v4l2loopback 2>/dev/null

# 2. モジュールをロード
echo "[2/3] Loading v4l2loopback (device: /dev/video$VIDEO_NR)..."
sudo modprobe v4l2loopback exclusive_caps=1 video_nr=$VIDEO_NR card_label="$CARD_LABEL"

if [ $? -eq 0 ]; then
    # 権限付与
    sudo chmod 666 /dev/video$VIDEO_NR
    echo "      Success: /dev/video$VIDEO_NR created."
else
    echo "      Error: Failed to load v4l2loopback. Is it installed?"
    exit 1
fi

# 3. ADBポート転送
echo "[3/3] Setting up ADB port forwarding..."
# 複数デバイスがある場合に備え、エラーを無視せず確認
adb forward tcp:$PORT tcp:$PORT

if [ $? -eq 0 ]; then
    echo "      Success: Port $PORT forwarded."
else
    echo "      Warning: ADB forward failed. Check if device is connected."
fi

echo "--------------------------------"
echo "Setup complete!"
echo "Next, run this command to start streaming:"
echo "ffmpeg -i http://localhost:$PORT -pix_fmt yuv420p -f v4l2 /dev/video$VIDEO_NR"
echo "--------------------------------"
