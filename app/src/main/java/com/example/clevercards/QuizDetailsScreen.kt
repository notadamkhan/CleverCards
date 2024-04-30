package com.example.clevercards

import android.content.Context
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.tasks.Tasks
import coil.compose.rememberAsyncImagePainter
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.image.ImageSize
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

@Composable
fun QuizDetailsScreen(quizId: String, quizTitle: String, onQuizUpdated: () -> Unit) {
    var description by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(false) }
    var coverImageUri by remember { mutableStateOf<Uri?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var isGeneratingImage by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }


    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        coverImageUri = uri
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Quiz Details",
            style = MaterialTheme.typography.headlineLarge
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Quiz Description") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Make quiz public")
            Checkbox(
                checked = isPublic,
                onCheckedChange = { isPublic = it }
            )
        }

        coverImageUri?.let { uri ->
            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = "Cover Image",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Text("Cover Image Selected", style = MaterialTheme.typography.bodyMedium)
        }
        Text(text = "Upload or Generate a cover image", style = MaterialTheme.typography.titleMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        isGeneratingImage = true
                        val prompt = "Generate a cover image for a quiz titled: $quizTitle. Description: $description"
                        try {
                            val generatedImageUri = DalleHelper.generateImage(context, prompt)
                            coverImageUri = generatedImageUri
                        } catch (e: Exception) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Image generation failed. Please try again.",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        } finally {
                            isGeneratingImage = false
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Generate Image")
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = {
                    launcher.launch("image/*")
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Upload Image")
            }
        }

        if (isGeneratingImage) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        Button(
            onClick = {
                val uploadTask = if (coverImageUri != null) {
                    DatabaseHelper.uploadImage(coverImageUri!!, "cover_images")
                } else {
                    Tasks.forResult(null)
                }

                uploadTask.addOnSuccessListener { downloadUri ->
                    val coverImageUrl = downloadUri?.toString() ?: ""

                    val updatedQuiz = Quiz(
                        id = quizId,
                        title = quizTitle,
                        description = description,
                        isPublic = isPublic,
                        coverImageUrl = coverImageUrl
                    )

                    DatabaseHelper.updateQuiz(updatedQuiz) { success ->
                        if (success) {
                            onQuizUpdated()
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Quiz update failed. Please try again.",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Next")
        }

        Text(
            text = "Image generation powered by OpenAI DALL·E 3. Image is based on your quiz title and description.",
            style = MaterialTheme.typography.labelSmall
        )
    }
    SnackbarHost(hostState = snackbarHostState)

}

@OptIn(BetaOpenAI::class)
object DalleHelper {
    suspend fun generateImage(context: Context, prompt: String): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val openAi = OpenAI("sk-proj-")
                val imageUrl = openAi.imageURL(
                    creation = ImageCreation(
                        prompt = prompt,
                        model = ModelId("dall-e-3"),
                        n = 1,
                        size = ImageSize.is1024x1024
                    )
                ).first()

                val fileName = "generated_image_${System.currentTimeMillis()}.png"
                val file = File(context.cacheDir, fileName)
                val outputStream = FileOutputStream(file)

                val inputStream = URL(imageUrl.url).openStream()
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                Uri.fromFile(file)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}