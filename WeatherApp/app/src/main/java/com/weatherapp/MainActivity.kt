package com.weatherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.weatherapp.screens.WeatherPage
import com.weatherapp.ui.theme.WeatherAppTheme
import com.weatherapp.viewModel.WeatherViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // enableEdgeToEdge()

        var weatherViewModel = ViewModelProvider(this).get(WeatherViewModel::class.java)
        setContent {
            WeatherAppTheme {

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->


                    WeatherPage(innerPadding,weatherViewModel)
                }
            }
        }
    }
}

