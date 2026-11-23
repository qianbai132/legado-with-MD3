package io.legado.app.help.readaloud.playback

import io.legado.app.domain.gateway.CloudTtsEngineGateway
import io.legado.app.domain.model.readaloud.CloudTtsSynthesisDiagnostics
import io.legado.app.domain.model.readaloud.CloudTtsSynthesisRequest
import io.legado.app.domain.model.readaloud.CloudTtsEngine
import io.legado.app.domain.model.readaloud.CloudTtsVoiceConfig
import io.legado.app.domain.model.readaloud.CloudTtsVoiceDescriptor
import io.legado.app.domain.model.readaloud.CharacterPerformanceProfile
import io.legado.app.domain.model.readaloud.ReadAloudVoice
import io.legado.app.domain.model.readaloud.SpeechRoleType
import io.legado.app.domain.model.readaloud.profile
import io.legado.app.utils.GSON
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CloudTtsAudioSynthesizer(
    private val engineGateway: CloudTtsEngineGateway,
) {
    private val registry = CloudTtsProviderRegistry()

    suspend fun fetchVoices(engineId: String): List<CloudTtsVoiceDescriptor> =
        withContext(Dispatchers.IO) {
            val engine = engineGateway.get(engineId) ?: error("云 TTS 引擎不存在")
            require(engine.enabled) { "云 TTS 引擎已停用" }
            registry.get(engine.provider).fetchVoices(engine)
        }

    suspend fun fetchVoices(engine: CloudTtsEngine): List<CloudTtsVoiceDescriptor> =
        withContext(Dispatchers.IO) {
            registry.get(engine.provider).fetchVoices(engine)
        }

    suspend fun synthesize(
        engine: CloudTtsEngine,
        request: CloudTtsSynthesisRequest,
        output: File,
    ): Boolean = withContext(Dispatchers.IO) {
        val audio = registry.get(engine.provider).synthesize(engine, request)
        output.parentFile?.mkdirs()
        output.writeBytes(audio.bytes)
        output.length() > 0
    }

    suspend fun synthesize(
        voice: ReadAloudVoice,
        text: String,
        output: File,
        styleOverride: String = "",
        characterPerformance: CharacterPerformanceProfile? = null,
        roleType: SpeechRoleType = SpeechRoleType.Unknown,
    ): Boolean = withContext(Dispatchers.IO) {
        val engine = engineGateway.get(voice.engineId)
            ?: error("云端引擎不存在或已被替换（engineId=${voice.engineId}），请重新测试连接并保存引擎")
        if (!engine.enabled) error("云端引擎已停用（${engine.name}），请在云 TTS 设置中启用")
        val config = runCatching {
            GSON.fromJson(voice.traitsJson, CloudTtsVoiceConfig::class.java)
        }.getOrNull() ?: CloudTtsVoiceConfig()
        val supportedStyles = runCatching {
            GSON.fromJson(voice.emotionCatalogJson, Array<String>::class.java).toSet()
        }.getOrDefault(emptySet())
        val emotionControl = if (config.automaticEmotion != false) {
            CloudTtsEmotionMapper.map(engine.provider, styleOverride)
        } else {
            CloudTtsEmotionControl()
        }
        val mappedStyle = emotionControl.style.takeIf { it in supportedStyles }
        val characterInstruction = if (config.characterPersonality != false) {
            CharacterPerformanceInstructionBuilder.build(engine.provider, characterPerformance)
        } else ""
        val roleInstruction = if (config.thoughtPerformance != false) {
            CloudTtsRoleInstructionMapper.map(engine.provider, roleType)
        } else ""
        val instructions = listOfNotNull(
            config.instructions.takeIf(String::isNotBlank),
            characterInstruction.takeIf(String::isNotBlank),
            roleInstruction.takeIf(String::isNotBlank),
            emotionControl.instruction.takeIf(String::isNotBlank),
        ).joinToString(" ")
        val request = CloudTtsSynthesisRequest(
            text = text,
            voiceId = voice.speakerId,
            locale = config.locale,
            style = mappedStyle ?: config.style,
            role = config.role,
            instructions = instructions,
            speed = config.speed,
            pitch = config.pitch,
            volume = config.volume,
            format = config.format,
            sampleRate = config.sampleRate,
        )
        val provider = registry.get(engine.provider)
        val audio = runCatching { provider.synthesize(engine, request) }.getOrElse { throwable ->
            // 兼容旧版「测试连接自动落地」音色：那版把目录里的能力标签当成 style、
            // 且情感/性格/思维三个增强全开，拼出的 style/instructions 部分服务端不认，
            // 返回 HTTP 4xx；而连接测试走的是干净请求所以成功。这里在确认请求确实
            // 带了这些增强字段、且是 4xx 时，用干净请求（情感/指令/角色全空）重试
            // 一次，与连接测试成功那次一致。鉴权(401/403)与其它错误不吞。
            val message = throwable.message.orEmpty()
            // 只针对「参数不被接受」的 400/422；鉴权 401/403、限流 429 不重试。
            val isRejectedPayload = "HTTP 400" in message || "HTTP 422" in message
            val hadEnrichment = request.style.isNotBlank() ||
                request.instructions.isNotBlank() || request.role.isNotBlank()
            if (!isRejectedPayload || !hadEnrichment) throw throwable
            provider.synthesize(engine, request.copy(style = "", instructions = "", role = ""))
        }
        output.parentFile?.mkdirs()
        output.writeBytes(audio.bytes)
        output.length() > 0
    }

    /**
     * 带诊断的合成：与 [synthesize] 走同一条 provider 链路，但把参数、耗时、
     * 音频字节数等可观测事实打包成 [CloudTtsSynthesisDiagnostics] 返回给调试功能。
     * 不抛异常——失败也被包进诊断的 [CloudTtsSynthesisDiagnostics.errorMessage]，
     * 方便调试台把「请求 → 结果」完整呈现。
     */
    suspend fun synthesizeDetailed(
        engine: CloudTtsEngine,
        request: CloudTtsSynthesisRequest,
        output: File,
    ): CloudTtsSynthesisDiagnostics = withContext(Dispatchers.IO) {
        val startedAt = System.nanoTime()
        var usedFallback = false
        val result = runCatching {
            val provider = registry.get(engine.provider)
            val audio = runCatching { provider.synthesize(engine, request) }.getOrElse { throwable ->
                // 与 synthesize(voice) 一致的兼容策略：参数被 400/422 拒绝时用干净请求重试一次。
                val message = throwable.message.orEmpty()
                val rejected = "HTTP 400" in message || "HTTP 422" in message
                val enriched = request.style.isNotBlank() ||
                    request.instructions.isNotBlank() || request.role.isNotBlank()
                if (!rejected || !enriched) throw throwable
                usedFallback = true
                provider.synthesize(engine, request.copy(style = "", instructions = "", role = ""))
            }
            output.parentFile?.mkdirs()
            output.writeBytes(audio.bytes)
            Triple(audio.bytes.size.toLong(), audio.format, audio.sampleRate)
        }
        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
        val (bytes, format, sampleRate) = result.getOrNull() ?: Triple(0L, request.format, null)
        val durationMs = if (bytes > 44 && sampleRate != null && sampleRate > 0) {
            // WAV 44 字节头；PCM16 单声道 = 2 字节/采样。
            (bytes - 44) * 1000L / (sampleRate * 2L)
        } else null
        CloudTtsSynthesisDiagnostics(
            engineName = engine.name,
            providerLabel = engine.provider.profile.displayName,
            model = engine.model,
            voiceId = request.voiceId,
            requestPreview = request.text.take(160),
            instructionPreview = request.instructions.take(160),
            style = request.style,
            elapsedMs = elapsedMs,
            audioBytes = bytes,
            format = format,
            sampleRate = sampleRate,
            durationMs = durationMs,
            usedFallback = usedFallback,
            success = result.isSuccess && bytes > 0,
            errorMessage = result.exceptionOrNull()?.message,
        )
    }
}
