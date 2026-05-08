package com.kreditpintar.chatbot.config

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.TextBlock
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@ConditionalOnProperty(name = ["llm.chat.provider"], havingValue = "anthropic")
@Service
class AnthropicChatClient(
    private val properties: LlmProperties,
) : ChatClient {
    private val client: AnthropicClient by lazy {
        AnthropicOkHttpClient.builder()
            .apiKey(properties.chat.apiKey)
            .build()
    }

    override fun chat(
        systemPrompt: String,
        context: String,
        history: List<Pair<String, String>>,
        userMessage: String,
        options: ChatOptions?,
    ): ChatResult {
        val maxTokens = options?.maxTokens?.toLong() ?: 2048L

        val builder = MessageCreateParams.builder()
            .model(properties.chat.model)
            .maxTokens(maxTokens)
            .system("$systemPrompt\n\n$context")

        history.forEach { (role, content) ->
            when (role) {
                "user" -> builder.addUserMessage(content)
                "assistant" -> builder.addUserMessage(content)
                else -> builder.addUserMessage(content)
            }
        }

        builder.addUserMessage(userMessage)

        if (options?.temperature != null) {
            builder.temperature(options.temperature.toDouble())
        }

        val request = builder.build()

        return try {
            val response = client.messages().create(request)
            val textBlocks = response.content().filterIsInstance<TextBlock>()
            val content = textBlocks.joinToString("") { it.text() }
            ChatResult(content = content)
        } catch (e: Exception) {
            logger.error(e) { "Anthropic chat call failed" }
            throw e
        }
    }

    override fun validateOutput(
        response: String,
        validationPrompt: String,
    ): String {
        val result = chat(
            systemPrompt = validationPrompt,
            context = "",
            history = emptyList(),
            userMessage = response,
            options = ChatOptions(temperature = 0.0f),
        )
        return result.content
    }
}
