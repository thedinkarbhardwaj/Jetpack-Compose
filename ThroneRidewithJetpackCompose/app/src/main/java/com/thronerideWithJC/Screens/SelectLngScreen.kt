@file:OptIn(ExperimentalMaterial3Api::class)

package com.thronerideWithJC.Screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.thronerideWithJC.R
import androidx.compose.ui.platform.LocalContext

@Composable
fun SelectLngScreen(navController: NavHostController) {

    var langList = listOf(
        "English", "Spanish")

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text("Skip", style = TextStyle(
            color = Color.Blue,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .clickable {
                    navController.navigate("main_screen") {
                        popUpTo("select_lanuange_screen") {
                            inclusive = true
                        }
                    }
                })

                Column( horizontalAlignment = Alignment.CenterHorizontally, // Align children horizontally in the center
                    modifier = Modifier.padding(10.dp)) {
                    Text(
                        "Select Language",
                        style = TextStyle(
                            color = Color.Black,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center
                        )
                    )

                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Choose your preferred language for the app", style = TextStyle(
                            color = Color.Gray,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    )

                    LazyColumn(
                        contentPadding = PaddingValues(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                       items(langList){langName ->

                           LanguangeRec(langName)
                       }
                    }


                }

    }
}

@Composable
fun LanguangeRec(langName: String) {
    val context = LocalContext.current // Get the context for Toast
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row() {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(50.dp) // Set a reasonable size for the image
                    .border(2.dp, Color.Red, CircleShape) // Border with circle shape
                    .clip(CircleShape)
            )

            Text(
                "$langName", fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                textAlign = TextAlign.Justify,
                modifier = Modifier.padding(
                    horizontal = 20.dp,
                    vertical = 0.dp
                )
            )

        }

        RadioButton(
            selected = (true),
            onClick = {
                Toast.makeText(context, "You selected $langName", Toast.LENGTH_SHORT).show()            }
        )

    }
}