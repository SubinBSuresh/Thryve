package com.dutch.thryve.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.dutch.thryve.BuildConfig
import com.dutch.thryve.ui.viewmodel.FirebaseViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@Composable
fun SplashScreen(
    navController: NavHostController, viewModel: FirebaseViewModel = hiltViewModel()
) {
    val TAG = "Dutch__SplashScreen"
    var isTitleVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isTitleVisible = true
        delay(2000) // Keep splash visible for 2 seconds to allow animation to finish

        val auth = FirebaseAuth.getInstance()
        var currentUser = auth.currentUser

        if (currentUser == null) {
            try {
                // Using test credentials from BuildConfig
                val testEmail = BuildConfig.TEST_EMAIL
                val testPassword = BuildConfig.TEST_PASSWORD
                auth.signInWithEmailAndPassword(testEmail, testPassword).await()
                currentUser = auth.currentUser
            } catch (e: Exception) {
                Log.e(TAG, "Sign-in failed", e)
                return@LaunchedEffect
            }
        }

        if (currentUser != null) {
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(0)
            }
        } else {
//            navController.navigate(Screen.Login.route) {
//                popUpTo(0)
//            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = isTitleVisible,
            enter = fadeIn(animationSpec = tween(1500)),
            exit = fadeOut()
        ) {
            Text(
                text = "THRYVE", fontSize = 48.sp, color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
