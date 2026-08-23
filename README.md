<div align="center">

# 🎙️ AI Text-To-Speech Engine for Android
### 安卓系统级 AI 在线语音大模型 TTS 引擎

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![Latest Release](https://img.shields.io/badge/Release-v2.1.0-brightgreen.svg)](https://github.com/misaka02/ai-tts-android/releases)

<br/>

> **说明**：本项目是一个基于 Android 标准 `TextToSpeechService` 实现的在线语音引擎，支持将各大主流 AI 大模型语音接口（如小米 MiMo、微软 Edge TTS、Google Gemini、MiniMax、火山豆包等及本地私有化节点）接入为安卓系统默认 TTS，供「开源阅读 (Legado)」、「静读天下」及系统读屏等第三方应用直接调用。

</div>

---

## 📖 快速上手

1. **配置引擎**：打开本 App，选择目标模型（如免 Key 直连的**微软 Edge TTS** 或填入 **小米 MiMo** 等 API Key），点击 **「立即试听」** 确认发音正常；
2. **设为默认**：进入系统「设置」 $\to$ 搜索 **「文字转语音」/「TTS」** $\to$ 将首选引擎切换为 **`AI 大模型语音引擎`**；
3. **开始听书**：在「开源阅读 (Legado)」或「静读天下」等应用中打开小说，朗读引擎选择 **「系统 TTS 引擎」** 即可。

---

## ✨ 模型支持

| 提供商 / 模型 | 说明与特性 | 鉴权方式 |
| :--- | :--- | :--- |
| [**微软 Edge TTS**](https://github.com/rany2/edge-tts) | 微软神经网络语音，支持晓晓、云希等 300+ 多语言音色 | 免 Key 直连 |
| [**小米 MiMo**](https://platform.xiaomimimo.com/) | MiMo-V2.5-TTS 语音大模型，支持声音克隆与情绪提示词 | API Key |
| [**Google Gemini**](https://ai.google.dev/) | Gemini 2.0 原生多模态音频生成，内置多种拟真音色 | API Key |
| [**MiniMax (海螺)**](https://www.minimaxi.com/) | Speech-02 语音模型，支持多种青年/叙事音色 | API Key |
| [**火山引擎 / 豆包**](https://www.volcengine.com/product/voice-technology) | 字节跳动 BigTTS 语音大模型 | API Key |
| [**硅基流动**](https://www.siliconflow.com/) | CosyVoice2 与 ChatTTS 在线 API 接入 | API Key |
| [**Fish Audio (鱼音)**](https://fish.audio/) | 支持声音克隆模型与社区音色 | API Key |
| [**阶跃星辰**](https://platform.stepfun.com/) | StepAudio 语音大模型 | API Key |
| [**OpenAI / 兼容接口**](https://platform.openai.com/docs/guides/text-to-speech) | 标准 OpenAI Audio 接口及第三方中转格式 | API Key |
| [**自定义 HTTP 节点**](https://github.com/RVC-Boss/GPT-SoVITS) | 支持私有部署的 GPT-SoVITS、CosyVoice、F5-TTS 等本地/局域网服务 | 填入自定义 URL |

---

## 🛠️ 主要功能特性

- **双 UI 界面自由切换**：支持「Next-Gen Studio 调音台工作台」与「经典紧凑列表」双套交互布局，界面顶部一键即时无缝切换。
- **流式并发预取**：播放当前段落时在后台异步预取下一句，降低长文本句间停顿。
- **内存音频解码与重采样**：音频流在内存中直接解码并保真重采样为 24000Hz 16-bit 单声道，避免跨音色切换时采样率失步变调。
- **小说多角色与情绪识别**：支持识别引号对白与引述语，可配置旁白、男主、女主、长者等多声线协同朗读，并注入情绪微调提示。
- **发音规则与文本清洗**：支持自定义正则替换规则、11 位手机号自动读“幺”、时间/金额规范化以及常用英文缩写连读。
- **本地缓存与故障切换**：支持合成音频本地缓存以节省重复请求配额，主引擎异常时支持自动降级至备用引擎。
- **试听语料库与一言集成**：内置多分类试听文本，支持一键从「一言」在线拉取名句台词快速试听。

---

## 💻 编译与构建

```bash
# 克隆仓库
git clone https://github.com/misaka02/ai-tts-android.git
cd ai-tts-android

# 执行单元测试
./gradlew test

# 构建 Release APK
./gradlew assembleRelease
```

---

## 💖 致谢

- 语音与模型服务：[小米 MiMo](https://platform.xiaomimimo.com/) · [微软 Edge-TTS](https://github.com/rany2/edge-tts) · [Google Gemini](https://ai.google.dev/) · [MiniMax](https://www.minimaxi.com/) · [火山引擎](https://www.volcengine.com/) · [硅基流动](https://www.siliconflow.com/) · [Fish Audio](https://fish.audio/) · [阶跃星辰](https://platform.stepfun.com/) · [OpenAI](https://openai.com/) · [一言 Hitokoto](https://hitokoto.cn/)
- 阅读生态与开源项目：[开源阅读 (Legado)](https://github.com/gedoor/legado) · [静读天下](http://www.moondownload.com/) · [GPT-SoVITS](https://github.com/RVC-Boss/GPT-SoVITS) · [CosyVoice](https://github.com/FunAudioLLM/CosyVoice)
