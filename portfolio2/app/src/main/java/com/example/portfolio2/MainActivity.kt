package com.example.portfolio2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.portfolio2.ui.theme.Portfolio2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Portfolio2Theme {
                Portfolio()

            }
        }
    }
}

@Composable
fun Portfolio() {

    Surface(
        elevation = 10.dp,
        color = MaterialTheme.colors.background,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {

       Column(modifier = Modifier
           .fillMaxWidth()
           .padding(20.dp),
       horizontalAlignment = Alignment.CenterHorizontally) {

           CircleImageView()
           Spacer(modifier = Modifier.height(20.dp))
           Divider(Modifier.height(2.dp), color = Color.Black)
           Spacer(modifier = Modifier.height(12.dp))
           Text(text = stringResource(R.string.name), fontSize = 22.sp,
           style = TextStyle(
               color = Color.Black,
               fontWeight = FontWeight.Medium,
               textAlign = TextAlign.Center
           ))

           Spacer(modifier = Modifier.height(4.dp))
           Text(text = stringResource(R.string.prof), fontSize = 16.sp,
               style = TextStyle(
                   color = Color.Blue,
                   fontWeight = FontWeight.Medium,
                   textAlign = TextAlign.Center
               ))
           Spacer(modifier = Modifier.height(20.dp))
           // Desc Text
           Text(
               text = stringResource(R.string.desc), fontSize = 14.sp,
               style = TextStyle(
                   color = Color.Black,
                   fontWeight = FontWeight.W400,
                   letterSpacing = 2.sp,
                   lineHeight = 18.sp
               ))
       }

    }
}

@Composable
fun CircleImageView() {
    Image(
        painter = painterResource(R.drawable.per),
        contentDescription = "Circle Image",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(100.dp) // Set a reasonable size for the image
            .border(5.dp, Color.Red, CircleShape) // Border with circle shape
            .clip(CircleShape) // Clip the image to a circular shape
    )
}

@Composable
fun ColumnLay() {
    
 Column() {
     for (i in 1..20){
         Text(text = "Welcome $i")
     }
 }   
}
