<img src="https://raw.githubusercontent.com/github/explore/4479d2a2c854198cb00160f8593519c14dc3b905/topics/kotlin/kotlin.png" alt="Kotlin" width="25" height="25" /> <img src="https://raw.githubusercontent.com/github/explore/ae48d1ca3274c0c3a90f872e605eaef069a16771/topics/jetpack-compose/jetpack-compose.png" alt="Jetpack Compsoe" width="25" height="25" /> <img src="https://avatars.githubusercontent.com/u/26833451?s=200&v=4" alt="ultralytics" width="25" height="25" /> <img src="https://avatars.githubusercontent.com/u/53104118?s=200&v=4" alt="roboflow" width="25" height="25" />
<img src="https://avatars.githubusercontent.com/u/15658638?s=200&v=4" alt="tensorflow" width="25" height="25" /> 


## Description

**Pedestrian Obstacle Detector** is an Android application designed to keep both visually impaired users and distracted pedestrians safe by detecting obstacles in real time using an on-device deep-learning model. The app continuously analyzes the camera feed, raising an audio alert for blind or low-vision users and a haptic (vibration) alert for sighted users who may be focused on their phone.

Under the hood, the application uses a customized YOLOv8 model retrained on a Roboflow-managed dataset of 18 obstacle classes (e.g., road edges, vehicles, pedestrians, trash bins) and optimizes inference through model quantization. After original inference times of 2.63 s, customization and pruning reduced this to 0.79 s, and with Float16 and full INT8 quantization, latency dropped to 0.68 s and 0.595 s respectively, enabling true real-time performance constrainted mobile devices.

The training pipeline leveraged Google Colab for initial model fine-tuning, with Roboflow’s annotation and augmentation tools streamlining dataset preparation and versioning. quantization strategies and edge-computing techniques further balance speed and accuracy, achieving 83 % overall mAP and up to 92 % for key classes like roads, vehicles, and people.

Built entirely in Kotlin with Jetpack Compose for a modern, reactive UI, the app integrates Kotlin Coroutines for seamless multithreading, Android’s TextToSpeech API for voice alerts, TensorFlow Lite for on-device inference, and NNAPI where available to accelerate quantized models.

## Requirements

- **Android 5.0 (Lollipop) or higher**  
- **Kotlin**  
- **Jetpack Compose**  
- **Ultralytics YOLOv8** (model and scripts)  
- **Roboflow** (dataset management)  
- **TensorFlow Lite** (+ INT8 quantization support)  
- **Kotlinx Coroutines**  
- **Android TextToSpeech**  
- **Vibrator** (Haptic feedback)

## Getting Started

1. **Clone** this repository  
   ```bash
   git clone https://github.com/YourUsername/ObstacleDetector-Android.git
   cd ObstacleDetector-Android

2. Open in Android Studio and Sync Gradle

3. Add your trained .tflite model into app/src/main/assets/model.tflite

4. Place demo images under app/src/main/res/drawable/demo1.png … demo5.png for test sections

5. Run on your device or emulator with camera support

## License

MIT License

Copyright (c) 2024 Abtin Zandi

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
authors OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
