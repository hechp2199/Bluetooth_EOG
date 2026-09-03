# Smartphone-Based EOG Eye Movement Classification

An Android application for real-time Electrooculography (EOG) signal visualization, recording, and eye movement classification using on-device deep learning.

The application receives dual-channel EOG signals wirelessly from an Arduino-based acquisition system through an HC-05 Bluetooth module. The signals are visualized and processed on the smartphone, and a lightweight EEGNet-based TensorFlow Lite model performs eye movement classification directly on the device.

## Features

- Wireless EOG signal acquisition through Bluetooth
- Real-time visualization of horizontal and vertical EOG signals
- EOG signal recording and CSV export
- Signal buffering and preprocessing on the Android device
- On-device eye movement classification using TensorFlow Lite
- Six-class eye movement recognition:
  - Left
  - Right
  - Up
  - Down
  - Blink
  - No Movement
- Lightweight neural network suitable for mobile deployment

## System Overview

```text
EOG Electrodes
      ↓
BioAmp EXG Pill
      ↓
Arduino Uno R3
      ↓
HC-05 Bluetooth
      ↓
Android Application
      ↓
┌─────────────────────────────┐
│ Signal Visualization        │
│ Data Recording              │
│ Signal Preprocessing        │
│ TensorFlow Lite Inference   │
└─────────────────────────────┘
      ↓
Eye Movement Prediction
