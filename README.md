<div align="center">

# AI Text-To-Speech Engine for Android
### 安卓系统级 AI 大模型与离线神经网络 TTS 语音引擎

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![Latest Release](https://img.shields.io/badge/Release-v3.8.0-brightgreen.svg)](https://github.com/misaka02/ai-tts-android/releases)
[![APK Size](https://img.shields.io/badge/APK_Size-2.07MB-blue.svg)](https://github.com/misaka02/ai-tts-android/releases)

<br/>

> [!IMPORTANT]
> **🤖 本项目的全部底层架构、业务代码、UI 设计、单元测试与文档发布均由 AI 智能体（Google DeepMind Antigravity）全自动自主构建与维护。**

<br/>

**AI TTS Android Engine** 是一款基于 Android 标准 `TextToSpeechService` 构建的系统级语音引擎。
无缝桥接云端主流 AI 语音大模型与端侧 Sherpa-ONNX 离线神经网络，为「开源阅读 (Legado)」、「静读天下」及系统无障碍读屏提供自然流畅的听书体验。

[下载最新正式版 (v3.8.0)](#安装包下载) · [体验最新开发测试预览版 (Round 43)](https://github.com/misaka02/ai-tts-android/releases/tag/previous)

</div>

---

## 核心特性

- **系统级原生对接**：遵循 Android 标准 TTS 服务规范，设为系统首选引擎后，「开源阅读 (Legado)」、「静读天下」及系统读屏直接调用，零多余配置。
- **云端主流大模型直连**：
  - 支持 **小米 MiMo-V2.5-TTS**（声音克隆与情绪提示词）、**微软 Edge-TTS**（免 Key 直连晓晓/云希等 300+ 神经网络音色）、**Google Gemini**、**MiniMax**、**火山/豆包**、**硅基流动**等。
  - 支持低延迟流式 (SSE) 与非流式推流，内置并发预取流水线与自适应语速控制，长篇听书稳定流畅。
- **端侧主流离线大模型 (Sherpa-ONNX)**：
  - 纯本地脱网运行，无惧断网与 API 额度，数据本地闭环。
  - **精选 20 款主流模型**：涵盖微软经典自然全系列（晓晓、云希、云扬、云健等）、GPT-SoVITS 自然大模型、ChatTTS 口语模型、原神/崩坏角色音色（刻晴、优菈、布洛妮娅等）。
  - **轻量量化专区 (13MB)**：提供轻量 INT8 模型，体积小巧，内存与功耗占用低。
- **轻量界面与分体式架构**：
  - 采用声波律动交互界面，提供实时声学反馈。
  - 分体式架构：主程序仅 **2.07MB**，端侧 8.3MB JNI 运行组件按需独立加载，长久听书稳定省电。

---

## 安装包下载

| 资源名称 | 版本说明 | 下载渠道 (国内高速) | 下载渠道 (官方直连) |
| :--- | :--- | :--- | :--- |
| **🚀 开发测试预览版 (Preview)** | **Round 43 预览版** (2.19MB，先行体验最新交互特性) | [高速镜像下载](https://ghfast.top/https://github.com/misaka02/ai-tts-android/releases/download/previous/ai-tts-engine-v3.8.0-pulse-round43.apk) | [GitHub Previous 专区](https://github.com/misaka02/ai-tts-android/releases/tag/previous) |
| **AI-TTS Engine 主程序** | **v3.8.0 正式版** (2.07MB) | [高速镜像下载](https://ghfast.top/https://github.com/misaka02/ai-tts-android/releases/download/v3.8.0/ai-tts-engine-v3.8.0.apk) | [GitHub Releases](https://github.com/misaka02/ai-tts-android/releases/download/v3.8.0/ai-tts-engine-v3.8.0.apk) |
| **离线 JNI 运行时组件** | **ARM64 架构** (8.3MB，使用离线模型时需安装) | [高速镜像下载](https://ghfast.top/https://github.com/misaka02/ai-tts-android/releases/download/v3.8.0/ai-tts-offline-runtime-arm64.apk) | [GitHub Releases](https://github.com/misaka02/ai-tts-android/releases/download/v3.8.0/ai-tts-offline-runtime-arm64.apk) |

> *注：若使用离线模型，可在应用内点击「下载独立离线组件」自动下载安装，也可直接下载上述运行时组件安装。*

---

## 快速上手

1. **配置音色**：打开应用，在在线大模型或离线模型库中选择心仪音色，点击 **试听声球** 试听；
2. **设为默认**：进入系统「设置」 -> 搜索 **「文字转语音」/「TTS」** -> 将首选引擎切换为 **`AI 大模型语音引擎`**；
3. **开始听书**：在「开源阅读 (Legado)」或「静读天下」中朗读小说，朗读引擎选择 **「系统 TTS 引擎」** 即可。

---

## 致谢

- **AI 智能体开发构建**：[Google DeepMind Antigravity](https://deepmind.google/)（本项目全自主代码设计、实现与维护）。
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

> **离线运行时解耦与追新机制**：
> - **体积轻量**：主程序 (~2MB) 与 Sherpa-ONNX C++ 推理引擎独立分体，仅在使用离线大模型时需安装 8.3MB 的运行时组件；
> - **双轨构建**：由独立模块 `runtime-addon` 维护，随上游 [Sherpa-ONNX](https://github.com/k2-fsa/sherpa-onnx) 升级。CI 构建时自动同步产出 `ai-tts-offline-runtime-arm64.apk`。

---

## 开源许可证

本项目基于 [Apache License 2.0](LICENSE) 协议开源。
