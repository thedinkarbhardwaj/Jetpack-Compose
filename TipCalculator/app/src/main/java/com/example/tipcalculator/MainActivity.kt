package com.example.tipcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tipcalculator.ui.theme.TipCalculatorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TipCalculatorTheme {
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

        Column {
            TipCalculator()
            //UserInput()
        }

    }
}

@Preview(showBackground = true)
@Composable
fun TipCalculator(amount:Float = 0f) {

    Column(modifier = Modifier.fillMaxWidth()) {

        Surface(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
            color = colorResource(id = R.color.blue),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 8.dp
        ) {
            
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center, modifier = Modifier.padding(20.dp)) {
                Text(text = "Total per person", style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                ))

                Text(text = "Rs $amount", style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                ),
                    modifier = Modifier.padding(top = 12.dp))
            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun UserInput(amount:String,amountChange:(String)->Unit) {

    var keyboardController = LocalSoftwareKeyboardController.current

    Surface(modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
        shadowElevation = 20.dp,
        shape = CircleShape,
        color = Color.White) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {

            OutlinedTextField(value = amount, onValueChange = {
                amountChange.invoke(amount)
            },
                modifier = Modifier.fillMaxWidth(), placeholder = { "Enter your amount" },
                keyboardOptions = KeyboardOptions(autoCorrect = true, keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {

                    keyboardController?.hide()
                })
            )
            
            Spacer(modifier = Modifier.padding(8.dp))

            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically) {

                Text(text = "Split", style = TextStyle(
                    color = Color.Black
                ))

            }

        }
    }
}