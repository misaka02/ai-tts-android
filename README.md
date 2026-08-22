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
> 
> * **100% AI 独立研发**：本项目从底层 Android `TextToSpeechService` 架构、多模型协议适配、双句并发预取流水线、PCM 声学 EQ 滤波、Jetpack Compose 界面到全套自动化测试，全量由 AI 编程助手自主完成设计与开发。
> * **实测支持范围**：真实生产环境已对【小米 MiMo】与【微软 Edge TTS（免 Key）】进行真机深度联调；其余模型基于各厂商公开规范实现。
> * **开源状态**：作为终态开源参考实现归档，欢迎社区自由 Fork 与定制。

<br/>

**一款原生替代 Android 系统自带 TTS 的高拟真 AI 语音大模型引擎。**  
通过标准 Android `TextToSpeechService` 接口，无缝接入主流在线与本地大模型，为小说阅读及系统读屏提供广播剧级真人拟真发音。

</div>

---

## 📱 系统接入与支持应用

本引擎基于 Android 官方标准 `android.speech.tts.TextToSpeech` 体系构建，所有支持调用系统 TTS 的应用均可直接使用：

* 📚 **[开源阅读 (Legado)](https://github.com/gedoor/legado)**：主流开源免费网络小说阅读器，完美支持断句流式伴读与语速同步
* 📖 **[静读天下 (Moon+ Reader)](http://www.moondownload.com/)**：经典全格式本地电子书阅读器（EPUB / TXT / PDF）
* 📑 **多看阅读 / 掌阅 / 搜狗阅读** 等支持调用系统语音引擎的阅读软件
* 🌐 **Android 系统无障碍读屏**（TalkBack、随选朗读 Select to Speak）与浏览器网页朗读

---

## ✨ 核心特性矩阵

| 提供商 / 模型 | 官方平台与文档 | 核心优势与音色特性 | 鉴权与配置 |
| :--- | :--- | :--- | :--- |
| **小米 MiMo** | [小米 MiMo 开放平台](https://platform.xiaomimimo.com/) | **MiMo-V2.5-TTS** 旗舰大模型，支持 Voice Studio 工作室模式、自建克隆声音与「导演提示词」情绪控制 | 需填写 API Key |
| **微软 Edge TTS** | [Edge-TTS 协议参考](https://github.com/rany2/edge-tts) | 微软官方神经网络语音，集成 `Sec-MS-GEC` 时间戳散列，支持晓晓、云希、云健等 300+ 多国音色 | **完全免费 · 免 Key 直连** |
| **Google Gemini** | [Google AI Studio](https://ai.google.dev/) | **Gemini 2.0 / Flash 原生 TTS**，原生多模态音频流，内置 Puck, Kore, Fenrir, Aoede 等多角色音色 | 需填写 API Key |
| **MiniMax (海螺)** | [MiniMax 开放平台](https://www.minimaxi.com/) | Speech-02 拟真大模型，支持青涩男声、精英青年、霸道总裁、有声书男女声等 16+ 角色 | 需填写 API Key |
| **火山引擎 / 豆包** | [火山引擎语音大模型](https://www.volcengine.com/product/voice-technology) | 字节跳动 BigTTS 语音大模型，内置 `爽快思思`、`灿灿主播`、`甜美小萱` 等高拟真电台音色 | 需填写 API Key |
| **硅基流动** | [硅基流动 SiliconFlow](https://www.siliconflow.com/) | 极速低延迟 `FunAudioLLM/CosyVoice2-0.5B` 与 ChatTTS 接入，支持在线动态拉取音色模型 | 需填写 API Key |
| **Fish Audio (鱼音)** | [Fish Audio 官网](https://fish.audio/) | 高表现力声音大模型，支持在线动态拉取个人自建声音克隆模型与社区热门音色 | 需填写 API Key |
| **阶跃星辰** | [阶跃星辰开放平台](https://platform.stepfun.com/) | `stepaudio-2.5-tts` 多模态语境感知大模型 | 需填写 API Key |
| **OpenAI / 兼容** | [OpenAI Audio API](https://platform.openai.com/docs/guides/text-to-speech) | 标准 OpenAI Audio 格式，支持 `alloy`, `echo`, `fable`, `onyx`, `nova`, `shimmer` 等全系音色 | 需填写 API Key |
| **自定义 HTTP 模板** | [GPT-SoVITS](https://github.com/RVC-Boss/GPT-SoVITS) / [CosyVoice](https://github.com/FunAudioLLM/CosyVoice) | 支持私有化部署的 **GPT-SoVITS**、**CosyVoice-v2**、**F5-TTS**、**VITS** 等本地与局域网节点 | 无需 Key / 填入自建地址 |

---

## 🚀 核心技术亮点

### 1. ⚡ 双句滑动窗口并发流式预取 (Zero-Gap Pipeline)
当前句子在流式推流播放时，后台协程自动并发预取并解码后续段落音频，彻底消除大模型网络请求导致的句间停顿空白，实现丝滑连续的拟人发音。

### 2. 🎭 小说对白智能情感与多角色驱动 (Emotion & Multi-Role)
* **语境情绪感知**：自动识别引述语中的 6 类情绪（愤怒、哀伤、惊恐、温婉、耳语、狂喜），动态向大模型注入导演控制指令；
* **角色音色分流**：智能区分小说对白与旁白叙述，支持分别指定旁白、男主、女主音色。

### 3. 🎛️ 软件级 PCM 声学 EQ 与人声增强 (Audio EQ Enhancer)
* **清澈人声滤波**：预加重高通滤波提升 1k~4kHz 人声齿音通透度，削弱手机扬声器低频浑浊；
* **软饱和动态压缩**：双曲正切软压缩放大微弱对白且极大音量防破音，提供「清澈人声 / 磁性电台 / 睡前护耳」等场景预设。

### 4. 🛡️ 双级容灾降级与网络抖动自愈 (Smart Failover)
遇偶发网络抖动时毫秒级微延迟自动重试自愈；主力模型欠费超额或持续异常时，无缝透明切换至备用引擎（如微软 Edge TTS 免 Key 晓晓），确保听书永不断流。

### 5. 🔢 网文文本净化与发音规则引擎 (Rules Engine)
内置修仙玄幻生僻字纠音库、英文缩写自然连读规整器（AI、WiFi、CPU、NPC 等）、排版特殊符号过滤，并支持一键导入「开源阅读」JSON 替换规则。

---

## 📖 对接「开源阅读 (Legado)」使用步骤

1. **安装并启动本 App**：选择目标模型（如**微软 Edge TTS** 或输入 **小米 MiMo** 的 API Key），点击 **「立即试听」** 确保发音正常。
2. **设为系统默认引擎**：在手机「系统设置」中搜索 **「文字转语音」** 或 **「TTS」**，将首选引擎切换为 **`AI 大模型语音引擎`**。
3. **在「开源阅读」中朗读**：在小说阅读界面唤出菜单 $\to$ 点击 **「朗读」** $\to$ 引擎选择 **「系统 TTS 引擎」** 即可。

---

## 🛠️ 本地开发与编译

```bash
# 克隆仓库
git clone https://github.com/misaka02/ai-tts-android.git
cd ai-tts-android

# 执行全量单元测试 (40+ 测试用例)
./gradlew test

# 构建 Release 生产包 (11.5MB，120 FPS 满帧编译)
./gradlew assembleRelease
```

---

## 💖 致谢与官方链接 (Credits & Acknowledgments)

本项目全生命周期由 **Google DeepMind Antigravity** 全自主研发生成。衷心感谢以下开源项目、开发框架与大模型语音技术提供方：

### 🤖 AI 研发平台与底层技术
* **[Google DeepMind](https://deepmind.google/)** - 通用人工智能研发团队
* **[Kotlin 语言与官方协程库](https://kotlinlang.org/)** - 现代高效的跨平台编程语言
* **[Jetpack Compose & Android 开源项目 (AOSP)](https://developer.android.com/jetpack/compose)** - 声明式现代 UI 框架
* **[Square OkHttp](https://square.github.io/okhttp/)** - 标杆级 HTTP/2 & WebSocket 网络通信框架

### 🎙️ 在线语音大模型与开放平台
* **[小米 MiMo 大模型开放平台](https://platform.xiaomimimo.com/)** - MiMo-V2.5-TTS 高拟真语音模型
* **[微软 Edge-TTS 协议参考 (rany2/edge-tts)](https://github.com/rany2/edge-tts)** - 微软神经网络 WebSocket 语音协议开源逆向参考
* **[微软 Azure AI 语音服务](https://azure.microsoft.com/products/ai-services/text-to-speech)** - 微软企业级高保真云端语音合成
* **[MiniMax 开放平台](https://www.minimaxi.com/)** - MiniMax Speech-02 拟真声音大模型
* **[火山引擎 语音大模型中心](https://www.volcengine.com/product/voice-technology)** - 字节跳动 BigTTS 豆包语音合成大模型
* **[硅基流动 (SiliconFlow)](https://www.siliconflow.com/)** - 开源大模型高速推理平台与 API 服务
* **[Fish Audio (鱼音)](https://fish.audio/)** - 表现力极强的人声克隆与 TTS 社区平台
* **[阶跃星辰 开放平台](https://platform.stepfun.com/)** - StepAudio 多模态感知大模型开放平台
* **[Google AI Studio / Gemini API](https://ai.google.dev/)** - Gemini 原生多模态音频与语音合成开发者中心
* **[OpenAI Audio API 官方文档](https://platform.openai.com/docs/guides/text-to-speech)** - OpenAI 行业标准 TTS 接口规范

### 🔬 开源本地私有化语音模型
* **[GPT-SoVITS 开源项目](https://github.com/RVC-Boss/GPT-SoVITS)** - 强大多语言少样本声音克隆与合成框架
* **[CosyVoice 开源语音大模型](https://github.com/FunAudioLLM/CosyVoice)** - 阿里巴巴多语言自回归语音大模型

### 📚 推荐移动阅读软件
* **[开源阅读 Legado (gedoor/legado)](https://github.com/gedoor/legado)** - 备受赞誉的 Android 端开源免费小说阅读器
* **[静读天下 (Moon+ Reader)](http://www.moondownload.com/)** - 经典强大的全格式移动端电子书阅读软件
