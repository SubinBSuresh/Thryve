package com.dutch.thryve.ui.screens

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dutch.thryve.ThryveApplication.Companion.LOG_TAG
import com.dutch.thryve.domain.model.ConnectionResult
import com.dutch.thryve.ui.theme.ThryveTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val TAG = LOG_TAG + "__MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThryveTheme {
                MainScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Splash : Screen("splash", "Splash", Icons.Default.Home)
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object Activity : Screen("activity", "Activity", Icons.Default.Person)
    object Nutrition : Screen("nutrition", "Nutrition", Icons.Default.Star)
    object Progress : Screen("progress", "Progress", Icons.Default.KeyboardArrowUp)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object PR : Screen("pr", "PR", Icons.Default.Star)
}

val bottomNavItems = listOf(Screen.Dashboard, Screen.PR)

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var connectionResult by remember { mutableStateOf<ConnectionResult?>(null) }

    // When a connection result comes in, if successful, create the ViewModel
    LaunchedEffect(connectionResult) {
        val result = connectionResult
        if (result != null && result.isReady && result.userId != null) {
            // FIREBASE IS CONNECTED. Now create the data layer with the known userId.

//            dailyViewModel = DailyViewModel(repository, result.userId)
        }
    }

    Scaffold(bottomBar = {
        if (currentRoute != Screen.Splash.route) {
            ThryveBottomBar(navController = navController, currentRoute = currentRoute)
        }
    }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                // The Splash screen performs the connection and updates the result state here
                SplashScreen(navController)
            }
            composable(Screen.Dashboard.route) { DailyScreen(navController) }
            composable(Screen.PR.route) { PRScreen(navController) }
        }
    }
}

@Composable
fun ThryveBottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        bottomNavItems.forEach { screen ->
            val selectedScreen = currentRoute == screen.route
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = selectedScreen,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Dashboard.route) {
                                saveState = true
                                inclusive = false
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                })
        }
    }
}


@Composable
fun PlaceHolderScreen(name: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {

        Text(
            "THR YVE: $name Screen\n(P1 Structure Complete, P2 Development Starts Now)",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!", modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ThryveTheme {
        Greeting("Android")
    }
}