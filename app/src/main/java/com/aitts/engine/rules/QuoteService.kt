package com.aitts.engine.rules

import com.aitts.engine.network.SharedHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * 试听语料与名言金句服务 (Quote & Corpus Service)：
 * 1. 内置多分类离线精选语料库 (小说剧场对白、新闻播报、文学诗词、现代科技数码、日常对话)；
 * 2. 支持从「一言 (Hitokoto)」公开 API 异步拉取实时文学名句与影视台词；
 * 3. 具备弱网/离线自动降级机制，保障 100% 毫秒级返回。
 */
object QuoteService {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class HitokotoResponse(
        val id: Long? = null,
        val hitokoto: String = "",
        val type: String? = null,
        val from: String? = null,
        val from_who: String? = null,
        val creator: String? = null
    )

    data class QuoteItem(
        val text: String,
        val category: String,
        val source: String? = null
    )

    // 分类离线精选语料库
    private val NOVEL_QUOTES = listOf(
        "“这柄天玄诛仙剑，乃是上古神魔遗留在凡间的至宝。”老者抚须长叹道。少年握紧剑柄，目光坚定：“前辈放心，我必以它荡平魔域！”",
        "“师尊，弟子有一事不明。”少女抬起头，清澈的眼眸中满是困惑，“为何修仙之人，反倒要斩断凡尘七情六欲？”",
        "魔尊漠然冷笑道：“区区蝼蚁，也妄想与日月争辉？今日便让你们知晓，何为真正的力量！”",
        "“快走！别管我！”队长嘶声力竭地怒吼着，单手拉开了防御结界的最后一道开关，“把情报送回基地，这是命令！”",
        "夜幕低垂，窗外的细雨声愈发清晰。他轻轻合上手中的旧相册，低声自语道：“十年了，原来你一直都在这里。”"
    )

    private val NEWS_QUOTES = listOf(
        "据国家航天局最新消息，新一代载人月球探测飞船已顺利完成各项预定测试，预计将于近期按计划实施发射任务。",
        "今日上午，国际人工智能与智能计算大会在国家会议中心开幕，多位图灵奖得主及产业界代表就大模型具身智能发表了主旨演讲。",
        "气象台发布最新天气预报：受冷空气南下影响，未来三天北方大部分地区将迎来明显降温，伴随4到6级阵风，请市民注意防寒保暖。",
        "最新金融统计数据显示，今年前三季度高技术制造业投资同比增长12.8%，产业结构升级步伐持续加快。"
    )

    private val LITERATURE_QUOTES = listOf(
        "寄蜉蝣于天地，渺沧海之一粟。哀吾生之须臾，羡长江之无穷。挟飞仙以遨游，抱明月而长终。",
        "多年以后，面对行刑队，奥雷里亚诺·布恩迪亚上校将会回想起父亲带他去见识冰块的那个遥远的下午。",
        "生活不可能像你想象的那么好，但也不会像你想象的那么糟。我觉得人的脆弱和坚强都超乎自己的想象。",
        "林花谢了春红，太匆匆。无奈朝来寒雨晚来风。胭脂泪，相留醉，几时重。自是人生长恨水长东。",
        "月光如流水一般，静静地泻在这一片叶子和花上。薄薄的青雾浮起在荷塘里。叶子和花仿佛在牛乳中洗过一样。"
    )

    private val TECH_QUOTES = listOf(
        "全新发布的旗舰处理器采用 3nm 先进工艺制程，集成新一代 NPU 与 GPU 架构，AI 神经网络推理算力提升超过 45%。",
        "这台设备配备了 1080P 高清 OLED 屏幕，支持 WiFi 6 与蓝牙 5.3 协议，底部提供了一个全功能的 USB-C 接口。",
        "系统通过异步非阻塞 I/O 与 HTTP/2 多路复用机制，将大模型首字生成的网络响应延迟控制在理想区间内。"
    )

    private val DAILY_QUOTES = listOf(
        "今天天气真不错，要不要下午一起去公园散散步，顺便喝杯热咖啡？",
        "真的太谢谢你了！要不是你帮忙，我今天肯定赶不上最后一班地铁了。",
        "请问您需要办理什么业务呢？如果是咨询账户相关的问题，我可以为您转接专属服务专员。"
    )

    /**
     * 随机获取一条本地精选语料
     */
    fun getRandomLocalQuote(category: String? = null): QuoteItem {
        return when (category) {
            "小说剧场" -> QuoteItem(NOVEL_QUOTES.random(), "小说剧场")
            "新闻播报" -> QuoteItem(NEWS_QUOTES.random(), "新闻播报")
            "经典文学" -> QuoteItem(LITERATURE_QUOTES.random(), "经典文学")
            "科技数码" -> QuoteItem(TECH_QUOTES.random(), "科技数码")
            "日常闲聊" -> QuoteItem(DAILY_QUOTES.random(), "日常闲聊")
            else -> {
                val allPool = listOf(
                    NOVEL_QUOTES.map { QuoteItem(it, "小说剧场") },
                    NEWS_QUOTES.map { QuoteItem(it, "新闻播报") },
                    LITERATURE_QUOTES.map { QuoteItem(it, "经典文学") },
                    TECH_QUOTES.map { QuoteItem(it, "科技数码") },
                    DAILY_QUOTES.map { QuoteItem(it, "日常闲聊") }
                ).flatten()
                allPool.random()
            }
        }
    }

    /**
     * 从「一言 (Hitokoto)」在线拉取最新名言金句
     * 失败或超时时自动平滑回退至本地文学语料
     */
    suspend fun fetchOnlineHitokoto(): QuoteItem = withContext(Dispatchers.IO) {
        try {
            val url = "https://v1.hitokoto.cn/?c=a&c=b&c=d&c=h&c=i&c=k&encode=json"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "AI-TTS-Android-Engine/2.0")
                .build()

            val client = SharedHttpClient.instance
            val response = client.newCall(request).execute()
            response.use { resp ->
                if (resp.isSuccessful) {
                    val bodyStr = resp.body?.string() ?: ""
                    if (bodyStr.isNotBlank()) {
                        val parsed = json.decodeFromString<HitokotoResponse>(bodyStr)
                        if (parsed.hitokoto.isNotBlank()) {
                            val sourceDesc = when {
                                !parsed.from.isNullOrBlank() && !parsed.from_who.isNullOrBlank() -> "《${parsed.from}》 · ${parsed.from_who}"
                                !parsed.from.isNullOrBlank() -> "《${parsed.from}》"
                                !parsed.from_who.isNullOrBlank() -> parsed.from_who
                                else -> "一言金句"
                            }
                            return@withContext QuoteItem(
                                text = parsed.hitokoto.trim(),
                                category = "一言金句",
                                source = sourceDesc
                            )
                        }
                    }
                }
            }
            getRandomLocalQuote("经典文学")
        } catch (e: Exception) {
            // 离线/弱网自动回退本地精品库
            getRandomLocalQuote("经典文学")
        }
    }
}
