package com.kreditpintar.chatbot.service

import com.kreditpintar.chatbot.config.ChatbotProperties
import com.kreditpintar.chatbot.config.MiniMaxClient
import com.kreditpintar.chatbot.config.OutputValidatorPrompt
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

data class ValidationResult(
    val passed: Boolean,
    val reason: String?,
)

@Service
class OutputValidatorService(
    private val miniMaxClient: MiniMaxClient,
    private val properties: ChatbotProperties,
) {
    fun validate(
        response: String,
        sessionId: String,
    ): ValidationResult {
        val prompt = OutputValidatorPrompt.VALIDATOR_PROMPT.replace("{response}", response)

        val validationResult =
            try {
                miniMaxClient.validateOutput(response, prompt)
            } catch (e: Exception) {
                logger.error(e) { "Output validator call failed for session=$sessionId" }
                return ValidationResult(
                    passed = false,
                    reason = "Gagal memvalidasi output: ${e.message}",
                )
            }

        val passed = validationResult.trim().startsWith("PASS", ignoreCase = true)
        val reason =
            if (!passed) {
                val reasonLine = validationResult.lines().find { it.startsWith("Alasan:") }
                reasonLine?.removePrefix("Alasan:")?.trim() ?: "Validasi gagal"
            } else {
                null
            }

        if (!passed) {
            logger.warn { "Output validator REJECTED response for session=$sessionId: reason=$reason" }
        } else {
            logger.info { "Output validator PASSED response for session=$sessionId" }
        }

        return ValidationResult(passed = passed, reason = reason)
    }
}
