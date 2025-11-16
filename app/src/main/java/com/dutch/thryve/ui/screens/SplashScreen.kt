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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.dutch.thryve.domain.model.ConnectionResult
import com.dutch.thryve.ui.viewmodel.FirebaseViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavHostController, viewModel: FirebaseViewModel = hiltViewModel()
) {
    var isTitleVisisble by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ConnectionResult?>(null) }


//    LaunchedEffect(Unit) {
//        isTitleVisisble = true
//        delay(500L)
//
//        val connectionResult = firebaseConnector.initializeAndSignIn()
//        result = connectionResult
//    }

    LaunchedEffect(Unit) {
        delay(1000)
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid != null) {
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
        AnimatedVisibility(visible = isTitleVisisble, enter = fadeIn(), exit = fadeOut()) {
            Text(
                text = "THRYVE", fontSize = 48.sp, color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        when {
            result?.error != null -> {
                Text(
                    text = "Connection Failed: ${result!!.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            result == null || result?.isReady == false -> {
                LinearProgressIndicator(
                    modifier = Modifier.width(128.dp), color = MaterialTheme.colorScheme.onPrimary
                )
            }

            else -> {
                LinearProgressIndicator(
                    modifier = Modifier.width(128.dp),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                )
            }
        }
    }
}