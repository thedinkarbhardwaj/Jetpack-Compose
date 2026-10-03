package com.sunpawtechnologies.supawui.naviagtion


import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sunpawtechnologies.supawui.screens.AddProductScreen
import com.sunpawtechnologies.supawui.screens.HomeScreen
import com.sunpawtechnologies.supawui.screens.LoginScreen
import com.sunpawtechnologies.supawui.screens.OtpScreen
import com.sunpawtechnologies.supawui.screens.SplashSceen

@Composable
fun AppNavigation() {

    var navController = rememberNavController()

    NavHost(navController = navController, startDestination = NavigationName.home){

        composable(NavigationName.splash) {
            SplashSceen(navController)
        }


        composable(NavigationName.login) {
            LoginScreen(navController)
        }

        composable("${NavigationName.otp}/{phnNumber}") { backStackEntry ->

            val phoneNumber =
                backStackEntry.arguments?.getString("phnNumber") ?: ""

            OtpScreen(navController,phoneNumber)
        }

        composable(NavigationName.home) {
            HomeScreen(navController)
        }


        composable("${NavigationName.addProduct}/{name}") {backStackEntry->

            var productName = backStackEntry.arguments?.getString("name") ?: ""

            AddProductScreen(navController,productName)
        }

    }
}