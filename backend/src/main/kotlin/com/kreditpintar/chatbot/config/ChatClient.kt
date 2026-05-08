package com.kreditpintar.chatbot.config

data class Message(
    val role: String,
    val content: String,
)

data class ChatResult(
    val content: String,
)

data class ChatOptions(
    val temperature: Float? = null,
    val maxTokens: Int? = null,
)

interface ChatClient {
    fun chat(
        systemPrompt: String,
        context: String,
        history: List<Pair<String, String>>,
        userMessage: String,
        options: ChatOptions? = null,
    ): ChatResult

    fun validateOutput(
        response: String,
        validationPrompt: String,
    ): String
}
