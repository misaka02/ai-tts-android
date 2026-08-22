<div align="center">

# 🎙️ AI Text-To-Speech Engine for Android
### 安卓系统级 AI 在线语音大模型 TTS 引擎

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![AI Autonomous](https://img.shields.io/badge/Created_by-100%25_AI_Agent-orange.svg)](https://deepmind.google/)
[![Latest Release](https://img.shields.io/badge/Release-v1.6.0-brightgreen.svg)](https://github.com/misaka02/ai-tts-android/releases)
[![Status](https://img.shields.io/badge/Status-Completed_&_Archived-red.svg)](https://github.com/misaka02/ai-tts-android)

<br/>

> ### 📢【项目声明】全生命周期由 AI Agent 独立构建
> * **100% AI 独立研发**：本项目从系统级 `TextToSpeechService` 架构、大模型协议适配、并发流式预取、PCM 声学 EQ 滤波、Compose UI 到测试与文档，全量由 AI 编程助手自主完成设计与开发。
> * **实测支持范围**：真实生产环境已对【小米 MiMo】与【微软 Edge TTS（免 Key 直连）】完成真机深度联调；其余厂商基于公开技术规范适配。
> * **项目状态**：作为终态开源参考实现归档，欢迎社区自由 Fork 与二次开发。

</div>

---

## 📖 快速上手与支持应用

本引擎基于 Android 官方标准 `android.speech.tts.TextToSpeech` 体系开发，所有支持调用系统 TTS 的应用均可无缝接入（如 **[开源阅读 (Legado)](https://github.com/gedoor/legado)**、**[静读天下 (Moon+ Reader)](http://www.moondownload.com/)**、多看阅读、掌阅、系统 TalkBack 读屏等）：

1. **配置引擎**：打开本 App，选择目标模型（如完全免费免 Key 的**微软 Edge TTS** 或输入 **小米 MiMo** 等 API Key），点击 **「立即试听」** 确认发音正常；
2. **设为默认**：打开手机「系统设置」 $\to$ 搜索 **「文字转语音」/「TTS」** $\to$ 将首选引擎切换为 **`AI 大模型语音引擎`**；
3. **开始听书**：在「开源阅读」等软件中打开小说，进入朗读控制栏选择 **「系统 TTS 引擎」** 即可享受真人拟真发音。

---

## ✨ 核心特性矩阵

| 模型 / 服务商 | 官方平台与文档 | 核心优势与音色特性 | 鉴权与配置 |
| :--- | :--- | :--- | :--- |
| **小米 MiMo** | [小米 MiMo 开放平台](https://platform.xiaomimimo.com/) | **MiMo-V2.5-TTS** 旗舰大模型，支持 Voice Studio 工作室、自建克隆音色与情绪控制 | 需填写 API Key |
| **微软 Edge TTS** | [Edge-TTS 协议参考](https://github.com/rany2/edge-tts) | 微软神经网络大模型语音，集成 `Sec-MS-GEC` 校验，支持晓晓、云希等 300+ 多国音色 | **完全免费 · 免 Key 直连** |
| **Google Gemini** | [Google AI Studio](https://ai.google.dev/) | **Gemini 2.0 / Flash 原生 TTS**，原生多模态音频流，内置 Puck, Kore, Fenrir 等音色 | 需填写 API Key |
| **MiniMax (海螺)** | [MiniMax 开放平台](https://www.minimaxi.com/) | Speech-02 拟真大模型，支持青涩男声、精英青年、霸道总裁、有声书等 16+ 音色 | 需填写 API Key |
| **火山引擎 / 豆包** | [火山引擎语音大模型](https://www.volcengine.com/product/voice-technology) | 字节跳动 BigTTS 语音大模型，内置爽快思思、灿灿主播、甜美小萱等高拟真音色 | 需填写 API Key |
| **硅基流动** | [硅基流动 SiliconFlow](https://www.siliconflow.com/) | 极速低延迟 `FunAudioLLM/CosyVoice2` 与 ChatTTS 接入，支持在线动态拉取模型 | 需填写 API Key |
| **Fish Audio (鱼音)** | [Fish Audio 官网](https://fish.audio/) | 高表现力声音大模型，支持在线动态拉取个人自建声音克隆模型与社区热门音色 | 需填写 API Key |
| **阶跃星辰** | [阶跃星辰开放平台](https://platform.stepfun.com/) | `stepaudio-2.5-tts` 多模态语境感知大模型 | 需填写 API Key |
| **OpenAI / 兼容** | [OpenAI Audio API](https://platform.openai.com/docs/guides/text-to-speech) | 标准 OpenAI Audio 格式，支持 `alloy`, `echo`, `fable`, `onyx`, `nova` 等全系音色 | 需填写 API Key |
| **自定义 HTTP 节点** | [GPT-SoVITS](https://github.com/RVC-Boss/GPT-SoVITS) / [CosyVoice](https://github.com/FunAudioLLM/CosyVoice) | 支持私有化部署的 **GPT-SoVITS**、**CosyVoice-v2**、**F5-TTS**、**VITS** 等本地与局域网节点 | 无需 Key / 填入自建地址 |

---

## 🚀 核心技术亮点

* ⚡ **双句滑动窗口并发预取 (Zero-Gap Pipeline)**：播放当前句时后台协程自动预取并解码后续段落音频，彻底消除网络请求带来的句间卡顿空白。
* 🎭 **小说智能情感与多角色驱动 (Emotion & Multi-Role)**：依据引述语智能识别 6 类情绪（愤怒、哀伤、惊恐、温婉、耳语、狂喜）并注入导演指令；支持旁白、男主、女主音色自动分流。
* 🎛️ **软件级 PCM 声学 EQ 滤波 (Audio EQ Enhancer)**：预加重高通滤波提升人声齿音通透度，双曲正切软饱和动态压缩防爆音，内置清澈人声/磁性电台/睡前护耳预设。
* 🛡️ **双级容灾降级与自愈重试 (Smart Failover)**：网络抖动毫秒级重试自愈，主力模型异常时无缝透明切换至备用引擎（如微软 Edge TTS 免 Key），确保听书永不断流。
* 🔢 **网文发音纠错与规则引擎 (Rules Engine)**：内置生僻字纠音库、英文缩写自然连读（AI、WiFi、CPU 等）、排版乱码过滤，并兼容「开源阅读」替换规则 JSON 导入。

---

## 🛠️ 本地开发与编译

```bash
# 1. 克隆仓库
git clone https://github.com/misaka02/ai-tts-android.git
cd ai-tts-android

# 2. 执行全量单元测试 (40+ 测试用例)
./gradlew test

# 3. 构建 Release 生产包 (11.5MB，120 FPS 满帧编译)
./gradlew assembleRelease
```

---

## 💖 致谢与开源参考 (Credits & Acknowledgments)

本项目全生命周期由 **Google DeepMind Antigravity** 全自主研发生成。特别鸣谢以下项目与服务支持：

* **大模型与语音服务**：[小米 MiMo](https://platform.xiaomimimo.com/) · [微软 Edge-TTS](https://github.com/rany2/edge-tts) · [微软 Azure 语音](https://azure.microsoft.com/products/ai-services/text-to-speech) · [MiniMax](https://www.minimaxi.com/) · [火山引擎豆包](https://www.volcengine.com/product/voice-technology) · [硅基流动](https://www.siliconflow.com/) · [Fish Audio](https://fish.audio/) · [阶跃星辰](https://platform.stepfun.com/) · [Google Gemini](https://ai.google.dev/) · [OpenAI](https://platform.openai.com/docs/guides/text-to-speech)
* **本地模型与阅读生态**：[GPT-SoVITS](https://github.com/RVC-Boss/GPT-SoVITS) · [CosyVoice](https://github.com/FunAudioLLM/CosyVoice) · [开源阅读 (Legado)](https://github.com/gedoor/legado) · [静读天下 (Moon+ Reader)](http://www.moondownload.com/)
* **开发平台与基础框架**：[Google DeepMind](https://deepmind.google/) · [Android Open Source Project](https://developer.android.com/jetpack/compose) · [Kotlin](https://kotlinlang.org/) · [Square OkHttp](https://square.github.io/okhttp/)
