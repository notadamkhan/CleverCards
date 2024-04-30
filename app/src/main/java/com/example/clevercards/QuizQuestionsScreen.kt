package com.example.clevercards

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.tasks.Tasks
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.TextButton


@Composable
fun QuizQuestionsScreen(quizId: String, onQuizCompleted: () -> Unit) {
    val questionList = remember { mutableStateListOf<Question>() }
    var questionText by remember { mutableStateOf("") }
    var answerText by remember { mutableStateOf("") }
    var questionImageUri by remember { mutableStateOf<Uri?>(null) }
    var answerImageUri by remember { mutableStateOf<Uri?>(null) }
    var showQuestionImageSnackbar by remember { mutableStateOf(false) }
    var showAnswerImageSnackbar by remember { mutableStateOf(false) }

    val questionImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            questionImageUri = uri
            showQuestionImageSnackbar = true
        }
    }

    val answerImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            answerImageUri = uri
            showAnswerImageSnackbar = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Add Questions",
            style = MaterialTheme.typography.headlineLarge
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = questionText,
                onValueChange = { questionText = it },
                label = { Text("Question Text") },
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    questionImageLauncher.launch("image/*")
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.imageup),
                    contentDescription = "Select Question Image"
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = answerText,
                onValueChange = { answerText = it },
                label = { Text("Answer Text") },
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    answerImageLauncher.launch("image/*")
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.imageup),
                    contentDescription = "Select Answer Image"
                )
            }

        }

        Button(
            onClick = {
                val questionImageUploadTask = if (questionImageUri != null) {
                    DatabaseHelper.uploadImage(questionImageUri!!, "question_images")
                } else {
                    Tasks.forResult(null)
                }

                val answerImageUploadTask = if (answerImageUri != null) {
                    DatabaseHelper.uploadImage(answerImageUri!!, "answer_images")
                } else {
                    Tasks.forResult(null)
                }

                Tasks.whenAllSuccess<Uri>(questionImageUploadTask, answerImageUploadTask)
                    .addOnSuccessListener { uris ->
                        val questionImageUrl = uris[0]?.toString() ?: ""
                        val answerImageUrl = uris[1]?.toString() ?: ""

                        val question = Question(
                            quizId = quizId,
                            text = questionText,
                            imageUrl = questionImageUrl,
                            answer = answerText,
                            answerImageUrl = answerImageUrl
                        )
                        questionList.add(question)
                        questionText = ""
                        answerText = ""
                        questionImageUri = null
                        answerImageUri = null
                    }
                    .addOnFailureListener {
                        // Handle image upload failure
                    }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Question")
        }

        Text(
            text = "Quiz Questions",
            style = MaterialTheme.typography.headlineSmall
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(questionList) { question ->
                QuestionItem(question = question)
            }
        }

        Button(
            onClick = {
                DatabaseHelper.addQuizQuestions(quizId, questionList) { success ->
                    if (success) {
                        onQuizCompleted()
                    } else {
                        // Handle error
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Finish Quiz Creation")
        }
    }

    if (showQuestionImageSnackbar) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(
                    onClick = { showQuestionImageSnackbar = false }
                ) {
                    Text(
                        text = "Dismiss",
                        color = MaterialTheme.colorScheme.inversePrimary
                    )
                }
            }
        ) {
            Text(
                text = "Question image selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
        }
    }

    if (showAnswerImageSnackbar) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(
                    onClick = { showAnswerImageSnackbar = false }
                ) {
                    Text(
                        text = "Dismiss",
                        color = MaterialTheme.colorScheme.inversePrimary
                    )
                }
            }
        ) {
            Text(
                text = "Answer image selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
        }
    }
}

@Composable
fun QuestionItem(question: Question) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question: ${question.text}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                question.imageUrl?.let { imageUrl ->
                    Image(
                        painter = rememberAsyncImagePainter(imageUrl),
                        contentDescription = "Question Image",
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Answer: ${question.answer}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                question.answerImageUrl?.let { imageUrl ->
                    Image(
                        painter = rememberAsyncImagePainter(imageUrl),
                        contentDescription = "Answer Image",
                        modifier = Modifier.size(60.dp)
                    )
                }
            }
        }
    }
}
