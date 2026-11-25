package io.legado.app.data.entities

import splitties.init.appCtx

/**
 * 首次启动写入的内置 AI Skill。
 *
 * id 固定，重复写入由 AiProfileDao.insertSkill 的 REPLACE 策略保证幂等；
 * 是否导入由 PreferKey.aiSkillsSeeded 控制，用户删除后不会再被塞回来。
 */
object DefaultAiSkills {

    private val CHAPTER_BRIEF = """
        # 章节速览

        ## 何时使用
        用户说「讲讲这章」「这章讲了什么」「概括一下」时。

        ## 步骤
        1. 调用 get_chapter_content 取当前章正文。若返回为空或被截断，改用 get_chapter_window 取当前章及前后各一章。
        2. 若用户问的是「刚才那几章」，先用 list_book_chapters 定位章节，再逐章取正文。
        3. 严格按下面三段输出，除小标题外不加任何多余内容：

        剧情：一句话说清这一章发生了什么，不超过 40 字。
        关键动作：列出 2 到 3 个推动剧情的人物行为，用「谁做了什么」的句式。
        伏笔：这一章留下的悬念或异常，没有就写「无」。

        ## 约束
        - 总字数不超过 200 字。
        - 不复述原文句子，不评价文笔，不剧透后面的章节。
        - 用户说「存下来」时，调用 save_ai_artifact，type 用 note，标题用「第N章 速览」。
    """.trimIndent()

    private val RECAP = """
        # 前情提要

        ## 何时使用
        用户追更前想看「前面讲到哪了」「回顾一下」「我上次看到哪」。

        ## 步骤
        1. 调用 get_reading_stats 确认当前阅读进度，拿到当前章节序号和标题。
        2. 调用 get_book_outline 取已有大纲。若大纲为空，用 list_book_chapters 取目录，再用 get_chapter_window 回溯最近 5 章。
        3. 按下面三块输出：

        主线进展：3 条以内，按时间顺序，每条一句。
        未解决的冲突：当前还悬着的问题，最多 3 条。
        主要人物现状：谁在哪、在做什么，只写最近几章有出场的。

        ## 约束
        - 只回溯最近 5 到 8 章，不要通读全书，也不要凭印象编。
        - 已有 save_book_outline_node 记录时优先复用，不重复读正文。
        - 结尾不要问「需要我继续吗」，直接给出内容。
    """.trimIndent()

    private val CHARACTER_MAP = """
        # 人物图谱

        ## 何时使用
        用户问「这书有哪些人」「谁和谁什么关系」「某某是谁」「这个人之前出现过吗」。

        ## 步骤
        1. 先调用 search_book_characters 查已有画像，命中就直接采用。
        2. 不足时用 search_chapter_content 搜人名，或 get_chapter_window 精读相关段落补充。
        3. 输出四列清单，按出场重要性排序：

        人物 | 身份或立场 | 与主角的关系 | 最近一次出场

        4. 用户要求「存下来」「记到档案里」时，逐个调用 save_book_character_profile；两人之间关系明确时，再调用 save_book_character_relation。

        ## 约束
        - 只写文本中有依据的内容。推测必须显式标注「推测」。
        - 同一人物有多个称呼时合并成一条，在身份栏注明别名。
        - 没有出过场、只被提到名字的，单独列在末尾并标注「仅提及」。
    """.trimIndent()

    private val FORESHADOW = """
        # 伏笔追踪

        ## 何时使用
        用户问「有什么伏笔」「这个坑填了吗」「前面那个东西后来怎么样了」。

        ## 步骤
        1. 调用 get_book_outline 看已有结构，再用 search_book_knowledge 查「伏笔」「悬念」类条目。
        2. 用 search_chapter_content 按关键词检索，常见关键词：信物、约定、身世、遗言、旧伤、失踪、信件。
        3. 按两段输出：

        已埋未收：伏笔内容 + 首次出现章节 + 目前状态。
        已回收：伏笔内容 + 埋设章节 + 回收章节。

        4. 用户要求记录时，调用 save_book_knowledge_entry，type 用 foreshadow，把章节号写进内容里。

        ## 约束
        - 严格区分「文本已明确回收」和「尚未回收」。无法判断就写「待确认」。
        - 不把普通情节当伏笔。只记录被反复提及、或明显带暗示的细节。
        - 章节号拿不准时不要编，写「约第N章前后」或省略。
    """.trimIndent()

    private val QUOTES = """
        # 金句摘录

        ## 何时使用
        用户说「摘几句好的」「这句话记一下」「有没有写得好的句子」。

        ## 步骤
        1. 取正文：当前章用 get_chapter_content；用户指定范围时用 get_chapter_window。
        2. 挑 3 到 5 句。标准是三条同时满足：脱离上下文仍能独立成立、包含明确观点或情绪、不是纯叙事交代。
        3. 输出格式：

        > 原句

        —— 第N章，后接一句 15 字以内的点评，可省略。

        4. 用户说「存下来」时，调用 save_ai_artifact，type 用 quote，把原文和章节号一起存进去。

        ## 约束
        - 必须逐字引用，不改写、不合并、不补标点。
        - 单句不超过 80 字，超了就截到最近的完整分句。
        - 一段里找不到符合标准的句子就直说，不要为了凑数硬挑。
    """.trimIndent()

    /**
     * 书源驯兽师（完整版 SKILL.md，约 155KB）。
     * 体积远超 JVM 单字符串常量 64KB 上限，放 assets 运行时读取。
     */
    private val BOOK_SOURCE_TAMER: String by lazy {
        runCatching {
            appCtx.assets.open("ai/skills/book_source_tamer.md").use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }
        }.getOrDefault("# 书源驯兽师\n\n技能内容加载失败，请检查安装资源。")
    }

    private val LORE_CODEX = """
        # 设定百科

        ## 何时使用
        用户问书里的世界观设定：「这个宗门/组织是什么」「某某功法/神器有什么用」「这个地名在哪」「XX 术语什么意思」「这个势力有什么规矩」。

        ## 步骤
        1. 先调用 search_book_knowledge 查已有设定条目，query 用用户问的关键词，type 可填 world_rule / location / faction / object / terminology。命中就直接采用，不重复读正文。
        2. 没命中就用 search_chapter_content 在正文里按该关键词检索，取回相关章节片段；上下文不够时再用 get_chapter_window 精读那一章。
        3. 用 get_reading_stats 确认当前章节序号。涉及「现在已经变成怎样」时只引用当前章之前的内容，避免剧透后文。
        4. 按三段输出：
        名称与类别：它是什么（功法 / 势力 / 地点 / 物品 / 制度）。
        设定要点：3 到 5 条，只写文本有明确依据的规则、来历、能力或限制。
        出场线索：首次出现的章节号，拿不准写「约第N章」。
        5. 用户说「记到设定里」「存下来」时，调用 save_book_knowledge_entry，type 按类别选，title 用名称，keywordsJson 传字符串形式的 JSON 数组，例如 "[\"宗门\",\"功法\"]"。

        ## 约束
        - 只写文本里有依据的内容，推测必须标注「推测」，绝不编造能力或规则。
        - keywordsJson、evidenceJson 这类参数是字符串形式的 JSON，不是数组，务必加引号并转义内部双引号。
        - 后续章节才揭示的设定一律不提前透露；判断不了就只说当前已知的。
        - 已有条目优先复用，不为同一设定重复建档。
    """.trimIndent()

    private val TIMELINE = """
        # 剧情时间线

        ## 何时使用
        用户问「这本书的时间线」「按时间顺序讲讲发生的事」「XX 事件是什么时候发生的」「梳理一下剧情发展」。

        ## 步骤
        1. 先调用 get_book_outline 取大纲节点，可传当前 chapterIndex 限定范围，拿到全书 / 卷 / 章的结构。
        2. 用 search_book_characters 列出主要人物，对关键人物调用 get_character_profile，并传 maxChapterIndex 为当前章节，只取已读部分的人物事件，避免剧透。
        3. 需要补具体事件时用 search_chapter_content 按事件关键词检索，或 get_chapter_window 精读相邻章节确认先后。
        4. 按故事内时间顺序（若有倒叙要说明）输出，每条一行：
        时间或章节 ｜ 事件 ｜ 涉及人物 ｜ 影响
        只列推动主线的大事件，控制在 10 到 15 条。
        5. 用户说「存下来」时，用 save_book_outline_node（nodeType 用 arc 或 chapter，startChapterIndex / endChapterIndex 填覆盖范围）或 save_book_character_event 逐条保存。

        ## 约束
        - 只覆盖已读章节，当前章之后的事件一律不列，也不暗示。
        - 故事内时间与章节顺序不一致（倒叙、插叙）时要显式说明。
        - 顺序拿不准就按章节顺序排，并注明「时间顺序待确认」。
        - 不把日常琐碎当大事件，只保留改变人物处境或主线走向的节点。
    """.trimIndent()

    private val READING_NOTES = """
        # 读书笔记

        ## 何时使用
        用户说「做读书笔记」「把这几章整理成笔记」「记一下我的感想」「帮我沉淀一下读到的东西」。区别于「章节速览」：速览只要客观概括，笔记要带上要点、感悟和疑问，供长期积累。

        ## 步骤
        1. 取正文：用户指定范围就用 get_chapter_window 取前后若干章，否则用 get_chapter_content 取当前章。
        2. 若用户在书里做过标注，用 search_bookmarks（传书名）取出书签 / 笔记作为重点参考。
        3. 按四块输出：
        核心要点：3 到 6 条，每条一句，只写关键信息。
        我的理解：一两段写这段在讲什么、和前文如何关联。
        精彩摘录：1 到 3 句原文，逐字引用并标注章节号。
        疑问与猜想：读到这里产生的疑问或对后续的猜想，猜想要标注「猜想」。
        4. 用户说「保存」「存进笔记」时，调用 save_ai_artifact，taskType 用 reading_note，output 填完整笔记，chapterIndex 填对应章节。

        ## 约束
        - 要点和摘录必须来自正文，感悟和猜想才可以是主观的，两者要分清。
        - 摘录逐字引用，不改写、不合并。
        - 不剧透当前章之后的内容，「我的理解」只基于已读部分。
        - 不为了凑数硬写，某一块没有内容就写「暂无」。
    """.trimIndent()

    private val BOOK_DISCUSSION = """
        # 剧情讨论

        ## 何时使用
        用户想就当前读到的内容和 AI 聊：「你觉得男主这么做对吗」「这个角色可信吗」「后面可能会怎样」「这段什么意思」「帮我解答这里的疑问」。

        ## 步骤
        1. 用 get_chapter_content 取当前章正文；需要上下文时用 get_chapter_window 取前后各一章。问题涉及具体人物 / 设定时，再用 search_chapter_content 检索相关片段，或 search_book_characters / search_book_knowledge 查已有档案。
        2. 用 get_reading_stats 确认当前章节，确保讨论只基于用户已读到的位置。
        3. 以「书友」口吻回应：
        - 先给明确观点再给理由，理由要引用文本里的具体情节作依据。
        - 涉及剧情预测时标注「以下是推测」，并说明依据了哪些已读线索。
        - 用户问的是客观信息（人物关系、设定、前情）时直接给准确答案，不要只发表感想。
        4. 发现用户记错情节时，温和指出并给出处章节。

        ## 约束
        - 绝不剧透用户当前章节之后的内容，包括结局、反转、人物命运。
        - 观点与事实分开：推测标注「推测」，文本明确写的才当事实。
        - 不做长篇说教，回应控制在 300 字以内，留空间继续讨论。
        - 不替用户下结论，涉及价值判断时给角度而不是标准答案。
    """.trimIndent()

    private val READING_REPORT = """
        # 阅读报告

        ## 何时使用
        用户问「我最近读了多少」「生成阅读报告」「这个月读书情况」「我的阅读统计」。

        ## 步骤
        1. 调用 get_reading_stats 取阅读统计、最近在读的书和每日阅读记录；用户指定周期时用 date 参数或按返回记录自行筛选。
        2. 需要书名和进度时用 search_books（query 留空返回最近读的书）补充。
        3. 按三块输出：
        总览：周期内读了几天、累计阅读时长或章节数（以工具实际返回的字段为准，没有的不要编）、最近在读的书。
        坚持情况：连续阅读天数、最活跃的一天。
        在读书单：最近在读的 3 到 5 本，各附当前进度。
        4. 所有数字以工具返回为准，缺失字段直接写「暂无统计」，不估算、不编造。

        ## 约束
        - 报告要简洁，用列表呈现，控制在 250 字以内。
        - 结尾给一句基于真实数据的鼓励，不要空泛鸡汤。
        - 不评价用户的阅读品味或阅读速度。
        - 工具没返回的指标一律不写，更不能推算。
    """.trimIndent()

    /** 内置技能版本。新增技能或升级内置技能正文时 +1，App 启动按版本补齐。 */
    const val VERSION = 5

    /**
     * 需要强制刷新正文的内置技能 id（版本升级时）。
     * 只对「当前确实存在」的技能刷新，绝不重新插入用户已删除的，避免复活。
     * 今后升级某个内置技能正文时，把它加进这里并升 VERSION。
     */
    val contentUpgradeIds: Set<String> = emptySet()

    /**
     * 升级时需要移除的旧内置技能 id：被新技能替换，避免新旧重复。
     * 只在 seed 版本升级时清理一次。
     */
    val removedIds: Set<String> = setOf("builtin.book_source")

    /** VERSION 1 发布的技能 id：迁移时视为已处理，避免把用户删掉的又塞回去。 */
    val legacyIds: Set<String> = setOf(
        "builtin.chapter_brief",
        "builtin.recap",
        "builtin.character_map",
        "builtin.foreshadow",
        "builtin.quotes",
    )

    fun all(): List<AiSkillProfile> = listOf(
        AiSkillProfile(
            id = "builtin.chapter_brief",
            name = "章节速览",
            description = "按固定结构概括当前章节：剧情、人物动作、伏笔，200 字以内",
            content = CHAPTER_BRIEF,
            sortNumber = 0,
        ),
        AiSkillProfile(
            id = "builtin.recap",
            name = "前情提要",
            description = "追更前回顾最近章节的主线、未解冲突和人物现状",
            content = RECAP,
            sortNumber = 1,
        ),
        AiSkillProfile(
            id = "builtin.character_map",
            name = "人物图谱",
            description = "梳理书中人物身份、立场与关系，可写入人物档案",
            content = CHARACTER_MAP,
            sortNumber = 2,
        ),
        AiSkillProfile(
            id = "builtin.foreshadow",
            name = "伏笔追踪",
            description = "找出已埋未收和已回收的伏笔，按章节定位",
            content = FORESHADOW,
            sortNumber = 3,
        ),
        AiSkillProfile(
            id = "builtin.quotes",
            name = "金句摘录",
            description = "逐字摘录可独立成立的句子，可选存入摘录库",
            content = QUOTES,
            sortNumber = 4,
        ),
        AiSkillProfile(
            id = "builtin.book_source_tamer",
            name = "书源驯兽师",
            description = "自动化分析网站结构生成书源，提供知识库支持、规则验证、真实调试和自我迭代优化",
            content = BOOK_SOURCE_TAMER,
            sortNumber = 5,
        ),
        AiSkillProfile(
            id = "builtin.lore_codex",
            name = "设定百科",
            description = "查书里的世界观设定：功法、势力、地点、物品、术语，可存入世界书",
            content = LORE_CODEX,
            sortNumber = 6,
        ),
        AiSkillProfile(
            id = "builtin.timeline",
            name = "剧情时间线",
            description = "按故事时间顺序梳理大事件，标出章节与涉及人物，只覆盖已读内容",
            content = TIMELINE,
            sortNumber = 7,
        ),
        AiSkillProfile(
            id = "builtin.reading_notes",
            name = "读书笔记",
            description = "把要点、理解、摘录和疑问整理成可长期积累的读书笔记",
            content = READING_NOTES,
            sortNumber = 8,
        ),
        AiSkillProfile(
            id = "builtin.book_discussion",
            name = "剧情讨论",
            description = "就当前章节答疑、讨论和剧情预测，只基于已读内容不剧透",
            content = BOOK_DISCUSSION,
            sortNumber = 9,
        ),
        AiSkillProfile(
            id = "builtin.reading_report",
            name = "阅读报告",
            description = "按真实阅读统计生成本周或本月的阅读总览与在读书单",
            content = READING_REPORT,
            sortNumber = 10,
        ),
    )
}
