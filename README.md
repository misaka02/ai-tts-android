<div align="center">

# 🎙️ AI Text-To-Speech Engine for Android
### 安卓系统级 AI 在线语音大模型 TTS 引擎

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![Latest Release](https://img.shields.io/badge/Release-v3.1.0-brightgreen.svg)](https://github.com/misaka02/ai-tts-android/releases)
[![APK Size](https://img.shields.io/badge/APK_Size-1.83MB-blue.svg)](https://github.com/misaka02/ai-tts-android/releases)

<br/>

> [!IMPORTANT]
> **🤖 本项目的全部底层架构、业务代码、UI 设计、单元测试与文档发布均由 AI 智能体（Google DeepMind Antigravity）全自动自主构建与维护。**

<br/>

**AI TTS Android Engine** 是一个基于 Android 标准 `TextToSpeechService` 实现的系统级在线语音引擎，支持将各大主流 AI 大模型语音接口接入为安卓系统默认 TTS，供「开源阅读 (Legado)」、「静读天下」及系统读屏等第三方应用直接调用。

</div>

---

## 📖 快速上手

1. **配置引擎**：打开应用选择发音模型（支持免 Key 直连的**微软 Edge TTS** 或填入 **小米 MiMo** 等 API Key），点击 **「全息声球」** 确认发音；
2. **设为默认**：进入系统「设置」 $\to$ 搜索 **「文字转语音」/「TTS」** $\to$ 将首选引擎切换为 **`AI 大模型语音引擎`**；
3. **开始听书**：在「开源阅读 (Legado)」或「静读天下」中打开小说，朗读引擎选择 **「系统 TTS 引擎」** 即可。

---

## ✨ 支持的模型与服务

| 提供商 / 模型 | 特性说明 | 鉴权方式 |
| :--- | :--- | :--- |
| [**微软 Edge TTS**](https://github.com/rany2/edge-tts) | 微软神经网络语音，支持晓晓、云希等 300+ 多语言音色 | 免 Key 直连 |
| [**小米 MiMo**](https://platform.xiaomimimo.com/) | MiMo-V2.5-TTS 语音大模型，支持声音克隆与情绪提示词 | API Key |
| [**Google Gemini**](https://ai.google.dev/) | Gemini 2.0 原生多模态音频生成，内置多种拟真音色 | API Key |
| [**MiniMax (海螺)**](https://www.minimaxi.com/) | Speech-02 语音模型，支持青年与叙事音色 | API Key |
| [**火山引擎 / 豆包**](https://www.volcengine.com/product/voice-technology) | 字节跳动 BigTTS 语音大模型 | API Key |
| [**硅基流动**](https://www.siliconflow.com/) | CosyVoice2 与 ChatTTS 在线 API 接入 | API Key |
| [**Fish Audio (鱼音)**](https://fish.audio/) | 支持声音克隆模型与社区音色 | API Key |
| [**阶跃星辰**](https://platform.stepfun.com/) | StepAudio 语音大模型 | API Key |
| [**OpenAI / 兼容接口**](https://platform.openai.com/docs/guides/text-to-speech) | 标准 OpenAI Audio 接口及第三方中转格式 | API Key |
| [**自定义 HTTP 节点**](https://github.com/RVC-Boss/GPT-SoVITS) | 私有部署的 GPT-SoVITS、CosyVoice、F5-TTS 本地/局域网服务 | 自定义 URL |

---

## 🛠️ 核心功能特性

- **Android 系统标准 TTS 桥接**：完整继承 `TextToSpeechService`，系统读屏与第三方阅读器零门槛一键对接。
- **流式并发预取调度**：播放当前句时在后台并发预取下一句音频，有效消除长文本朗读的句间卡顿。
- **小说多角色对话与情绪识别**：智能切分对白与旁白并匹配不同音色，支持情绪提示词精准注入。
- **纯内存音频解码与保真重采样**：音频在 RAM 内存中流式解码并统一重采样至 24000Hz 16-bit，无磁盘磨损与变调失步。
- **发音清洗与规范化**：内置正则替换规则、11 位手机号读“幺”、时间/金额规范化与英文缩写连读。
- **本地缓存与故障自动切换**：支持合成音频本地缓存节省配额，主引擎异常时自动降级至备选引擎。
- **极致精简体积**：R8 全模式剪裁混淆，安装包仅 **1.78 MB**。

---

## 💖 致谢

- **AI 智能体开发构建**：[Google DeepMind Antigravity](https://deepmind.google/)（本项目全自主代码设计、实现与维护）。
- **语音模型与服务支持**：[小米 MiMo](https://platform.xiaomimimo.com/) · [微软 Edge-TTS](https://github.com/rany2/edge-tts) · [Google Gemini](https://ai.google.dev/) · [MiniMax](https://www.minimaxi.com/) · [火山引擎](https://www.volcengine.com/) · [硅基流动](https://www.siliconflow.com/) · [Fish Audio](https://fish.audio/) · [阶跃星辰](https://platform.stepfun.com/) · [OpenAI](https://openai.com/)
- **开源生态与阅读软件**：[开源阅读 (Legado)](https://github.com/gedoor/legado) · [静读天下](http://www.moondownload.com/) · [GPT-SoVITS](https://github.com/RVC-Boss/GPT-SoVITS) · [CosyVoice](https://github.com/FunAudioLLM/CosyVoice)

---

## 💻 编译构建

```bash
git clone https://github.com/misaka02/ai-tts-android.git
cd ai-tts-android

# 执行单元测试
./gradlew test

# 构建 Release APK
./gradlew assembleRelease
```

---

## 📄 开源许可证

本项目基于 [Apache License 2.0](LICENSE) 协议开源。
