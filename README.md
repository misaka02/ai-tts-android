<div align="center">

# 🎙️ AI Text-To-Speech Engine for Android
### 安卓系统级 AI 在线语音大模型 TTS 引擎替代软件

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![AI Generated](https://img.shields.io/badge/Created_by-AI_Autonomous_Agent-orange.svg)](https://deepmind.google)
[![GitHub release](https://img.shields.io/github/v/release/your-username/ai-tts-android?include_prereleases&color=brightgreen)](https://github.com)

**一款能够替代 Android 系统原生 TTS（文字转语音）引擎的开源神器。**  
通过底层标准 Android `TextToSpeechService` 桥接，无缝接入市面上顶尖的在线 AI 语音大模型，为「开源阅读 (Legado)」、微信读书、静读天下、系统朗读等提供电影级、真人情感饱满的拟真语音合成体验。

---

> 🤖 **关于本项目 (AI 创作与自主发布声明)**：  
> 本项目从底层 Android 系统架构设计、多模型协议逆向与对齐、音频编解码管线、UI 交互（Jetpack Compose + Material 3）、测试套件到 GitHub CI/CD 流水线，**全部由 AI 编程智能体自主交互、编写、验证并自动化发布构建**。

</div>

---

## ✨ 核心特性矩阵

| 提供商 / 模型 | 官方规范端点 | 核心优势与音色特性 | 免费 / API Key |
| :--- | :--- | :--- | :--- |
| **小米 MiMo** | `api.xiaomimimo.com/v1/chat/completions` | **MiMo-V2.5-TTS** 旗舰大模型，支持「导演模式 (Director Mode)」自然语言控制语速/音调/情感，内置 `茉莉`、`冰糖`、`苏打`、`白桦` 等官方音色 | 需填 Key (有免费额度) |
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

### 1. 🎬 大模型「导演模式 (Director Mode)」智能指令编译器
针对 Xiaomi MiMo、Gemini 等新一代生成式语音大模型不接受传统 DSP 语速音调数字参数的特性，本引擎内置了**导演指令编译系统**：
* 拖动语速/音调滑动条时，引擎会自动将其转换为自然语言指令（如 `“语速稍快，轻快生动，音调偏清脆明亮”`）；
* 配合自定义提示词输入（如 `“用温柔知性的语气朗读，情感丰富细腻，适合言情小说”`），在 `role: user` 层面下发导演指令，由大模型端到端生成具有极高表现力的声音。

### 2. ⚡ 低延迟智能长句切分与缓存管线
* **智能断句分块**：基于正则表达式识别逗号、句号、引号、对话与段落边界，切分为自然短句流式请求，首字发音延迟低至毫秒级。
* **发音多音字与特殊符号清洗规则**：内置多音字校正（如“重庆”、“银行”）、长省略号转换为自然停顿、过滤小说中常见的特殊排版括号。

### 3. 🛡️ 稳健的系统级 TextToSpeechService 桥接
* 严格遵循 Android Framework 的 `SynthThread` 运行机制，采用同步阻塞式推流与生命周期管控，避免异步过早退出导致的引擎断音或无声。
* 封装基于 FileDescriptor 的 `MediaCodec` 解码器与原生 PCM/WAV 封包技术，在各大国产定制 ROM（MIUI/HyperOS、ColorOS、OriginOS、HarmonyOS）上均能稳定工作。

---

## 📱 界面预览与功能截图

- 🏠 **仪表盘与即时试听**：一键切换当前全局激活的模型、测试短句并测定端到端合成延迟。
- ⚙️ **模型编辑与一键预设**：一键「自动重置为官方默认参数与地址」，小白也能 1 秒完成配置。
- 🎭 **可视化音色选择器**：支持按角色、性别、语言、适用场景搜索并在线实时同步云端模型。
- 🔧 **发音正则规则管理**：自由添加、启用、导出与备份文本发音替换规则。

---

## 📖 对接「阅读 (Legado)」等小说软件教程

1. **安装并打开本 App**：
   * 选择任意模型（如**微软 Edge TTS** 或输入 **小米 MiMo / Gemini** 的 Key）。
   * 点击底部 **「测试此模型配置」**，确保能正常听到声音。
2. **在系统设置中设为默认引擎**：
   * 打开手机「系统设置」 $\to$ 搜索 **「文字转语音」** 或 **「TTS」**；
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

本项目采用 [Apache License 2.0](LICENSE) 开源许可证。欢迎提交 Issue、Pull Request 与 Star 支持！
