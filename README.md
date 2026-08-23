<div align="center">

# 🎙️ AI Text-To-Speech Engine for Android
### 安卓系统级 AI 在线语音大模型 TTS 引擎

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![Latest Release](https://img.shields.io/badge/Release-v2.3.1-brightgreen.svg)](https://github.com/misaka02/ai-tts-android/releases)
[![APK Size](https://img.shields.io/badge/APK_Size-1.78MB-blue.svg)](https://github.com/misaka02/ai-tts-android/releases)

<br/>

> [!IMPORTANT]
> **🤖 本项目的全部底层架构、业务代码、UI 设计、单元测试与文档发布均由 AI（Google DeepMind Antigravity）全自动自主构建与维护。**

<br/>

**AI TTS Android Engine** 是一个基于 Android 标准 `TextToSpeechService` 实现的系统级在线语音引擎，可将各大主流 AI 大模型语音接口接入为安卓系统默认 TTS，供「开源阅读 (Legado)」、「静读天下」及系统读屏等第三方应用直接调用。

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

- **三套 UI 范式自由切换**：支持「🚀 Bento 全息声球工作台」、「🎛️ DAW 专业调音台」与「📋 经典紧凑列表」三套交互布局。
- **10 套高对比专属主题**：深海曜蓝、极客翡翠、钛金极简、落日暖金、莫兰迪灰、赛博霓虹、极光薄荷、樱花幽粉、暗夜曜石、炽阳枫红，深浅模式层级分明。
- **真实 STFT 物理示波器**：纯内存 PCM 短时傅里叶变换与 32 频段 Mel 能量映射，实时呈现人声共鸣与物理重力回落阻尼。
- **文件级全量配置备份**：接入 SAF 框架支持直接导出与导入 `.json` 备份文件，无剪贴板容量限制。
- **流式并发预取**：后台异步预取下一句音频，消除长文本段落停顿。
- **小说多角色与情绪识别**：智能识别对话与旁白，支持多声线协同朗读与情绪微调提示。
- **发音规则清洗**：自定义正则替换规则、11 位手机号读“幺”、数字/金额规范化。
- **超轻量体积**：R8 全模式优化剪裁，安装包仅 **1.78 MB**。

---

## 💻 编译构建

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

## 📄 开源许可证

本项目基于 [Apache License 2.0](LICENSE) 协议开源。
