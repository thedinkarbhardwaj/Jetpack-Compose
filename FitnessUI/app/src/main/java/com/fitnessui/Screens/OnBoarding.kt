package com.fitnessui.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitnessui.R
import com.fitnessui.ui.theme.AppColor
import com.fitnessui.ui.theme.AppFont

@Composable
fun OnBoarding(modifier: Modifier = Modifier) {

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = modifier
            .fillMaxWidth()) {


            Column(modifier = modifier.fillMaxWidth()
                .weight(1f)) {

                OnBoardingUpperDesign()
            }

            Row(modifier = Modifier.fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 40.dp),
                horizontalArrangement = Arrangement.End) {

                Row(modifier = Modifier.size(50.dp,50.dp)
                    .clip(shape = CircleShape)
                    .background(brush = Brush.linearGradient(listOf(AppColor.Blue,AppColor.LightPink))),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "", tint = AppColor.white,
                        modifier = Modifier.size(30.dp)
                    )
                }

            }


        }

    }
}

@Composable
fun OnBoardingUpperDesign() {
    Image(painter = painterResource(R.drawable.onboarding1), contentDescription = "",
        modifier = Modifier.fillMaxWidth()
            .height(400.dp)
            .offset(y = (-20).dp))

    Text(text = "Track Your Goal", fontSize = 24.sp,
        fontFamily = AppFont.poppinsBold,
        color = AppColor.black,
        modifier = Modifier.padding(vertical = 20.dp, horizontal = 40.dp))

    Text(text = "Don't worry if you have trouble determining your goals, We can help you determine your goals and track your goals",
        fontSize = 14.sp,
        fontFamily = AppFont.poppinsRegular,
        color = AppColor.Gray,
        modifier = Modifier.padding(vertical = 1.dp, horizontal = 40.dp))
}
