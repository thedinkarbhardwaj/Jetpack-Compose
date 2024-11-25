package com.fitnessui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fitnessui.Navigation.Route
import com.fitnessui.Screens.OnBoarding
import com.fitnessui.Screens.RegCompleteProf
import com.fitnessui.Screens.Register
import com.fitnessui.Screens.Splash
import com.fitnessui.ui.theme.FitnessUITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // enableEdgeToEdge()
        setContent {
            FitnessUITheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    var navController = rememberNavController()

                    NavHost(navController = navController, startDestination = Route.Register,
                        builder = {

                            composable(route = Route.Splash){
                                Splash(navController)
                            }

                            composable(route = Route.OnBoarding){
                                OnBoarding(navController)
                            }

                            composable(route = Route.Register){
                                Register(navController)
                            }


                            composable(route = Route.RegCompleteProf){
                                RegCompleteProf(navController)
                            }
                        })

                }
            }
        }
    }
}
