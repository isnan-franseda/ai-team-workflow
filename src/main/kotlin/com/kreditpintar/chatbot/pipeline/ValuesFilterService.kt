package com.kreditpintar.chatbot.pipeline

import com.kreditpintar.chatbot.config.MiniMaxClient
import com.kreditpintar.chatbot.config.ValuesFilterPrompt
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

data class FilteredWebResult(
    val title: String,
    val content: String,
    val url: String,
    val passed: Boolean,
    val filterReason: String?
)

@Service
class ValuesFilterService(
    private val miniMaxClient: MiniMaxClient
) {
    fun filter(webResults: List<WebSearchEntry>, sessionId: String): List<FilteredWebResult> {
        return webResults.map { result ->
            val prompt = ValuesFilterPrompt.VALUES_FILTER_PROMPT.replace("{web_content}", result.content)

            val validationResult = try {
                miniMaxClient.validateOutput(result.content, prompt)
            } catch (e: Exception) {
                logger.error(e) { "Values filter call failed for session=$sessionId, url=${result.url}" }
                "FAIL\nAlasan: Gagal memvalidasi konten"
            }

            val passed = validationResult.trim().startsWith("PASS", ignoreCase = true)
            val reason = if (!passed) {
                val reasonLine = validationResult.lines().find { it.startsWith("Alasan:") }
                reasonLine?.removePrefix("Alasan:")?.trim() ?: "Gagal validasi nilai"
            } else null

            if (!passed) {
                logger.info { "Values filter REJECTED web result for session=$sessionId: url=${result.url}, reason=$reason" }
            }

            FilteredWebResult(
                title = result.title,
                content = result.content,
                url = result.url,
                passed = passed,
                filterReason = reason
            )
        }.also { results ->
            val passCount = results.count { it.passed }
            val failCount = results.count { !it.passed }
            logger.info { "Values filter: $passCount passed, $failCount failed for session=$sessionId" }
        }
    }
}
