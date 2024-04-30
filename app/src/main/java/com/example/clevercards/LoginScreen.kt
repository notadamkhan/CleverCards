package com.example.clevercards

// LoginScreen.kt

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.example.clevercards.DatabaseHelper

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { signInTask ->
                        if (signInTask.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null) {
                                val newUser = User(id = user.uid, name = user.displayName ?: "")
                                DatabaseHelper.createUser(newUser) { success ->
                                    if (success) {
                                        onLoginSuccess()
                                    } else {
                                        Log.e("LoginScreen", "Failed to create user in Firestore")
                                    }
                                }
                            } else {
                                Log.e("LoginScreen", "User is null after Google sign-in")
                            }
                        } else {
                            Log.e("LoginScreen", "Google Sign-in error: ${signInTask.exception?.message}")
                        }
                    }
            } else {
                Log.e("LoginScreen", "Google ID token is null")
            }
        } catch (e: ApiException) {
            Log.e("LoginScreen", "Google Sign-in error: ${e.message}")
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.brain),
                contentDescription = "CleverCards logo",
                modifier = Modifier.size(100.dp)
            )
            Text(
                text = "Welcome to CleverCards",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(2.dp)
            )
            Text(
                text = "Please sign in or sign up to continue",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
            if (errorMessage != null) {
                Snackbar(
                    modifier = Modifier
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally),
                    action = {
                        TextButton(onClick = { errorMessage = null }) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    }
                ) {
                    Text(errorMessage!!)
                }
            }
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.padding(8.dp)
            )
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.padding(8.dp),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Email and password must not be empty"
                        } else {
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { signInTask ->
                                    if (signInTask.isSuccessful) {
                                        onLoginSuccess()
                                    } else {
                                        errorMessage =
                                            "Sign-in error: ${signInTask.exception?.message}"
                                    }
                                }
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Sign In")
                }
                OutlinedButton(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Email and password must not be empty"
                        } else {
                            auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener { signUpTask ->
                                    if (signUpTask.isSuccessful) {
                                        val user = auth.currentUser
                                        if (user != null) {
                                            val newUser =
                                                User(id = user.uid, name = user.email ?: "")
                                            DatabaseHelper.createUser(newUser) { success ->
                                                if (success) {
                                                    onLoginSuccess()
                                                } else {
                                                    errorMessage =
                                                        "Sign-up error: ${signUpTask.exception?.message}"
                                                }
                                            }
                                        } else {
                                            errorMessage =
                                                "Sign-up error: ${signUpTask.exception?.message}"
                                        }
                                    } else {
                                        errorMessage =
                                            "Sign-up error: ${signUpTask.exception?.message}"
                                    }
                                }
                        }
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Sign Up")
                }
            }
            TextButton(
                onClick = {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestIdToken("382807194516-9bsi16ilv6vkloju33foqofu61ciif0m.apps.googleusercontent.com")
                        .build()
                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                    launcher.launch(googleSignInClient.signInIntent)
                },
            ) {
                Text("Sign In with Google")
            }

        }
    }

}