package com.example.clevercards

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign

@Composable
fun PracticeScreen(quizId: String, onBack: () -> Unit) {
    val quiz = remember { mutableStateOf<Quiz?>(null) }
    val questions = remember { mutableStateListOf<Question>() }
    val currentQuestionIndex = remember { mutableStateOf(0) }
    val showAnswer = remember { mutableStateOf(false) }
    val context = LocalContext.current


    LaunchedEffect(quizId) {
        RecentlyPracticedQuizzes.saveRecentlyPracticedQuizId(context, quizId)
        DatabaseHelper.getQuiz(quizId) { fetchedQuiz ->
            quiz.value = fetchedQuiz
            fetchedQuiz?.let {
                DatabaseHelper.getQuizQuestions(quizId) { fetchedQuestions ->
                    questions.clear()
                    questions.addAll(fetchedQuestions)
                }
            }
        }
    }

    val currentQuestion = questions.getOrNull(currentQuestionIndex.value)

    @OptIn(ExperimentalAnimationApi::class)
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = quiz.value?.title ?: "No Title",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp)
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .weight(1f)
                .clickable { showAnswer.value = !showAnswer.value }
        ) {
            val rotation = animateFloatAsState(
                targetValue = if (showAnswer.value) 180f else 0f,
                animationSpec = tween(durationMillis = 400), label = "rotation"
            )

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationX = rotation.value
                        cameraDistance = 8 * density
                    }
            ) {
                if (rotation.value <= 90f) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = currentQuestion?.text ?: "",
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            if (currentQuestion?.imageUrl?.isNotEmpty() == true) {
                                Image(
                                    painter = rememberAsyncImagePainter(currentQuestion.imageUrl),
                                    contentDescription = "Question Image",
                                    modifier = Modifier
                                        .size(200.dp)
                                        .padding(vertical = 8.dp)
                                )
                            }
                            Text(
                                text = "Question",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationX = 180f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = currentQuestion?.answer ?: "",
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            if (currentQuestion?.answerImageUrl?.isNotEmpty() == true) {
                                Image(
                                    painter = rememberAsyncImagePainter(currentQuestion.answerImageUrl),
                                    contentDescription = "Answer Image",
                                    modifier = Modifier
                                        .size(200.dp)
                                        .padding(vertical = 8.dp)
                                )
                            }
                            Text(
                                text = "Answer",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = {
                    if (currentQuestionIndex.value > 0) {
                        currentQuestionIndex.value--
                        showAnswer.value = false
                    }
                },
                enabled = currentQuestionIndex.value > 0
            ) {
                Text("Previous")
            }

            Button(
                onClick = {
                    if (currentQuestionIndex.value < questions.lastIndex) {
                        currentQuestionIndex.value++
                        showAnswer.value = false
                    }
                },
                enabled = currentQuestionIndex.value < questions.lastIndex
            ) {
                Text("Next")
            }
        }

        LinearProgressIndicator(
            progress = (currentQuestionIndex.value + 1).toFloat() / questions.size,
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        )
    }
}