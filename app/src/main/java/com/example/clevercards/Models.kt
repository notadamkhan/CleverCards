package com.example.clevercards

// Models.kt

data class User(
    val id: String = "",
    val name: String = "",
    val quizzes: List<String> = emptyList()
)

data class Quiz(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val authorId: String = "",
    val isPublic: Boolean = false,
    val coverImageUrl: String = "",
    val questions: List<Question> = emptyList()
)

data class Question(
    val id: String = "",
    val quizId: String = "",
    val text: String = "",
    val imageUrl: String = "",
    val answer: String = "",
    val answerImageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)