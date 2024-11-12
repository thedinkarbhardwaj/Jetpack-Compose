package com.fitnessui.Constants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitnessui.R
import com.fitnessui.ui.theme.AppColor
import com.fitnessui.ui.theme.AppFont

object Constanst {

    @Composable
    fun CustomButton(text: String,onClick: () -> Unit) {
        Button(
            onClick =  onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 20.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            AppColor.Blue,
                            AppColor.LightPink
                        )
                    ),
                    shape = RoundedCornerShape(30.dp) // Apply rounded corners here
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent // Set background to transparent to show gradient
            ),
            shape = RoundedCornerShape(30.dp), // Apply rounded corners to the Button itself
            border = BorderStroke(width = 1.dp, color = AppColor.black)
            ) {
            Text(
                text = text,
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}