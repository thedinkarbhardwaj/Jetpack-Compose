package com.sunpawtechnologies.supawui.screens


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sunpawtechnologies.supawui.R
import com.sunpawtechnologies.supawui.naviagtion.NavigationName
import com.sunpawtechnologies.supawui.ui.theme.Primary
import kotlinx.coroutines.delay


@Composable
fun SplashSceen(navController: NavHostController) {

    LaunchedEffect(Unit) {

        delay(2000)
        navController.navigate(NavigationName.login){

        }

    }

    Column(modifier = Modifier.background(Primary)) {

        Column(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(R.drawable.logo), contentDescription = "",
                modifier = Modifier.size(200.dp))

            Text("Inventory Mangement App", style = TextStyle(
                color = Color.White,
                fontSize = 20.sp
            ))
        }

//        Column(modifier = Modifier) { }
        Text("V1.0", style = TextStyle(color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth().padding(20.dp))
    }

}