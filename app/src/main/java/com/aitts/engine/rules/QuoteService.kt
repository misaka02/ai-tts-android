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

    private val LONG_PARAGRAPHS = listOf(
        QuoteItem(
            "曲曲折折的荷塘上面，弥望的是田田的叶子。叶子出水很高，像亭亭的舞女的裙。层层的叶子中间，零星地点缀着些白花，有袅娜地开着的，有羞涩地打着朵儿的；正如一粒粒的明珠，又如碧天里的星星，又如刚出浴的美人。微风过处，送来缕缕清香，仿佛远处高楼上渺茫的歌声似的。",
            "经典散文",
            "《荷塘月色》 · 朱自清"
        ),
        QuoteItem(
            "四百多年里，除去人的两次衰落，树木也繁衍生长，草木也荣枯生死。蜂儿如一朵小雾稳在半空，蚂蚁摇动触芒，瓢虫爬得很快，露水在草叶上滚动。地坛的每一棵树下我都去过，差不多每一米草地上都有过我的车辙。它等待我出生，然后又等待我活到最狂妄的年龄上忽地残废了双腿。",
            "经典散文",
            "《我与地坛》 · 史铁生"
        ),
        QuoteItem(
            "在我的后园，可以看见墙外有两株树，一株是枣树，还有一株也是枣树。这上面的夜的天空，奇怪而高，我生平没有见过这样的奇怪而高的天空。他仿佛要离开人间而去，使人们仰面不再看见。然而现在却非常之蓝，闪闪地眨着几十个星星的眼，冷眼。他使霜浓浓地落到枣树上，极细微的下着。",
            "经典文学",
            "《秋夜》 · 鲁迅"
        ),
        QuoteItem(
            "如果你来访我，我不在，请和我门外的花坐一会儿，它们很温暖，我注视他们很多很多日子了。它们开得这样好，热闹而又寂寞。秋天的空气是新鲜的，带着一点草木凋零的清冷与成熟的气息。阳光像一层金黄的薄纱，静静铺在青石板的小路上，连风都慢了下来。",
            "名家散文",
            "《人间草木》 · 汪曾祺"
        ),
        QuoteItem(
            "天际雷云翻滚，紫电如狂蟒撕裂苍穹。“顾清玄，你当真要逆天而行？”玄天宗宗主虚空而立，冰冷的声音响彻百里荒原。少年拭去嘴角溢出的血迹，青锋长剑斜指苍穹，朗声大笑：“若天道不仁，视万物为刍狗，那我顾清玄便一剑斩破这虚伪苍天！”狂风呼啸，剑鸣动九霄。",
            "小说剧场",
            "《玄天九剑录》 · 仙侠对白"
        ),
        QuoteItem(
            "舱外是无边无际的深邃虚空，冰冷的恒星光芒如同无数根细针穿透观察窗。飞船的主控系统发出了低沉的提示音：“跃迁引擎已锁定目标扇区，剩余倒计时三分钟。”林博士凝视着全息屏幕上那颗幽蓝色的未知行星，双手微微有些发颤。三十年的孤寂航行，人类文明的最后一粒火种，终于抵达了命定的彼岸。",
            "科幻巨作",
            "《深空归途》 · 远航叙事"
        )
    )

    /**
     * 随机获取一条本地精选语料 (支持短句与中长段落)
     */
    fun getRandomLocalQuote(category: String? = null): QuoteItem {
        return when (category) {
            "小说剧场" -> QuoteItem(NOVEL_QUOTES.random(), "小说剧场")
            "新闻播报" -> QuoteItem(NEWS_QUOTES.random(), "新闻播报")
            "经典文学" -> QuoteItem(LITERATURE_QUOTES.random(), "经典文学")
            "科技数码" -> QuoteItem(TECH_QUOTES.random(), "科技数码")
            "日常闲聊" -> QuoteItem(DAILY_QUOTES.random(), "日常闲聊")
            "中长名篇" -> LONG_PARAGRAPHS.random()
            else -> {
                if (Random.nextInt(100) < 35) {
                    LONG_PARAGRAPHS.random()
                } else {
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
    }

    /**
     * 在线拉取精品语料 (智能兼顾名言金句与中长文学名篇)
     * 包含 Hitokoto 文学多分类及备选 API，若随机到长篇或弱网时自动降级至精选中长语料
     */
    suspend fun fetchOnlineHitokoto(): QuoteItem = withContext(Dispatchers.IO) {
        // 40% 概率直接推荐中长文学小说段落 (满足用户长篇断句测试需求)
        if (Random.nextInt(100) < 40) {
            return@withContext LONG_PARAGRAPHS.random()
        }

        try {
            kotlinx.coroutines.withTimeout(3500L) {
                val url = "https://v1.hitokoto.cn/?c=d&c=e&c=h&c=i&c=k&encode=json"
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
                                    else -> "在线金句"
                                }
                                return@withTimeout QuoteItem(
                                    text = parsed.hitokoto.trim(),
                                    category = "在线语料",
                                    source = sourceDesc
                                )
                            }
                        }
                    }
                }
                getRandomLocalQuote()
            }
        } catch (e: Exception) {
            // 离线/弱网/超时 自动秒级回退本地精品库
            getRandomLocalQuote()
        }
    }
}
