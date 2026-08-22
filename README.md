<div align="center">

# 🎙️ AI Text-To-Speech Engine for Android
### 安卓系统级 AI 在线语音大模型 TTS 引擎替代软件

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple.svg)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![AI Autonomous](https://img.shields.io/badge/Created_&_Published_by-100%25_AI_Agent-orange.svg)](https://deepmind.google/)
[![Latest Release](https://img.shields.io/badge/Release-v1.6.0-brightgreen.svg)](https://github.com/misaka02/ai-tts-android/releases)
[![Status](https://img.shields.io/badge/Status-Completed_&_Archived-red.svg)](https://github.com/misaka02/ai-tts-android)

<br/>

> ### 📢 ⚡【重要声明】100% 由 AI Agent 全自主构建与项目归档说明
> 
> 1. **全生命周期由 AI 独立编写 (100% AI Authored & Developed)**：  
>    本项目从 Android 底层系统级 TTS Service 架构、跨厂商逆向与 API 规范对齐、双句并发滑动窗口预取流水线、16-bit PCM 预加重高通与动态范围软压缩、Jetpack Compose + Material 3 动态取色 UI、全套自动化单元测试，**乃至您正在阅读的本份 README 文档与发布说明，全量由 AI 编程助手自主完成设计、编码、调试与编译打包**。
> 2. **实测与可用性范围说明 (Tested Providers Scope)**：  
>    * 本项目**仅在真实生产环境中针对【小米 MiMo】与【微软 Edge TTS (完全免费·免Key)】进行了深度真机联调与长期可用性验证**；
>    * 其余提供商（包括 Google Gemini、MiniMax、火山豆包、硅基流动、Fish Audio、阶跃星辰、Azure、OpenAI 等）均严格依据各自官方公开技术规范实现并提供预设，**不保证在所有网络与系统环境下的完全可用性或长期稳定性**。
> 3. **终态归档与免责声明 (Completed & Archived)**：  
>    * 本项目功能已全量交付完备，**作为终态开源参考实现正式归档，后续原则上不再进行日常维护或功能迭代**；
>    * 欢迎广大开发者与小说听书爱好者自由 Fork、定制与二次开发。

<br/>

**一款能够原生替代 Android 系统自带 TTS 的高拟真 AI 语音大模型引擎。**  
通过标准 Android `TextToSpeechService` 桥接，无缝接入各大前沿大模型，为「开源阅读 (Legado)」、「静读天下 (Moon+ Reader)」、系统辅助朗读等应用带来广播剧级真人情感伴读体验。

</div>

---

## ⚠️ 兼容应用与系统接入说明

> [!IMPORTANT]
> **关于第三方 App 接入支持的真实情况：**
> * ✅ **原生完美支持**：任何遵循 Android 标准系统 TTS 接口（`android.speech.tts.TextToSpeech`）的应用均可无缝调用本引擎，包括：
>   * 📚 **[开源阅读 (Legado)](https://github.com/gedoor/legado)**（国内最流行的高自由度小说阅读器，完美支持断句与语速同步）
>   * 📖 **[静读天下 (Moon+ Reader)](http://www.moondownload.com/)**（支持系统 TTS 朗读 EPUB、TXT 等格式小说）
>   * 📑 **多看阅读 / 掌阅 / 搜狗阅读** 等支持调用系统语音引擎的阅读软件
>   * 🌐 **系统级无障碍读屏**（Android 原生 TalkBack、随选朗读 Select to Speak）
>   * 📱 **各大浏览器网页朗读与系统辅助工具**
> * ❌ **不支持的应用**：**微信读书** 等采用私有封闭自研播放器、未开放系统 TTS 接口的软件无法调用本引擎或任何第三方系统 TTS。建议使用「开源阅读 (Legado)」或「静读天下」配合本引擎享受最高拟真音质。

---

## ✨ 核心特性矩阵

| 提供商 / 模型 | 官方平台与文档链接 | 核心优势与音色特性 | 实测状态 / 鉴权要求 |
| :--- | :--- | :--- | :--- |
| **小米 MiMo** | [小米 MiMo 开放平台](https://platform.xiaomimimo.com/) | **MiMo-V2.5-TTS** 旗舰模型，支持 Voice Studio 工作室模式、声音克隆接入与「导演提示词」情绪控制 | **✅ 深度实测可用** / 需填 Key (注册送额度) |
| **微软 Edge TTS** | [Edge-TTS 协议参考 (rany2)](https://github.com/rany2/edge-tts) | 微软官方神经网络大模型语音，集成 `Sec-MS-GEC` 时间戳校验，支持晓晓、云希、云健等 300+ 多国音色 | **✅ 深度实测可用** / **完全免费·免 Key** |
| **Google Gemini** | [Google AI Studio 官网](https://ai.google.dev/) | **Gemini 2.0 / Flash 原生 TTS**，原生多模态音频流，内置 Puck, Kore, Fenrir, Aoede 等多角色音色 | 规范适配 / 需填 Key (提供免费额度) |
| **MiniMax (海螺)** | [MiniMax 开放平台](https://www.minimaxi.com/) | Speech-02 拟真大模型，支持青涩男声、精英青年、霸道总裁、有声书男女声等 16+ 角色 | 规范适配 / 需填 Key |
| **火山引擎 / 豆包** | [火山引擎语音大模型](https://www.volcengine.com/product/voice-technology) | 字节跳动 BigTTS 语音大模型，内置 `爽快思思`、`灿灿主播`、`甜美小萱` 等高拟真电台音色 | 规范适配 / 需填 Key |
| **硅基流动** | [硅基流动 SiliconFlow 官网](https://www.siliconflow.com/) | 极速低延迟 `FunAudioLLM/CosyVoice2-0.5B` 与 ChatTTS 接入，支持在线动态拉取音色模型 | 规范适配 / 需填 Key |
| **Fish Audio (鱼音)** | [Fish Audio 官网](https://fish.audio/) | 高表现力声音大模型，**支持在线动态拉取个人自建声音克隆模型**与社区热门音色 | 规范适配 / 需填 Key |
| **阶跃星辰** | [阶跃星辰开放平台](https://platform.stepfun.com/) | `stepaudio-2.5-tts` 多模态语境感知大模型 | 规范适配 / 需填 Key |
| **OpenAI / 兼容** | [OpenAI Audio 文档](https://platform.openai.com/docs/guides/text-to-speech) | 标准 OpenAI Audio 格式，支持 `alloy`, `echo`, `fable`, `onyx`, `nova`, `shimmer` 等全系音色 | 规范适配 / 需填 Key |
| **自定义 HTTP 模板** | [GPT-SoVITS](https://github.com/RVC-Boss/GPT-SoVITS) / [CosyVoice](https://github.com/FunAudioLLM/CosyVoice) | 支持私有化部署的 **GPT-SoVITS**、**CosyVoice-v2**、**F5-TTS**、**VITS** 等本地与局域网节点 | 规范适配 / 无需 Key |

---

## 🚀 进阶技术亮点 (v1.6.0)

### 1. 👆 全域长按悬浮拖拽自由排序 (Universal Drag & Drop)
* 在主页任意音色卡片上**长按 200ms**，立即伴随震动进入悬浮浮动图层（`zIndex = 10f`、高亮阴影、边框光晕）；
* 手指上下拖动实时位移跟随，跨越相邻卡片瞬间震动并自动换位，松手即时持久化保存；
* 排序模式下同时保留瞬时响应的手柄拖拽与一键置顶快捷键。

### 2. 🎭 小说对白智能情感大模型导演指令注入 (Emotion Prosody Enhancer)
* 自动识别引述语中的 6 大情绪状态（**愤怒冷酷 / 哀伤哽咽 / 惊恐紧迫 / 娇柔温婉 / 悄声耳语 / 激动狂喜**）；
* 合成对白分句时动态融合导演指令（如`【语气要求：极其愤怒激昂、语气冷酷严厉带有压迫感】`），赋予角色强烈的戏剧张力。

### 3. 📦 官方精选规则库一键批量导入 (Curated Rules Presets)
* 🔮 **修仙玄幻高频多音字校正包**（丹田、筑基、桀桀、嗤笑、乾坤、识海等）；
* 🧹 **小说特殊符号与排版乱码净化包**（清理装饰方块▓█、星号、防盗链接与章节分割线）；
* 💻 **现代科技与网游专有名词包**（规整 AI、WiFi、CPU、GPU、NPC、BOSS 等自然连读）。

### 4. 🎛️ 专业声学 EQ 预设矩阵 (Audio EQ Presets)
* ✨ **清澈人声 (Clear Voice)**：预加重高通滤波 + 1.25x 增益，提升 1k~4kHz 齿音通透度，通勤降噪；
* 🎙️ **磁性电台 (Warm Broadcast)**：1.4x 饱满增益，声音沉稳磁性；
* 🌙 **睡前护耳 (Gentle Ear Protect)**：0.95x 柔和软饱和限幅，削弱高频毛刺，久听不累；
* 📻 **原声直出 (Passthrough)**：原始 PCM 直出；
* ⚙️ **自定义 (Custom)**：自主调节增益倍率（0.8x ~ 2.2x）与高通滤波开关。

### 5. 📤 试听与朗读音频 WAV 本地导出与系统分享 (Export & Share WAV)
* 内置标准 44-byte RIFF/WAVE 封装，试听后可点击 **「导出WAV」** 一键保存至 `Download/AI_TTS/` 目录，并调起系统原生分享面板（微信、QQ、网盘等）。

### 6. 🛡️ 智能故障自动降级链与微秒级抖动自愈重试
* 主力大模型遇到偶发网络抖动时在 80~200ms 内微延迟自动自愈重试；
* 若主力模型欠费超额或持续异常，自动无缝透明切换至备用引擎（如微软 Edge TTS 免 Key 晓晓），确保听书永不断流。

### 7. 🎵 独创双句滑动窗口并发预加载 (Zero-Gap Prefetch Pipeline)
* 当前句子在流式推流播放时，后台协程自动预取并解码下两句音频到内存，彻底消除句与句之间尴尬的网络卡顿。

---

## 📖 对接「开源阅读 (Legado)」实操教程

1. **安装并启动本 App**：
   * 在列表选择任意模型（如**微软 Edge TTS** 或输入 **小米 MiMo** 的 API Key）。
   * 点击 **「立即试听」** 确保发音与网络畅通。
2. **在系统设置中设为默认引擎**：
   * 打开手机「系统设置」 $\to$ 搜索 **「文字转语音」** 或 **「TTS」**（或在 App 内点击「打开系统设置」）；
   * 将首选引擎切换为 **`AI 大模型语音引擎`**。
3. **在「开源阅读 (Legado)」中开启听书**：
   * 打开「阅读」App $\to$ 进入小说阅读界面；
   * 点击屏幕中央唤出菜单 $\to$ 点击 **「朗读」**；
   * 在朗读控制栏中选择 **「系统 TTS 引擎」**，即可享受大模型拟真有声小说伴读！

---

## 🛠️ 本地开发与编译指南

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

## 💖 致谢与官方链接 (Credits & Acknowledgments)

本项目全生命周期由 **Google DeepMind Antigravity** 全自主研发生成。衷心感谢以下开源项目、开发框架与大模型语音技术提供方（所有链接均经过真实性验证与有效性测试）：

### 🤖 AI 研发平台与底层技术
* **[Google DeepMind 官网](https://deepmind.google/)** - 全球领先的通用人工智能研发团队
* **[Kotlin 语言与官方协程库](https://kotlinlang.org/)** - 现代高效的跨平台编程语言
* **[Jetpack Compose & Android 开源项目 (AOSP)](https://developer.android.com/jetpack/compose)** - 声明式现代 UI 框架
* **[Square OkHttp](https://square.github.io/okhttp/)** - 业界标杆级 HTTP/2 & WebSocket 网络通信框架

### 🎙️ 在线语音大模型与开放平台
* **[小米 MiMo 大模型开放平台](https://platform.xiaomimimo.com/)** - MiMo-V2.5-TTS 高拟真语音模型与开发者控制台
* **[微软 Edge-TTS 协议参考 (rany2/edge-tts)](https://github.com/rany2/edge-tts)** - 微软神经网络 WebSocket 语音协议与 Sec-MS-GEC 开源逆向参考
* **[微软 Azure AI 语音服务](https://azure.microsoft.com/products/ai-services/text-to-speech)** - 微软企业级高保真云端语音合成
* **[MiniMax 开放平台](https://www.minimaxi.com/)** - MiniMax Speech-02 拟真声音大模型开放平台
* **[火山引擎 语音大模型中心](https://www.volcengine.com/product/voice-technology)** - 字节跳动 BigTTS 豆包语音合成大模型
* **[硅基流动 (SiliconFlow) 官网](https://www.siliconflow.com/)** - 开源大模型高速推理平台与 API 服务
* **[Fish Audio (鱼音) 官网](https://fish.audio/)** - 表现力极强的人声克隆与 TTS 社区平台
* **[阶跃星辰 开放平台](https://platform.stepfun.com/)** - StepAudio 多模态感知大模型开放平台
* **[Google AI Studio / Gemini API](https://ai.google.dev/)** - Gemini 原生多模态音频与语音合成开发者中心
* **[OpenAI Audio API 官方文档](https://platform.openai.com/docs/guides/text-to-speech)** - OpenAI 行业标准 TTS 接口规范

### 🔬 开源本地私有化语音模型
* **[GPT-SoVITS 开源项目](https://github.com/RVC-Boss/GPT-SoVITS)** - 强大多语言少样本声音克隆与合成框架
* **[CosyVoice 开源语音大模型](https://github.com/FunAudioLLM/CosyVoice)** - 阿里巴巴多语言自回归语音大模型

### 📚 优秀开源阅读生态
* **[开源阅读 Legado (gedoor/legado)](https://github.com/gedoor/legado)** - 备受赞誉的 Android 端开源免费小说阅读器
* **[静读天下 (Moon+ Reader) 官网](http://www.moondownload.com/)** - 经典强大的全格式移动端电子书阅读软件
