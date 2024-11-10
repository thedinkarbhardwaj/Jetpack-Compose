package com.fitnessui.Screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.fitnessui.Constants.Constanst
import com.fitnessui.Navigation.Route
import com.fitnessui.R
import com.fitnessui.ui.theme.AppFont
import com.fitnessui.ui.theme.AppColor


@Composable
fun Splash(navHostController: NavHostController) {
    Column (
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        AppColor.lightPurple,
                        AppColor.Purple
                    ) // Define the colors of the gradient
                )
            )
    ) {

        Column(modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = Color.White,
                            fontSize = 36.sp,
                            fontFamily = AppFont.poppinsBold
                        )
                    ) {
                        append("DIDPOOL")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = Color.Black,
                            fontSize = 36.sp,
                            fontFamily = AppFont.poppinsBold
                        )
                    ) {
                        append("Fit")
                    }
                },
                style = MaterialTheme.typography.bodyLarge // You can change this based on your design
            )

            Text(text = "Everybody Can Train", fontSize = 16.sp,
                color = AppColor.white,
                fontFamily = AppFont.poppinsRegular)
        }


        Constanst.CustomButton(stringResource(R.string.get_started),
            onClick = {

                navHostController.navigate(Route.OnBoarding)
                // Your action here
                println("Button clicked!")
                Log.d("Checkedddd","Button clicked!")
            })

        Spacer(Modifier.height(40.dp))
    }
}