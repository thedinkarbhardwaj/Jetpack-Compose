package com.example.dollarcount

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dollarcount.ui.theme.DollarCountTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DollarCountTheme {
                // A surface container using the 'background' color from the theme
                MyApp()
            }
        }
    }

}

@Composable
fun MyApp() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        DollarCounter()
    }
}

@Composable
fun DollarCounter() {

    var counter = remember {
        mutableStateOf(1)
    }
    Column(
        modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "$${counter.value*100}", style = TextStyle(color = Color.Black),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.size(200.dp))

        CustomButton() {

            counter.value++
        }
    }
}

@Composable
fun CustomButton(onclick: () -> Unit) {

    Card(
        modifier = Modifier
            .size(120.dp)
            .clickable {

                onclick.invoke()
            }, shape = CircleShape, colors = CardDefaults.cardColors(
            Color.Blue
        )
    ) {

        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {

            Text(
                text = "TAP", style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF000000),
                    fontSize = 20.sp
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DollarCountTheme {
        DollarCounter()
    }
}