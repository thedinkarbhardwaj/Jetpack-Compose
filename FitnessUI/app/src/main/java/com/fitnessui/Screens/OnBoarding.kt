package com.fitnessui.Screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.fitnessui.Model.OnBoardingList
import com.fitnessui.Navigation.Route
import com.fitnessui.R
import com.fitnessui.ui.theme.AppColor
import com.fitnessui.ui.theme.AppFont

@Composable
fun OnBoarding(navController: NavHostController,modifier: Modifier = Modifier) {

    var list = onBoardingList()

    var listPos by remember {
        mutableIntStateOf(0)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = modifier
            .fillMaxWidth()) {

            Column(modifier = modifier.fillMaxWidth()
                .weight(1f)) {

                if(list.size > listPos) {
                    OnBoardingUpperDesign(list.get(listPos))
                }else{

                }
            }

            Row(modifier = Modifier.fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 40.dp),
                horizontalArrangement = Arrangement.End) {

                Row(modifier = Modifier.size(50.dp,50.dp)
                    .clip(shape = CircleShape)
                    .background(brush = Brush.linearGradient(listOf(AppColor.Blue,AppColor.LightPink)))
                    .clickable {
                        if (listPos < list.size - 1) {
                            listPos++
                        } else {

                         navController.navigate(Route.Register)

                        }
                    },
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
fun OnBoardingUpperDesign(list: OnBoardingList) {
    Image(painter = painterResource(list.image), contentDescription = "",
        modifier = Modifier.fillMaxWidth()
            .height(400.dp)
            .offset(y = (-20).dp))

    Text(text = list.title, fontSize = 24.sp,
        fontFamily = AppFont.poppinsBold,
        color = AppColor.black,
        modifier = Modifier.padding(vertical = 20.dp, horizontal = 40.dp))

    Text(text = list.desc,
        fontSize = 14.sp,
        fontFamily = AppFont.poppinsRegular,
        color = AppColor.Gray,
        modifier = Modifier.padding(vertical = 1.dp, horizontal = 40.dp))
}

fun onBoardingList(): List<OnBoardingList> {
    return listOf(
        OnBoardingList(R.drawable.onboarding1,"Track Your Goal","Don't worry if you have trouble determining your goals, We can help you determine your goals and track your goals"),
        OnBoardingList(R.drawable.onboarding2,"Get Burn","Let’s keep burning, to achive yours goals, it hurts only temporarily, if you give up now you will be in pain forever"),
        OnBoardingList(R.drawable.onboarding3,"Eat Well","Let's start a healthy lifestyle with us, we can determine your diet every day. healthy eating is fun"),
        OnBoardingList(R.drawable.onboarding4,"Improve Sleep  Quality","Improve the quality of your sleep with us, good quality sleep can bring a good mood in the morning"),

    )
}
