package com.thronerideWithJC.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.thronerideWithJC.R

@Composable
fun Splash(navController: NavHostController) {

    LaunchedEffect(key1 = true) {
        // Delay for 3 seconds (for example)
        kotlinx.coroutines.delay(3000)
        // Navigate to the main screen
        navController.navigate("main_screen") {
            // Clear back stack to avoid going back to splash screen
            popUpTo("splash_screen") { inclusive = true }
        }
    }
    Column(
        modifier = Modifier
            .background(color = Color.White)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {


        Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Logo",
        modifier = Modifier.size(200.dp))

    }
}