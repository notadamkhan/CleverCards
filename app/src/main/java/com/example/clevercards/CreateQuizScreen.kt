package com.example.clevercards

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun CreateQuizScreen(onQuizCreated: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showErrorSnackbar by remember { mutableStateOf(false) }
    if (errorMessage != null) {
        showErrorSnackbar = true
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create a New Quiz",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Quiz Title") },
            modifier = Modifier.fillMaxWidth(),
            isError = errorMessage != null
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    if (title.isNotBlank()) {
                        val quiz = Quiz(
                            title = title,
                            authorId = currentUser.uid
                        )
                        DatabaseHelper.createQuiz(quiz) { quizId ->
                            if (quizId != null) {
                                onQuizCreated(quizId, title)
                            } else {
                                errorMessage = "Failed to create quiz. Please try again."
                            }
                        }
                    } else {
                        errorMessage = "Please enter a quiz title."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Quiz")
        }
        if (showErrorSnackbar) {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                action = {
                }
            ) {
                Text(
                    text = errorMessage ?: "An error occurred.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        }
    }
}