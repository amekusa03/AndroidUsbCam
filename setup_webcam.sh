#!/bin/bash

# 設定
VIDEO_NR=10
CARD_LABEL="AndroidCam"
V_MIC_SINK="AndroidMic"
V_MIC_SOURCE="AndroidMicSource"
PORT_VIDEO=8080
PORT_AUDIO=8081

echo "--- Android USB Webcam & Mic Setup ---"

# 1. 既存のビデオモジュールを削除
echo "[1/4] Removing existing v4l2loopback module..."
sudo modprobe -r v4l2loopback 2>/dev/null

# 2. ビデオモジュールをロード
echo "[2/4] Loading v4l2loopback (device: /dev/video$VIDEO_NR)..."
sudo modprobe v4l2loopback exclusive_caps=1 video_nr=$VIDEO_NR card_label="$CARD_LABEL"

if [ $? -eq 0 ]; then
    sudo chmod 666 /dev/video$VIDEO_NR
    echo "      Success: /dev/video$VIDEO_NR created."
else
    echo "      Error: Failed to load v4l2loopback."
    exit 1
fi

# 3. 仮想マイクのセットアップ (PulseAudio/PipeWire)
echo "[3/4] Setting up virtual microphone..."
# 既存のモジュールをアンロード（あれば）
pactl unload-module module-null-sink 2>/dev/null
pactl unload-module module-remap-source 2>/dev/null

# 仮想Sinkを作成
pactl load-module module-null-sink sink_name=$V_MIC_SINK sink_properties=device.description=$V_MIC_SINK
# SinkのMonitorをSource（マイク）として再マッピング
pactl load-module module-remap-source master=$V_MIC_SINK.monitor source_name=$V_MIC_SOURCE source_properties=device.description=$V_MIC_SOURCE

echo "      Success: Virtual Mic '$V_MIC_SOURCE' created."

# 4. ADBポート転送
echo "[4/4] Setting up ADB port forwarding..."
adb forward tcp:$PORT_VIDEO tcp:$PORT_VIDEO
adb forward tcp:$PORT_AUDIO tcp:$PORT_AUDIO

if [ $? -eq 0 ]; then
    echo "      Success: Ports $PORT_VIDEO (Video) and $PORT_AUDIO (Audio) forwarded."
else
    echo "      Warning: ADB forward failed."
fi

echo "--------------------------------"
echo "Setup complete!"
echo "--- To start Video ---"
echo "ffmpeg -i http://localhost:$PORT_VIDEO -pix_fmt yuv420p -f v4l2 /dev/video$VIDEO_NR"
echo ""
echo "--- To start Audio ---"
echo "ffmpeg -f s16le -ar 44100 -ac 1 -i tcp://localhost:$PORT_AUDIO -f pulse $V_MIC_SINK"
echo "--------------------------------"
