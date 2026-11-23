package io.legado.app.domain.model.readaloud

data class CloudTtsEngine(
    val id: String,
    val name: String,
    val provider: CloudTtsProviderType,
    val baseUrl: String = "",
    val apiKey: String = "",
    val secretKey: String = "",
    val region: String = "",
    val appId: String = "",
    val model: String = "",
    val optionsJson: String = "{}",
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

enum class CloudTtsProviderType(val storageValue: String) {
    OpenAiSpeech("openai_speech"),
    GeminiTts("gemini_tts"),
    Mimo("mimo"),
    AzureSpeech("azure_speech"),
    AlibabaCloud("alibaba_cloud"),
    AwsPolly("aws_polly"),
    Volcengine("volcengine");

    companion object {
        fun fromStorage(value: String): CloudTtsProviderType =
            entries.firstOrNull { it.storageValue == value } ?: Mimo
    }
}

data class CloudTtsProviderProfile(
    val displayName: String,
    val defaultModel: String = "",
    val apiKeyLabel: String = "API Key",
    val modelLabel: String = "模型",
    val modelOptions: List<String> = emptyList(),
    val regionOptions: List<String> = emptyList(),
    val audioFormats: List<String> = listOf("mp3"),
    val voiceCatalogType: CloudTtsVoiceCatalogType = CloudTtsVoiceCatalogType.BuiltIn,
)

enum class CloudTtsVoiceCatalogType {
    BuiltIn,
    Remote,
    Partial,
}

val CloudTtsProviderType.profile: CloudTtsProviderProfile
    get() = when (this) {
        CloudTtsProviderType.OpenAiSpeech -> CloudTtsProviderProfile(
            displayName = "OpenAI Speech",
            defaultModel = "gpt-4o-mini-tts",
            modelOptions = listOf(
                "gpt-4o-mini-tts",
                "gpt-4o-mini-tts-2025-12-15",
                "tts-1",
                "tts-1-hd",
            ),
            audioFormats = listOf("mp3", "opus", "aac", "flac", "wav", "pcm"),
        )
        CloudTtsProviderType.GeminiTts -> CloudTtsProviderProfile(
            displayName = "Google Gemini TTS",
            defaultModel = "gemini-3.1-flash-tts-preview",
            modelOptions = listOf(
                "gemini-3.1-flash-tts-preview",
                "gemini-2.5-flash-preview-tts",
                "gemini-2.5-pro-preview-tts",
            ),
            audioFormats = listOf("wav"),
        )
        CloudTtsProviderType.Mimo -> CloudTtsProviderProfile(
            displayName = "Xiaomi MiMo",
            defaultModel = "mimo-v2.5-tts",
            audioFormats = listOf("wav"),
        )
        CloudTtsProviderType.AzureSpeech -> CloudTtsProviderProfile(
            displayName = "Microsoft Azure Speech",
            apiKeyLabel = "Speech Key",
            modelLabel = "模型（可选）",
            regionOptions = listOf(
                "eastasia", "southeastasia", "australiaeast", "northeurope", "westeurope",
                "eastus", "eastus2", "southcentralus", "westus2", "westus3",
                "japaneast", "koreacentral", "centralindia",
            ),
            audioFormats = listOf("mp3", "wav", "ogg"),
            voiceCatalogType = CloudTtsVoiceCatalogType.Remote,
        )
        CloudTtsProviderType.AlibabaCloud -> CloudTtsProviderProfile(
            displayName = "Alibaba Cloud Model Studio",
            defaultModel = "qwen3-tts-flash",
            modelOptions = listOf("qwen3-tts-flash", "qwen3-tts-instruct-flash"),
            audioFormats = listOf("wav"),
            voiceCatalogType = CloudTtsVoiceCatalogType.Partial,
        )
        CloudTtsProviderType.AwsPolly -> CloudTtsProviderProfile(
            displayName = "Amazon Polly",
            defaultModel = "neural",
            apiKeyLabel = "Access Key ID",
            modelLabel = "合成引擎",
            modelOptions = listOf("standard", "neural", "long-form", "generative"),
            regionOptions = listOf(
                "us-east-1", "us-east-2", "us-west-2", "ca-central-1",
                "eu-central-1", "eu-west-1", "eu-west-2", "eu-west-3",
                "ap-south-1", "ap-southeast-1", "ap-southeast-2",
                "ap-northeast-1", "ap-northeast-2",
            ),
            audioFormats = listOf("mp3", "ogg_vorbis", "wav"),
            voiceCatalogType = CloudTtsVoiceCatalogType.Remote,
        )
        CloudTtsProviderType.Volcengine -> CloudTtsProviderProfile(
            displayName = "Volcengine Speech",
            apiKeyLabel = "Access Token",
            modelLabel = "模型（可选）",
            audioFormats = listOf("mp3", "ogg_opus", "wav", "pcm"),
            voiceCatalogType = CloudTtsVoiceCatalogType.Partial,
        )
    }

data class CloudTtsVoiceDescriptor(
    val id: String,
    val displayName: String,
    val locale: String = "",
    val gender: String = "",
    val styles: List<String> = emptyList(),
    val roles: List<String> = emptyList(),
    val supportedModels: List<String> = emptyList(),
    val sampleRate: Int? = null,
)

data class CloudTtsSynthesisRequest(
    val text: String,
    val voiceId: String,
    val locale: String = "",
    val style: String = "",
    val role: String = "",
    val instructions: String = "",
    val speed: Float = 1f,
    val pitch: Float = 1f,
    val volume: Float = 1f,
    val format: String = "mp3",
    val sampleRate: Int? = null,
)

data class CloudTtsAudio(
    val bytes: ByteArray,
    val format: String,
    val sampleRate: Int? = null,
)

/**
 * 一次云 TTS 合成的诊断快照，供「测试连接 / 试听 / 调试台」展示。
 *
 * 只记录可观测的事实（参数、耗时、字节数），不含 API Key 等敏感信息；
 * [requestPreview] 会对文本和指令做截断，避免在诊断面板里刷屏。
 */
data class CloudTtsSynthesisDiagnostics(
    val engineName: String,
    val providerLabel: String,
    val model: String,
    val voiceId: String,
    val requestPreview: String,
    val instructionPreview: String,
    val style: String,
    val elapsedMs: Long,
    /** 音频字节数（WAV 容器内已含 44 字节头）。 */
    val audioBytes: Long,
    val format: String,
    val sampleRate: Int?,
    /** 由字节数与采样率估算的时长（毫秒），仅 WAV/PCM 可算。 */
    val durationMs: Long?,
    /** 是否命中了「参数被服务端拒绝后自动用干净请求重试」的兼容分支。 */
    val usedFallback: Boolean,
    val success: Boolean,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
) {
    /** 供 UI 一行展示的摘要，例如「1.24s · 12.8KB · 24kHz · wav」。 */
    fun summaryLabel(): String = buildList {
        add("%.2fs".format(elapsedMs / 1000f))
        if (audioBytes > 0) add("%.1fKB".format(audioBytes / 1024f))
        sampleRate?.let { add("${it / 1000}kHz") }
        add(format)
    }.joinToString(" · ")
}

/** 把诊断快照格式化成可复制的多行报告（不含任何密钥）。 */
fun CloudTtsSynthesisDiagnostics.toShareText(title: String): String = buildString {
    appendLine(title)
    appendLine("─".repeat(24))
    appendLine("状态      : ${if (success) "成功" else "失败"}")
    appendLine("引擎      : $engineName（$providerLabel）")
    appendLine("模型      : $model")
    appendLine("音色      : $voiceId")
    appendLine("耗时      : ${elapsedMs} ms")
    appendLine("音频大小  : $audioBytes bytes")
    appendLine("格式 / 采样: $format / ${sampleRate ?: "-"} Hz")
    durationMs?.let { appendLine("估算时长  : ${it} ms") }
    if (style.isNotBlank()) appendLine("风格      : $style")
    if (usedFallback) appendLine("兼容重试  : 是（参数被拒后用干净请求重试）")
    appendLine("请求文本  : $requestPreview")
    if (instructionPreview.isNotBlank()) appendLine("指令      : $instructionPreview")
    errorMessage?.let { appendLine("错误      : $it") }
}
