<div align="center">

# AI Text-To-Speech Engine for Android
### 安卓系统级 AI 大模型与离线神经网络 TTS 语音引擎

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![Latest Release](https://img.shields.io/badge/Release-v3.9.0-brightgreen.svg)](https://github.com/misaka02/ai-tts-android/releases)
[![APK Size](https://img.shields.io/badge/APK_Size-2.26MB-blue.svg)](https://github.com/misaka02/ai-tts-android/releases)

<br/>

**AI TTS Android Engine** 是一款基于 Android 标准 `TextToSpeechService` 构建的系统级语音引擎。
无缝桥接云端主流 AI 语音大模型与端侧 Sherpa-ONNX 离线神经网络，为「开源阅读 (Legado)」、「静读天下」及系统无障碍读屏提供自然流畅的听书体验。

[下载最新正式版 (v3.9.0)](#安装包下载) · [体验最新开发测试预览版](https://github.com/misaka02/ai-tts-android/releases/tag/previous)

</div>

---

## 核心特性

- **系统级原生对接**：遵循 Android 标准 TTS 服务规范，设为系统首选引擎后，「开源阅读 (Legado)」、「静读天下」及系统读屏直接调用，零多余配置。
- **5 大界面设计系统**：
  - **Google 官方样式 (Material 3)**：基于 Google Recorder 与 Pixel 原生设计语言打造，纯净极简，遵循 Material 3 规范分级收纳；
  - **极光微胶囊 (Pulse)**：灵动光效与双层对称声谱波形交互；
  - **全景网格 (Bento)**：模块化网格矩阵工作台；
  - **专业调音台 (Studio)**：多轨音频控制台；
  - **复古黑胶 (Vinyl)**：典藏黑胶质感。
- **云端主流大模型直连**：
  - 支持 **小米 MiMo-V2.5-TTS**（声学指令与流式物理拉伸）、**微软 Edge-TTS**（免 Key 直连晓晓/云希等 300+ 神经网络音色）、**Google Gemini**、**MiniMax**、**火山/豆包**、**阶跃星辰**、**OpenAI**、**硅基流动**等。
  - 支持低延迟流式 (SSE) 与非流式推流，自适应四大调速体系，长篇听书稳定流畅。
- **端侧离线大模型 (Sherpa-ONNX)**：
  - 纯本地脱网运行，无惧断网与 API 额度，数据本地闭环。
  - 涵盖微软自然全系列（晓晓、云希、云扬等）、GPT-SoVITS 自然大模型、ChatTTS 口语模型、原神角色音色等。
  - 提供轻量 INT8 模型，体积小巧，内存与功耗占用低。
- **轻量界面与分体式架构**：
  - 主程序仅约 **2.26MB**，端侧 JNI 运行组件按需独立加载，长久听书稳定省电。

---

## 安装包下载

| 资源名称 | 版本说明 | 下载渠道 (官方直连) |
| :--- | :--- | :--- |
| **🚀 AI-TTS Engine 主程序** | **v3.9.0 正式版** (~2.26MB) | [GitHub Releases 下载](https://github.com/misaka02/ai-tts-android/releases/download/v3.9.0/ai-tts-engine-v3.9.0.apk) |
| **离线 JNI 运行时组件** | **ARM64 架构** (8.3MB，使用离线模型时需安装) | [GitHub Releases 下载](https://github.com/misaka02/ai-tts-android/releases/download/v3.9.0/ai-tts-offline-runtime-arm64.apk) |
| **开发测试预览版 (Preview)** | **最新开发测试版** (先行体验最新交互特性) | [GitHub Previous 专区](https://github.com/misaka02/ai-tts-android/releases/tag/previous) |

> *注：若使用离线模型，可在应用内点击「下载独立离线组件」自动下载安装，也可直接下载上述运行时组件安装。*

---

## 快速上手

1. **配置音色**：打开应用，在服务商列表中选择心仪模型与音色，点击试听按钮试听；
2. **设为默认**：进入系统「设置」 -> 搜索 **「文字转语音」/「TTS」** -> 将首选引擎切换为 **`AI 大模型语音引擎`**；
3. **开始听书**：在「开源阅读 (Legado)」或「静读天下」中朗读小说，朗读引擎选择 **「系统 TTS 引擎」** 即可。

---

## 致谢

- **音频与底层算法**：[Sonic](https://github.com/waywardgeek/sonic) (Bill Cox) · [Sherpa-ONNX](https://github.com/k2-fsa/sherpa-onnx) (k2-fsa) · [Apache Commons Compress](https://commons.apache.org/proper/commons-compress/) · [OkHttp](https://github.com/square/okhttp) (Square)
- **语音模型与服务支持**：[小米 MiMo](https://platform.xiaomimimo.com/) · [微软 Edge-TTS](https://github.com/rany2/edge-tts) · [Google Gemini](https://ai.google.dev/) · [MiniMax](https://www.minimaxi.com/) · [火山引擎](https://www.volcengine.com/) · [硅基流动](https://www.siliconflow.com/) · [Fish Audio](https://fish.audio/) · [阶跃星辰](https://platform.stepfun.com/) · [OpenAI](https://openai.com/) · [Piper](https://github.com/rhasspy/piper) · [MeloTTS](https://github.com/myshell-ai/MeloTTS) · [Matcha-TTS](https://github.com/shivamm/matcha-tts) · [Kokoro](https://github.com/hexgrad/kokoro) · [ChatTTS](https://github.com/2noise/ChatTTS) · [GPT-SoVITS](https://github.com/RVC-Boss/GPT-SoVITS) · [CosyVoice](https://github.com/FunAudioLLM/CosyVoice)
- **开源生态与阅读软件**：[开源阅读 (Legado)](https://github.com/gedoor/legado) · [静读天下](http://www.moondownload.com/)

---

## 编译构建

```bash
git clone https://github.com/misaka02/ai-tts-android.git
cd ai-tts-android

# 执行单元测试
./gradlew test

# 构建正式版主程序与离线运行时组件
./gradlew assembleRelease
```

---

## 开源许可证

本项目基于 [Apache License 2.0](LICENSE) 协议开源。
