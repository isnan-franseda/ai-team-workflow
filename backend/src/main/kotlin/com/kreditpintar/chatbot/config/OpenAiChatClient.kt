package com.kreditpintar.chatbot.config

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.chat.completions.ChatCompletionCreateParams
import mu.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@ConditionalOnProperty(name = ["llm.chat.provider"], havingValue = "openai", matchIfMissing = true)
@Service
class OpenAiChatClient(
    private val properties: LlmProperties,
) : ChatClient {
    private val client: OpenAIClient by lazy {
        OpenAIOkHttpClient.builder()
            .apiKey(properties.chat.apiKey)
            .baseUrl(properties.chat.baseUrl)
            .build()
    }

    override fun chat(
        systemPrompt: String,
        context: String,
        history: List<Pair<String, String>>,
        userMessage: String,
        options: ChatOptions?,
    ): ChatResult {
        val builder = ChatCompletionCreateParams.builder()
            .model(properties.chat.model)
            .addSystemMessage("$systemPrompt\n\n$context")

        history.forEach { (role, content) ->
            when (role) {
                "user" -> builder.addUserMessage(content)
                "assistant" -> builder.addAssistantMessage(content)
                else -> builder.addUserMessage(content)
            }
        }

        builder.addUserMessage(userMessage)

        if (options?.temperature != null) {
            builder.temperature(options.temperature.toDouble())
        }
        if (options?.maxTokens != null) {
            builder.maxTokens(options.maxTokens.toLong())
        }

        val request = builder.build()

        return try {
            val response = client.chat().completions().create(request)
            val content = response.choices()[0].message().content().orElse("")
            ChatResult(content = content)
        } catch (e: Exception) {
            logger.error(e) { "OpenAI chat call failed" }
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
