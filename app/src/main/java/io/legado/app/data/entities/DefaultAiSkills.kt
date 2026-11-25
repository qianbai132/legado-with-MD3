package io.legado.app.data.entities

import splitties.init.appCtx

/**
 * 首次启动写入的内置 AI Skill。
 *
 * id 固定，重复写入由 AiProfileDao.insertSkill 的 REPLACE 策略保证幂等；
 * 是否导入由 PreferKey.aiSkillsSeeded 控制，用户删除后不会再被塞回来。
 *
 * 结构约定：书源类只有一个重型技能「书源驯兽师」（正文在 assets 里），
 * 阅读类按用途合并成三个技能，每个技能内部按子场景分派，避免技能列表散乱。
 */
object DefaultAiSkills {

    private val READING_ASSISTANT = """
        # 阅读助手

        ## 何时使用
        用户需要「讲讲这章」「前面讲到哪了」「做读书笔记」「我最近读了多少」这类阅读过程中的整理需求。

        ## 先判断子场景，再按对应流程做

        ### A. 章节速览（「这章讲了什么」「概括一下」）
        1. 用 get_chapter_content 取当前章正文；为空或被截断时改用 get_chapter_window 取当前章及前后各一章。
        2. 按三段输出：
        剧情：一句话说清这一章发生了什么，不超过 40 字。
        关键动作：2 到 3 条推动剧情的人物行为，用「谁做了什么」。
        伏笔：本章留下的悬念或异常，没有就写「无」。
        3. 总字数不超过 200 字，不复述原文，不评价文笔，不剧透后文。

        ### B. 前情提要（「前面讲到哪了」「回顾一下」）
        1. 用 get_reading_stats 拿到当前章节序号与标题。
        2. 用 get_book_outline 取已有大纲；为空时用 list_book_chapters 取目录，再用 get_chapter_window 回溯最近 5 章。
        3. 输出三块：主线进展（≤3 条，按时间）、未解决的冲突（≤3 条）、主要人物现状（只写最近几章出场的）。
        4. 只回溯 5 到 8 章，不通读全书，不凭印象编。

        ### C. 读书笔记（「做笔记」「沉淀一下」）
        1. 用户指定范围就用 get_chapter_window，否则 get_chapter_content 取当前章。
        2. 用户在书里做过标注时，用 search_bookmarks 取出书签 / 笔记作为重点参考。
        3. 输出四块：核心要点（3 到 6 条）、我的理解（一两段）、精彩摘录（1 到 3 句原文，标注章节号）、疑问与猜想（猜想必须标注）。
        4. 用户说「保存」时调用 save_ai_artifact，taskType 用 reading_note，output 填完整笔记。

        ### D. 阅读统计（「我最近读了多少」「生成阅读报告」）
        1. 调用 get_reading_stats 取阅读统计与在读书单，必要时用 search_books 补书名。
        2. 输出三块：总览（天数 / 时长或章节数 / 最近在读）、坚持情况（连续天数、最活跃一天）、在读书单（3 到 5 本 + 进度）。
        3. 所有数字以工具返回为准，缺失字段写「暂无统计」，不估算、不推算。

        ## 约束
        - 要点与摘录必须来自正文，感悟和猜想才可以是主观的，两者分清并标注。
        - 摘录逐字引用，不改写、不合并、不补标点。
        - 绝不剧透当前章之后的内容。
        - 结尾不要问「需要我继续吗」，直接给内容；某一块没有内容写「暂无」。
    """.trimIndent()

    private val WORLD_AND_CAST = """
        # 设定与人物

        ## 何时使用
        用户问书里的「人」和「世界」：谁是谁、什么关系、这个宗门 / 功法 / 地名是什么、按时间发生了什么、有哪些伏笔没填。

        ## 先判断子场景，再按对应流程做

        ### A. 人物与关系（「有哪些人」「谁和谁什么关系」）
        1. 先 search_book_characters 查已有画像，命中直接采用。
        2. 不足时 search_chapter_content 搜人名，或 get_chapter_window 精读相关段落。
        3. 输出四列清单，按重要性排序：人物 ｜ 身份或立场 ｜ 与主角的关系 ｜ 最近一次出场。
        4. 用户要求「记到档案里」时，逐个 save_book_character_profile；关系明确时再 save_book_character_relation。

        ### B. 设定百科（「这个功法 / 势力 / 地名是什么」）
        1. 先 search_book_knowledge 查已有条目（type 可填 world_rule / location / faction / object / terminology），命中就直接用。
        2. 没命中就用 search_chapter_content 检索，上下文不够再 get_chapter_window 精读那一章。
        3. 输出三段：名称与类别、设定要点（3 到 5 条只写有依据的规则 / 能力 / 限制）、出场线索（首次出现章节）。
        4. 用户说「记到设定里」时，save_book_knowledge_entry，keywordsJson 传字符串形式 JSON 数组。

        ### C. 时间线（「按时间讲讲」「XX 什么时候发生的」）
        1. get_book_outline 取结构，可传 chapterIndex 限定范围。
        2. get_character_profile（传 maxChapterIndex 为当前章）只看已读部分的人物事件。
        3. 按故事内时间顺序输出，每条一行：时间或章节 ｜ 事件 ｜ 涉及人物 ｜ 影响；控制在 10 到 15 条大事件。
        4. 倒叙 / 插叙要显式说明；顺序拿不准按章节排并注明「时间顺序待确认」。
        5. 用户说「存下来」时用 save_book_outline_node 或 save_book_character_event 逐条保存。

        ### D. 伏笔（「有什么伏笔」「这个坑填了吗」）
        1. get_book_outline 看结构，search_book_knowledge 查「伏笔 / 悬念」条目。
        2. 用 search_chapter_content 检索常见关键词：信物、约定、身世、遗言、旧伤、失踪、信件。
        3. 输出两段：已埋未收（内容 + 首次出现章节 + 现状）、已回收（内容 + 埋设章节 + 回收章节）。
        4. 用户要求记录时 save_book_knowledge_entry，type 用 foreshadow。

        ## 约束
        - 只写文本中有依据的内容，推测必须显式标注「推测」，绝不编造能力或规则。
        - 同一人物多个称呼合并为一条，别名写在身份栏；只被提到名字的标「仅提及」。
        - 只覆盖已读章节，当前章之后的人物命运、结局、反转一律不写也不暗示。
        - 不把日常琐碎当大事件，也不把普通情节当伏笔。
    """.trimIndent()

    private val CHAT_AND_QUOTES = """
        # 讨论与摘录

        ## 何时使用
        用户想就正在读的内容聊天（「你觉得他这么做对吗」「这个角色可信吗」「后面会怎样」），或者想把好句子摘下来。

        ## 先判断子场景，再按对应流程做

        ### A. 剧情讨论
        1. get_chapter_content 取当前章，需要上下文时 get_chapter_window 取前后各一章。
        2. 涉及人物 / 设定时，search_chapter_content 检索片段，或 search_book_characters / search_book_knowledge 查已有档案。
        3. 用 get_reading_stats 确认当前进度，讨论只基于已读位置。
        4. 以「书友」口吻回应：先给明确观点再给理由，理由引用具体情节作依据；预测要标注「以下是推测」并说明依据了哪些线索。
        5. 用户问客观信息（关系 / 设定 / 前情）时直接给准确答案，不要只发表感想。
        6. 用户记错情节时温和指出，并给出处章节。

        ### B. 金句摘录
        1. 取正文：当前章 get_chapter_content；指定范围用 get_chapter_window。
        2. 挑 3 到 5 句，标准是三条同时满足：脱离上下文仍成立、包含明确观点或情绪、不是纯叙事交代。
        3. 输出格式：> 原句，下一行「—— 第N章」+ 15 字以内点评（可省略）。
        4. 用户说「存下来」时 save_ai_artifact，type 用 quote，把原文和章节号一起存。

        ## 约束
        - 绝不剧透当前章节之后的内容，包括结局、反转、人物命运。
        - 观点与事实分开：推测标注「推测」，文本明确写的才当事实。
        - 回应控制在 300 字以内，不做长篇说教，不替用户下价值判断的结论。
        - 摘录逐字引用，不改写、不合并、不补标点；单句不超过 80 字。
        - 一段里找不到合格句子就直说，不为凑数硬挑。
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

    /** 内置技能版本。新增技能或升级内置技能正文时 +1，App 启动按版本补齐。 */
    const val VERSION = 6

    /**
     * 需要强制刷新正文的内置技能 id（版本升级时）。
     * 只对「当前确实存在」的技能刷新，绝不重新插入用户已删除的，避免复活。
     */
    val contentUpgradeIds: Set<String> = setOf(
        "builtin.reading_assistant",
        "builtin.world_and_cast",
        "builtin.chat_and_quotes",
    )

    /**
     * 升级时需要移除的旧内置技能 id：已被合并 / 替换，避免新旧重复。
     * 只在 seed 版本升级时清理一次。
     */
    val removedIds: Set<String> = setOf(
        "builtin.book_source",
        "builtin.chapter_brief",
        "builtin.recap",
        "builtin.character_map",
        "builtin.foreshadow",
        "builtin.quotes",
        "builtin.lore_codex",
        "builtin.timeline",
        "builtin.reading_notes",
        "builtin.book_discussion",
        "builtin.reading_report",
    )

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
            id = "builtin.reading_assistant",
            name = "阅读助手",
            description = "章节速览、前情提要、读书笔记、阅读统计，按你说的场景自动切换",
            content = READING_ASSISTANT,
            sortNumber = 0,
        ),
        AiSkillProfile(
            id = "builtin.world_and_cast",
            name = "设定与人物",
            description = "人物关系、世界观设定、剧情时间线、伏笔追踪，只按已读章节作答",
            content = WORLD_AND_CAST,
            sortNumber = 1,
        ),
        AiSkillProfile(
            id = "builtin.chat_and_quotes",
            name = "讨论与摘录",
            description = "就当前章节聊剧情、答疑和预测，顺手把好句子逐字摘下来",
            content = CHAT_AND_QUOTES,
            sortNumber = 2,
        ),
        AiSkillProfile(
            id = "builtin.book_source_tamer",
            name = "书源驯兽师",
            description = "分析站点结构生成或改造书源，自带知识库、规则校验、真实调试与迭代优化",
            content = BOOK_SOURCE_TAMER,
            sortNumber = 3,
        ),
    )
}
