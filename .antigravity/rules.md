# AI-TTS Android 项目核心底层研发准则 (Core Rules)

> [!IMPORTANT]
> 本文件为项目最高优先级底层要求，每次对话与每次思考前必须完整通读执行。

## 1. 思考与分析铁律 (Pre-Thought Protocols)
- **每次思考前必须完整查看整个软件的相关代码**：逐行排查涉及的底层逻辑、数据流与调用链路，严禁凭空假设、敷衍与臆想；
- **每次思考前必须按时间正序完整审阅最近 30 轮用户对话**：牢记用户的每一个具体要求、历史反馈与明确限制，严禁说一句忘一句。

## 2. 计划与执行审批铁律 (Approval Protocols)
- **必须严格执行 Planning Mode**：任何代码修改、重构或构建发布前，必须先编写/更新 `implementation_plan.md` 并标记 `RequestFeedback: true, UserFacing: true`；
- **未经用户明确批准，绝对严禁擅自修改代码或运行修改指令**。

## 3. 音频质量与语速调控铁律 (Audio & Speed Rules)
- **大模型语音原生高保真原则**：严禁采用粗暴截断采样点的破坏性 PCM 切片或产生杂音/炸音的劣质 DSP 处理，保持大模型生成声音 100% 纯净高保真透传；
- **播放器与系统服务全链路贯通**：软件内所有播放器（`AndroidAudioPlayer` 等）必须原生支持语速参数设置，确保试听与实际调用速度 100% 精确生效。

## 4. 版本保留与发布铁律 (Release Retention Rules)
- **GitHub `previous` 渠道版本保留规则**：必须且仅保留最新 **3 个** 历史版本（例如 Round 38, Round 39, Round 40），绝不全清，也绝不多删。

## 5. 工作目录与文档规范红线 (Workspace & Documentation Rules)
- **工作目录绝对禁区**：项目工作根目录绝对只能是 `C:\Users\s1356\.gemini\antigravity\scratch\ai-tts-android`。`brain/` 是 Agent 内部日志/Artifact 存储区，**严禁将项目构建脚本、临时代码文件或 Release APK 引用/放置在 `brain/`**，一切编译与操作必须在项目工作目录内闭环！
- **严禁微信读书幻觉**：微信读书为私有封闭播放器，不支持 Android 标准 `TextToSpeechService`。**任何文档、README、UI 说明中绝对严禁提及微信读书**，必须仅列出真实支持的「开源阅读 (Legado)」、「静读天下」及系统读屏！
- **README 核心规范**：
  1. 必须将 `> [!IMPORTANT]` AI 智能体全自动构建维护声明放置于最醒目位置；
  2. 功能介绍必须极为简明，只介绍核心功能，绝不拖泥带水；
  3. 绝不包含与用户的对话痕迹；
  4. 必须包含完整的致谢与官网链接。
