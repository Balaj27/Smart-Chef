package com.gemini.chatbot.model

data class DataResponse(
    val message: String,
    val isAI: Int,  // 0 for user, 1 for AI
    val content: String,
    val imageUri: String = ""
)
