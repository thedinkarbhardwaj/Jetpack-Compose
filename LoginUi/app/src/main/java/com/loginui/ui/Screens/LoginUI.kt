package com.loginui.ui.Screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loginui.MainActivity
import com.loginui.R
import com.loginui.ui.theme.AppColors


@Composable
fun LoginUI(context:MainActivity,modifier: Modifier = Modifier) {

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Image(painter = painterResource(R.drawable.login), contentDescription = "",
            modifier = Modifier.size(200.dp))

        Text("Welcome Back", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text("Login to your account", fontSize = 14.sp)

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(value = email, onValueChange = {
            email = it
        }, label = {
            Text("Email Address")
        },
            singleLine = true)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = password, onValueChange = {
            password = it
        }, label = {
            Text("Password")
        },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation())

        Spacer(modifier = Modifier.height(2.dp))
        Text("Forgot Password?", textAlign = TextAlign.End, fontSize = 14.sp, color = Color.Blue,
            fontFamily = FontFamily.Serif,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 34.dp)
                .clickable {

                })

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {

            valid(email,password)
            Toast.makeText(context,"Login",Toast.LENGTH_SHORT).show()

        },
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 10.dp,
                pressedElevation = 6.dp,
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.blue,
                contentColor = Color.White
            ),
            border = BorderStroke(width = 1.dp, color = Color.Black),
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 30.dp)
                .width(200.dp)// Add spacing between buttons

            ) {
            Text("Login", fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Or sign in with")
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            Image(painter = painterResource(R.drawable.google), contentDescription = null,
                modifier = Modifier.size(40.dp)
                    .clickable {
                        Toast.makeText(context,"Google",Toast.LENGTH_SHORT).show()
                    })

            Image(painter = painterResource(R.drawable.facebook), contentDescription = null,
                modifier = Modifier.size(40.dp)
                    .clickable {
                        Toast.makeText(context,"Facebook",Toast.LENGTH_SHORT).show()
                    })

            Image(painter = painterResource(R.drawable.twitter), contentDescription = null,
                modifier = Modifier.size(40.dp)
                    .clickable {
                        Toast.makeText(context,"Twitter",Toast.LENGTH_SHORT).show()
                    })

        }
    }


}


fun valid(email: String, password: String) {
    if (email.isNullOrEmpty()){

    }
}


