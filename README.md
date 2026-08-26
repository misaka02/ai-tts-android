<div align="center">

# 🎙️ AI Text-To-Speech Engine for Android
### 安卓系统级 AI 大模型 & 离线神经网络 TTS 语音引擎

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Pulse_Theme-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![Latest Release](https://img.shields.io/badge/Release-v3.7.0-brightgreen.svg)](https://github.com/misaka02/ai-tts-android/releases)
[![APK Size](https://img.shields.io/badge/APK_Size-2.07MB-blue.svg)](https://github.com/misaka02/ai-tts-android/releases)

<br/>

**AI TTS Android Engine** 是一款基于 Android 标准 `TextToSpeechService` 构建的现代化系统级语音引擎。
无缝桥接云端主流 AI 语音大模型与端侧 Sherpa-ONNX 离线神经网络，为「静读天下」、「开源阅读 (Legado)」等阅读器提供媲美真人的高保真听书体验。

[📥 立即下载最新正式版 (v3.7.0)](#-安装包下载)

</div>

---

## 🚀 核心特性

- **📱 系统级原生对接**：遵循 Android 标准 TTS 规范，设为系统首选引擎后，「静读天下」、「开源阅读」、微信读书及无障碍读屏一键即用。
- **☁️ 云端主流大模型直连**：
  - 支持 **小米 MiMo-V2.5-TTS**（声音克隆与情绪提示词）、**微软 Edge-TTS**（免 Key 直连晓晓/云希等 300+ 音色）、**Google Gemini**、**MiniMax**、**火山/豆包**、**硅基流动**等。
  - 支持超低延迟流式 (SSE) 与非流式推流，内置并发预取与听书自适应语速控制，长文朗读丝滑无卡顿。
- **🔌 端侧主流离线大模型 (Sherpa-ONNX)**：
  - 纯本地脱网运行，无惧断网与 API 额度，隐私零上传。
  - **精选 21 款主流模型**：涵盖微软经典自然全系列（晓晓、云希、云扬、云健等）、GPT-SoVITS 自然大模型、ChatTTS 口语模型、原神/崩坏 ACG 角色音色（刻晴、优菈、布洛妮娅等）。
  - **🔥 极速 13MB 专区**：推出极轻量 INT8 模型，秒下秒用，超低功耗与内存占用。
- **💎 灵动脉冲 Pulse 界面 & 2MB 极简体积**：
  - 赛博深色全息声球交互，视觉与听觉实时律动。
  - 独创分体式架构：主程序仅 **2.07MB**，端侧 8.3MB JNI 运行组件按需独立加载，长久听书稳定不杀后台。

---

## 📥 安装包下载

| 资源名称 | 版本说明 | 下载渠道 (国内高速) | 下载渠道 (官方直连) |
| :--- | :--- | :--- | :--- |
| **AI-TTS Engine 主程序** | **v3.7.0 正式版** (2.07MB) | [高速镜像下载](https://ghfast.top/https://github.com/misaka02/ai-tts-android/releases/download/v3.7.0/ai-tts-engine-v3.7.0.apk) | [GitHub Releases](https://github.com/misaka02/ai-tts-android/releases/download/v3.7.0/ai-tts-engine-v3.7.0.apk) |
| **离线 JNI 运行时组件** | **ARM64 架构** (8.3MB，使用离线模型时需安装) | [高速镜像下载](https://ghfast.top/https://github.com/misaka02/ai-tts-android/releases/download/v3.7.0/ai-tts-offline-runtime-arm64.apk) | [GitHub Releases](https://github.com/misaka02/ai-tts-android/releases/download/v3.7.0/ai-tts-offline-runtime-arm64.apk) |

> 💡 *注：若使用离线模型，可在应用内点击「下载独立离线组件」自动下载安装，也可直接下载上述运行时组件安装。*

---

## 📖 3 步快速上手

1. **配置音色**：打开应用，在在线大模型或离线模型库中选择心仪音色，点击 **「全息声球」** 试听；
2. **设为默认**：进入系统「设置」 $\to$ 搜索 **「文字转语音」/「TTS」** $\to$ 将首选引擎切换为 **`AI 大模型语音引擎`**；
3. **沉浸听书**：在「静读天下」或「开源阅读」中朗读小说，朗读引擎选择 **「系统 TTS 引擎」** 即可。

---

## 💻 编译构建

```bash
git clone https://github.com/misaka02/ai-tts-android.git
cd ai-tts-android

# 执行单元测试
./gradlew test

# 构建正式版 Release APK
./gradlew assembleRelease
```

---

## 📄 开源许可证

本项目基于 [Apache License 2.0](LICENSE) 协议开源。
