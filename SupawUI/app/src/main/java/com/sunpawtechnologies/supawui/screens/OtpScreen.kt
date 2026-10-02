package com.sunpawtechnologies.supawui.screens

import android.widget.Space
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sunpawtechnologies.supawui.R
import com.sunpawtechnologies.supawui.naviagtion.NavigationName.otp
import com.sunpawtechnologies.supawui.ui.theme.Primary

@Composable
fun OtpScreen(navController: NavHostController) {

    var otp by rememberSaveable {
        mutableStateOf("")
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {

            IconButton(onClick = {
                navController.popBackStack()
            }) {
                Icon(Icons.Default.ArrowBack,"")
            }

            Image(painter = painterResource(R.drawable.headerlogo),"",
                modifier = Modifier.size(100.dp))

            Spacer(modifier = Modifier.size(1.dp))


        }

        Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.Center) {

            Text("Enter the OTP", style = TextStyle(fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp, fontSize = 20.sp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "We just sent an otp verification code to this +91-9876543210",
                style = TextStyle(
                    color = Color.Gray,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(20.dp))

            OtpInput(
                otp = otp,
                onOtpChange = { otp = it },
                length = 6
            )
            Spacer(modifier = Modifier.height(20.dp))

            ElevatedButton(onClick = {}, modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.White,
                    containerColor = Primary
                ),
                shape = RoundedCornerShape(16.dp),
                ) {

                Text("Verify OTP", style = TextStyle(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                ))
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("Didn't received the code? ")
                Spacer(Modifier.width(2.dp))
                Text("Resend", style = TextStyle(color = Color.Magenta))
            }
        }

    }

}

@Composable
fun OtpInput(
    otp: String,
    onOtpChange: (String) -> Unit,
    length: Int = 6
) {

    BasicTextField(
        value = otp,
        onValueChange = { newValue ->

            if (newValue.length <= length &&
                newValue.all { it.isDigit() }
            ) {
                onOtpChange(newValue)
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        ),
        decorationBox = {

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                repeat(length) { index ->

                    val number = if (index < otp.length) {
                        otp[index].toString()
                    } else {
                        ""
                    }

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .border(
                                width = 1.dp,
                                color = Color.Gray,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = number,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    )
}