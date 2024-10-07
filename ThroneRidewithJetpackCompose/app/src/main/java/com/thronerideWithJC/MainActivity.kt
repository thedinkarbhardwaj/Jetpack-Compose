package com.thronerideWithJC

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.thronerideWithJC.Screens.Splash
import com.thronerideWithJC.ui.theme.ThroneRideWithJetpackComposeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThroneRideWithJetpackComposeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    var navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "splash_screen"){
                        composable("splash_screen") {
                            Splash(navController)
                        }

                        composable("main_screen") {
                            Homeact()
                        }
                    }


                }
            }


        }
    }

    @Composable
    fun Homeact() {
        Text(text = "Home")
    }
}

