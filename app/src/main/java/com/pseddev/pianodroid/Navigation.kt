package com.pseddev.pianodroid

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pseddev.pianodroid.ui.ListenScreen
import com.pseddev.pianodroid.ui.MainScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(onListen = { navController.navigate("listen") })
        }
        composable("listen") {
            ListenScreen(onBack = { navController.popBackStack() })
        }
    }
}
