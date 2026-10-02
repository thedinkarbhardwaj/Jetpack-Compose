package com.sunpawtechnologies.supawui.naviagtion


import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sunpawtechnologies.supawui.screens.LoginScreen
import com.sunpawtechnologies.supawui.screens.OtpScreen
import com.sunpawtechnologies.supawui.screens.SplashSceen

@Composable
fun AppNavigation() {

    var navController = rememberNavController()

    NavHost(navController = navController, startDestination = NavigationName.splash){

        composable(NavigationName.splash) {
            SplashSceen(navController)
        }


        composable(NavigationName.login) {
            LoginScreen(navController)
        }

        composable(NavigationName.otp) {
            OtpScreen(navController)
        }

    }
}