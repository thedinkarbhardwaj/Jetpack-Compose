package com.shayriapp.ui.Screens

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.shayriapp.R
import com.shayriapp.ui.routing.ShayriRoutingNames
import com.shayriapp.ui.theme.orangeColor
import com.shayriapp.ui.theme.primaryColor


@Composable
fun SplashScreen(navHostController: NavHostController,modifier: Modifier = Modifier) {

    Surface() {
        Column(modifier = modifier.fillMaxSize().background(primaryColor),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {

            Spacer(modifier = Modifier.weight(1f))
            Image(painter = painterResource(R.drawable.shayri), contentDescription = "",
                modifier = Modifier.size(100.dp))
            Spacer(modifier = Modifier.weight(1f))
            CircularProgressIndicator(modifier = Modifier.padding(bottom = 20.dp),
                color = orangeColor)

        }

        Handler(Looper.getMainLooper()).postDelayed(
            Runnable {

                navHostController.navigate(ShayriRoutingNames.categoryScreen.route)
            },2000
        )
    }

}