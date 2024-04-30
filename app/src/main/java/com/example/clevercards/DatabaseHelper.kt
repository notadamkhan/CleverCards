package com.example.clevercards

// DatabaseHelper.kt

import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.dynamiclinks.androidParameters
import com.google.firebase.dynamiclinks.dynamicLink
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.dynamiclinks.shortLinkAsync
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

// when a user creates a new quiz, call DatabaseHelper.createQuiz() to save the quiz data to Firestore
//  DatabaseHelper.uploadImage() to upload the cover image to Cloud Storage.
//  DatabaseHelper.getQuiz() to retrieve quiz data and display it
object DatabaseHelper {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = Firebase.storage


    // User-related functions
    fun createUser(user: User, onComplete: (Boolean) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid)
                .set(user)
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        } else {
            onComplete(false)
        }
    }


    fun getPublicQuizzes(onComplete: (List<Quiz>) -> Unit) {
        db.collection("quizzes")
            .whereEqualTo("isPublic", true)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val quizzes = querySnapshot.documents.mapNotNull { document ->
                    document.toObject(Quiz::class.java)
                }
                onComplete(quizzes)
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }

    fun getUser(userId: String, onComplete: (User?) -> Unit) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val user = document.toObject(User::class.java)
                    onComplete(user)
                } else {
                    onComplete(null)
                }
            }
            .addOnFailureListener { onComplete(null) }
    }

    fun uploadImage(imageUri: Uri, folder: String): Task<Uri> {
        val storageRef = storage.reference
        val imageRef = storageRef.child("$folder/${System.currentTimeMillis()}.jpg")
        val uploadTask = imageRef.putFile(imageUri)

        return uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            imageRef.downloadUrl
        }
    }

    // Quiz-related functions
    fun createQuiz(quiz: Quiz, onComplete: (String?) -> Unit) {
        Log.d("DatabaseHelper", "Creating quiz in Firestore: $quiz")
        db.collection("quizzes")
            .add(quiz)
            .addOnSuccessListener { documentReference ->
                val quizId = documentReference.id
                Log.d("DatabaseHelper", "Quiz created with ID: $quizId")
                db.collection("quizzes").document(quizId)
                    .update(
                        mapOf(
                            "id" to quizId,
                            "title" to quiz.title
                        )
                    )
                    .addOnSuccessListener {
                        Log.d("DatabaseHelper", "Quiz ID and title updated successfully")
                        updateUserQuizzes(quiz.authorId, quizId) { success ->
                            if (success) {
                                Log.d("DatabaseHelper", "User quizzes updated successfully")
                                onComplete(quizId)
                            } else {
                                Log.e("DatabaseHelper", "Failed to update user quizzes")
                                onComplete(null)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("DatabaseHelper", "Failed to update quiz ID and title", e)
                        onComplete(null)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("DatabaseHelper", "Failed to create quiz", e)
                onComplete(null)
            }
    }

    fun addQuizQuestions(quizId: String, questions: List<Question>, onComplete: (Boolean) -> Unit) {
        val batch = Firebase.firestore.batch()

        questions.forEach { question ->
            val questionRef = Firebase.firestore.collection("quizzes").document(quizId)
                .collection("questions").document()
            batch.set(questionRef, question)
        }

        batch.commit()
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }

    private fun updateUserQuizzes(userId: String, quizId: String, onComplete: (Boolean) -> Unit) {
        val userRef = db.collection("users").document(userId)
        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    val updatedQuizzes = user?.quizzes?.toMutableList() ?: mutableListOf()
                    updatedQuizzes.add(quizId)
                    userRef.update("quizzes", updatedQuizzes)
                        .addOnSuccessListener {
                            onComplete(true)
                        }
                        .addOnFailureListener {
                            onComplete(false)
                        }
                } else {
                    onComplete(false)
                }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }
    fun updateQuiz(quiz: Quiz, onComplete: (Boolean) -> Unit) {
        val quizId = quiz.id
        if (quizId.isNotBlank()) {
            val quizRef = db.collection("quizzes").document(quizId)
            quizRef.update(
                mapOf(
                    "title" to quiz.title,
                    "description" to quiz.description,
                    "isPublic" to quiz.isPublic,
                    "coverImageUrl" to quiz.coverImageUrl
                )
            )
                .addOnSuccessListener {
                    onComplete(true)
                }
                .addOnFailureListener {
                    onComplete(false)
                }
        } else {
            onComplete(false)
        }
    }

    fun getQuiz(quizId: String, onComplete: (Quiz?) -> Unit) {
        db.collection("quizzes").document(quizId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val quiz = document.toObject(Quiz::class.java)
                    onComplete(quiz)
                } else {
                    onComplete(null)
                }
            }
            .addOnFailureListener { onComplete(null) }
    }

    fun getQuizQuestions(quizId: String, onComplete: (List<Question>) -> Unit) {
        db.collection("quizzes").document(quizId)
            .collection("questions")
            .orderBy("text")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val questions = querySnapshot.documents.mapNotNull { it.toObject(Question::class.java) }
                onComplete(questions)
            }
            .addOnFailureListener {
                onComplete(emptyList())
            }
    }
    // Firebase Dynamic Links functions
    fun createDynamicLink(quizId: String, onComplete: (String?) -> Unit) {
        val dynamicLink = Firebase.dynamicLinks.dynamicLink {
            link = Uri.parse("https://clevercards.page.link/quiz/$quizId")
            domainUriPrefix = "https://clevercards.page.link"
            androidParameters {
                minimumVersion = 1
            }
        }
        val shortLinkTask = Firebase.dynamicLinks.shortLinkAsync {
            longLink = dynamicLink.uri
        }.addOnSuccessListener { result ->
            val shortLink = result.shortLink
            onComplete(shortLink.toString())
        }.addOnFailureListener {
            onComplete(null)
        }
    }
}