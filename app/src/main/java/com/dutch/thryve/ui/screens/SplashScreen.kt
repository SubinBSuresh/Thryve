package com.dutch.thryve.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.dutch.thryve.domain.model.ConnectionResult
import com.dutch.thryve.domain.model.FirebaseConnector
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavHostController,
    onConnectionResult: (ConnectionResult) -> Unit // Passes the result back up
) {

    var isTitleVisisble by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ConnectionResult?>(null) }
    val firebaseConnector = remember { FirebaseConnector() }

    // 1. Start the connection immediately
    LaunchedEffect(Unit) {
        isTitleVisisble = true
        delay(500L) // Wait for title animation

        // --- CORE FIREBASE CONNECTION/AUTH ---
        val connectionResult = firebaseConnector.initializeAndSignIn()
        result = connectionResult
        onConnectionResult(connectionResult) // Pass the crucial userId/ready state back
    }

    // 2. Navigate when the connection is successful and a minimum time has passed
    LaunchedEffect(result) {
        val currentResult = result
        if (currentResult != null && currentResult.isReady && currentResult.userId != null) {
            delay(500L) // Ensure smooth transition (minimum 2s total time)
            navController.popBackStack()
            navController.navigate(Screen.Dashboard.route)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(visible = isTitleVisisble, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = "THRYVE", fontSize = 48.sp, color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Display progress or error state
        when {
            result?.error != null -> {
                Text(
                    text = "Connection Failed: ${result!!.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            result == null || result?.isReady == false -> {
                // Connecting
                LinearProgressIndicator(
                    modifier = Modifier.width(128.dp), color = MaterialTheme.colorScheme.onPrimary
                )
            }
            // Ready, waiting for navigation delay
            else -> {
                LinearProgressIndicator(
                    modifier = Modifier.width(128.dp),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                )
            }
        }
    }
}