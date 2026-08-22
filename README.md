<div align="center">

# 🎙️ AI Text-To-Speech Engine for Android
### 安卓系统级 AI 在线语音大模型 TTS 引擎替代软件

[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![AI Autonomous](https://img.shields.io/badge/Created_&_Published_by-100%25_AI_Agent-orange.svg)](https://deepmind.google)
[![Status](https://img.shields.io/badge/Status-Completed_&_Archived-red.svg)](https://github.com/misaka02/ai-tts-android)

**一款能够完美替代 Android 系统原生 TTS 的高拟真 AI 语音引擎。**  
通过标准 Android `TextToSpeechService` 系统服务桥接，无缝接入市面主流在线 AI 语音大模型，为「开源阅读 (Legado)」、微信读书、静读天下、系统辅助朗读提供广播剧级真人情感拟真发音。

---

> ### 🤖 关于本项目与 AI 创作声明 (Notice of AI Creation & Disclaimer)
> 
> 1. **全 AI 编写与生成 (100% AI Authored)**：本项目从底层 Android 系统架构、逆向网络协议适配、多模型 API 规范对齐、双句并发滑动窗口预取管线、音频编解码、UI 界面（Jetpack Compose + Material 3）、全套自动化测试，**包括您正在阅读的本份 README 文档本身，全量由 AI 编程助手自主设计、撰写与构建**。
> 2. **实测与可用性范围声明 (Tested Providers Scope)**：
>    * 本项目**仅在真实生产环境中针对【小米 MiMo】与【微软 Edge TTS】进行过深度联调与真机可用性验证**；
>    * 其余厂商及自定义节点（包括 Google Gemini、MiniMax、火山豆包、硅基流动、Fish Audio、StepFun、Azure、GPT-SoVITS 等）均严格依据各自官方公开规范编写并提供预设，**不保证在所有环境下的完全可用性、稳定性或长期有效性**。
> 3. **终态归档与免责声明 (Completed & Archived)**：
>    * 本项目功能已交付完备，**作为终态开源项目归档，后续将不会有任何功能更新、版本迭代或日常维护**；
>    * 欢迎广大社区开发者自由 Fork、修改与二次开发。

</div>

---

## ✨ 核心特性矩阵

| 提供商 / 模型 | 官方规范端点 | 核心优势与音色特性 | 实测状态 / 鉴权要求 |
| :--- | :--- | :--- | :--- |
| **小米 MiMo** | `api.xiaomimimo.com/v1/chat/completions` | **MiMo-V2.5-TTS** 旗舰大模型，内置 Voice Studio 工作室模式（支持标准合成、音色设计模版、个人克隆声音接入），支持「导演模式」情绪控制 | **✅ 深度实测可用** / 需填 Key (有免费额度) |
| **微软 Edge TTS** | Bing Speech WebSocket 协议 | 微软官方神经网络大模型语音，集成最新 `Sec-MS-GEC` 时间戳散列 DRM，支持 300+ 多国音色（晓晓、云希、云健、晓伊等） | **✅ 深度实测可用** / **完全免费·免 Key** |
| **Google Gemini** | `generativelanguage.googleapis.com` | **Gemini 2.5 / 3.1 Flash 原生 TTS**，原生多模态生成，内置 `Puck`、`Kore`、`Charon` 等 30 款预置音色，自动 PCM $\to$ WAV 动态封装 | 规范适配 / 需填 Key (提供免费 Tier) |
| **MiniMax (海螺)** | `api.minimax.chat/v1/t2a_v2` | Speech-02 拟真大模型，支持 16+ 角色音色（青涩男声、精英青年、霸道总裁、有声书男女声等），自适应动态音调调节 | 规范适配 / 需填 Key |
| **火山引擎 / 豆包** | `openspeech.bytedance.com/api/v1/tts` | 字节跳动 BigTTS 语音大模型，内置 `爽快思思`、`灿灿主播`、`甜美小萱` 等高拟真电台与网文音色 | 规范适配 / 需填 Key |
| **硅基流动** | `api.siliconflow.cn/v1/audio/speech` | 极速低延迟 `FunAudioLLM/CosyVoice2-0.5B` 与 ChatTTS 接入，支持在线动态拉取音色模型 | 规范适配 / 需填 Key |
| **Fish Audio (鱼音)** | `api.fish.audio/v1/tts` | 高表现力声音大模型，**支持在线动态拉取个人自建声音克隆模型**与社区热门音色 | 规范适配 / 需填 Key |
| **阶跃星辰** | `api.stepfun.com/v1/audio/speech` | `stepaudio-2.5-tts` 多模态语境感知大模型 | 规范适配 / 需填 Key |
| **OpenAI / 兼容** | `api.openai.com/v1/audio/speech` | 标准 OpenAI 格式，扩充包含 GPT-4o 旗舰音色 (`coral`, `sage`, `ash`) | 规范适配 / 需填 Key |
| **自定义 HTTP 模板** | 任意自建端点 | 支持私有化部署的 **GPT-SoVITS**、**CosyVoice-v2**、**F5-TTS**、**VITS** 等本地与局域网节点 | 规范适配 / 无需 Key |

---

## 🚀 进阶技术亮点

### 1. 🎭 小说智能多角色双音色广播剧引擎 (Multi-Role Dual-Voice)
* 智能识别小说段落中的引号对话 `“...”` 与旁白叙述；
* 自动路由旁白至主音色（如沉稳沉浸男声），对话至专属角色音色（如灵动少女音/青年音），并发无缝推流，呈现广播剧级听书享受。

### 2. 🎵 独创双句滑动窗口并发预加载 (Zero-Gap Prefetch Pipeline)
* 当前句子在流式推流播放时，后台协程自动预取并解码下两句音频到内存；
* 彻底消除传统 TTS 句与句之间尴尬的 0.5s 网络等待空白，实现丝滑连续的拟人发音。

### 3. 🌐 全局 HTTP / SOCKS5 代理路由与网络调优
* 在 App 设置中直接配置本地或远程代理（如 `127.0.0.1:7890`），无缝打通海外节点（Google Gemini、OpenAI 等）直连与超时重试。

### 4. 🔢 小说章节与中文数字发音优化器
* 自动将生硬的阿拉伯数字转为流畅的中文发音（例如：“第123章” $\to$ “第一百二十三章”，“2026年” $\to$ “二零二六年”，“99.5%” $\to$ “百分之九十九点五”）。

### 5. 🔄「阅读 (Legado)」替换规则无缝导入与精品多音字库
* 支持一键粘贴并导入「阅读 3.0」规则 JSON，内置多音字纠错词典（银行/行长、重庆/重量、参差、差遣、便宜行事、关卡等数十个高频生僻多音字）。

### 6. 🧪 全性能测试工作台与律动跳动声波
* 内置专业实验室面板，提供实时动态声波 Canvas 律动、首字出声延迟 (TTFB) 监控、典型小说预设与一键试听。

---

## 📖 对接「阅读 (Legado)」等小说软件教程

1. **安装并打开本 App**：
   * 选择任意模型（如**微软 Edge TTS** 或输入 **小米 MiMo** 的 Key）。
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
git clone https://github.com/misaka02/ai-tts-android.git
cd ai-tts-android

# 执行单元测试
./gradlew test

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease
```

---

## 💖 致谢 (Credits & Acknowledgments)

本项目全生命周期由 **Google DeepMind Antigravity** 全自主代码与架构大模型研发生成，特别感谢开源社区对 AI 驱动软件工程的支持！
