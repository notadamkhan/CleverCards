package com.example.clevercards
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

@Composable
fun rememberFirebaseAuth(): FirebaseAuth {
    return remember { FirebaseAuth.getInstance() }
}

@Composable
fun rememberFirebaseUser(auth: FirebaseAuth = rememberFirebaseAuth()): FirebaseUser? {
    return remember { auth.currentUser }
}