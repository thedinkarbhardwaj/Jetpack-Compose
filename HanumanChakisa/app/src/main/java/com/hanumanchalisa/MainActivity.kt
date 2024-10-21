package com.hanumanchalisa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.hanumanchalisa.ui.Screens.HanumanChalisa
import com.hanumanchalisa.ui.theme.HanumanChakisaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HanumanChakisaTheme {

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    HanumanChalisa(innerPadding)
                }
            }
        }
    }
}
