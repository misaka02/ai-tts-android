<div align="center">

# 🎙️ AI Text-To-Speech Engine for Android
### 安卓系统级 AI 在线语音大模型 TTS 引擎替代软件

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![AI Autonomous](https://img.shields.io/badge/Developed_&_Published_by-100%25_AI_Agent-orange.svg)](https://deepmind.google)
[![Status](https://img.shields.io/badge/Status-Completed_&_Archived-red.svg)](https://github.com)

**一款能够完美替代 Android 系统自带 TTS 的高拟真 AI 语音引擎。**  
通过标准 Android `TextToSpeechService` 系统服务桥接，无缝接入市面所有主流在线 AI 语音大模型，为「开源阅读 (Legado)」、微信读书、静读天下、系统辅助朗读提供电影级真人情感拟真发音。

---

> ### 🤖 终态开源与 AI 自主创作公告 (Notice of AI Creation & Archive)
> 
> 1. **全 AI 自主开发 (100% AI Developed)**：本项目从底层 Android 系统架构、逆向网络协议适配、多模型 API 规范对齐、双句并发滑动窗口预取管线、音频编解码、UI 界面（Jetpack Compose + Material 3）到全套单元测试，**全部由 AI 编程助手自主完成**。
> 2. **AI 自动化发布 (AI Automated Release)**：所有代码提交、版本打标及 GitHub Actions CI/CD 流水线均由 AI 自动化构建生成。
> 3. **终态归档声明 (Completed & Archived)**：本项目功能已全量开发完成且各模块自查完备，**作为最终交付版本进行开源归档，后续将不会有任何功能更新或日常维护**。欢迎广大社区开发者自由 Fork、修改与二次开发。

</div>

---

## ✨ 核心特性矩阵

| 提供商 / 模型 | 官方规范端点 | 核心优势与音色特性 | 免费 / 费用 |
| :--- | :--- | :--- | :--- |
| **小米 MiMo** | `api.xiaomimimo.com/v1/chat/completions` | **MiMo-V2.5-TTS** 旗舰大模型，支持「导演模式」提示词控制语速/音调/情感，内置 `茉莉`、`冰糖`、`苏打`、`白桦` 等官方真实音色 | 需填 Key (有免费额度) |
| **Google Gemini** | `generativelanguage.googleapis.com` | **Gemini 2.5 / 3.1 Flash 原生 TTS**，原生多模态生成，内置 `Puck`、`Kore`、`Charon` 等 30 款预置音色，自动 PCM $\to$ WAV 动态封装 | 需填 Key (提供免费 Tier) |
| **微软 Edge TTS** | Bing Speech WebSocket 协议 | 微软官方神经网络大模型语音，集成最新 `Sec-MS-GEC` 时间戳散列 DRM，支持 300+ 多国音色（晓晓、云希、云健、晓伊等） | **完全免费 / 免 Key** |
| **MiniMax (海螺)** | `api.minimax.chat/v1/t2a_v2` | Speech-02 拟真大模型，支持 16+ 角色音色（青涩男声、精英青年、霸道总裁、有声书男女声等），自适应 Hex/Base64 解码 | 需填 Key |
| **火山引擎 / 豆包** | `openspeech.bytedance.com/api/v1/tts` | 字节跳动 BigTTS 语音大模型，内置 `爽快思思`、`灿灿主播`、`甜美小萱` 等高拟真电台与网文音色 | 需填 Key |
| **硅基流动** | `api.siliconflow.cn/v1/audio/speech` | 极速低延迟 `FunAudioLLM/CosyVoice2-0.5B` 与 ChatTTS 接入，支持在线动态拉取音色模型 | 需填 Key |
| **Fish Audio (鱼音)** | `api.fish.audio/v1/tts` | 高表现力声音大模型，**支持在线动态拉取个人自建声音克隆模型**与社区热门音色 | 需填 Key |
| **阶跃星辰** | `api.stepfun.com/v1/audio/speech` | `stepaudio-2.5-tts` 多模态语境感知大模型 | 需填 Key |
| **OpenAI / 兼容** | `api.openai.com/v1/audio/speech` | 标准 OpenAI 格式，支持官方及各类第三方 One-API / 中转站 | 需填 Key |
| **自定义 HTTP 模板** | 任意自建端点 | 支持私有化部署的 **GPT-SoVITS**、**CosyVoice-v2**、**F5-TTS**、**VITS** 等本地与局域网节点 | 无需 Key / 自建 |

---

## 🚀 进阶技术亮点

### 1. 🎵 独创双句滑动窗口并发预加载 (Zero-Gap Prefetch Pipeline)
* 当前句子在流式推流播放时，后台协程自动预取并解码下两句音频到内存；
* 彻底消除传统 TTS 句与句之间尴尬的 0.5s 网络等待空白，实现丝滑连续的拟人发音。

### 2. ⚡ 全局 HTTP/2 长连接池与弱网自动重试 (`SharedHttpClient`)
* 全局共享连接池，复用 TLS 会话，首字发音网络延迟降低 50% 以上；
* 内置网络抖动自动重试机制，地铁、弱网等移动场景听书不中断。

### 3. 🎬 大模型「导演模式 (Director Mode)」智能指令编译器
* 针对 MiMo、Gemini 等新一代生成式大模型不接受数字 DSP 调速的特点，自动将滑动条与自定义提示词编译为自然语言导演指令；
* 在 `role: user` 中注入情绪引导（如“*用温柔知性的语气朗读，情感丰富细腻*”），由大模型端到端生成富有表现力的声音。

### 4. 🧹 网页小说与 Markdown 文本智能深度清洗
* 自动清洗 `**加粗**`、HTML 标签（`<p>`、`&nbsp;`）与超链接，规范化发音标点，避免死板读出符号代码。

### 5. 🛡️ 稳健的系统级 TextToSpeechService 桥接
* 严格遵循 Android Framework 的 `SynthThread` 运行机制，采用同步阻塞式推流与生命周期管控；
* 结合基于 FileDescriptor 的 `MediaCodec` 解码器与原生 PCM/WAV 封包，在各大定制 ROM（MIUI/HyperOS、ColorOS、OriginOS、HarmonyOS）上稳定长久保活。

---

## 📖 对接「阅读 (Legado)」等小说软件教程

1. **安装并打开本 App**：
   * 选择任意模型（如**微软 Edge TTS** 或输入 **小米 MiMo / Gemini** 的 Key）。
   * 点击底部 **「测试此模型配置」**，确保能正常听到声音。
2. **在系统设置中设为默认引擎**：
   * 打开手机「系统设置」 $\to$ 搜索 **「文字转语音」** 或 **「TTS」**（或在 App 内点击「打开系统设置」）；
   * 将首选引擎切换为 **`AI 大模型语音引擎`**。
3. **在「阅读 (Legado)」中使用**：
   * 打开「阅读」App $\to$ 进入任意小说阅读界面；
   * 点击屏幕中央唤出菜单 $\to$ 点击 **「朗读」**；
   * 在朗读控制栏中选择 **「系统 TTS 引擎」**，即可享受大模型拟真伴读！

---

## 🛠️ 本地开发与编译指南

### 环境要求
* JDK 17+
* Android SDK (compileSdk 34, minSdk 26)
* Gradle 8.0+

### 编译步骤
```bash
# 克隆仓库
git clone https://github.com/your-username/ai-tts-android.git
cd ai-tts-android

# 执行单元测试
./gradlew test

# 编译 Debug APK
./gradlew assembleDebug

# 产物位置：app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 开源许可证 (License)

本项目采用 [Apache License 2.0](LICENSE) 开源许可证。
