package com.sunpawtechnologies.supawui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.sunpawtechnologies.supawui.naviagtion.AppNavigation
import com.sunpawtechnologies.supawui.screens.SplashSceen
import com.sunpawtechnologies.supawui.ui.theme.SupawUITheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {


            val primaryColor by ThemeManager.primaryColor
                .collectAsState()

            SupawUITheme( primaryColor = primaryColor) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                    AppNavigation()

                }
            }
        }
    }
}

object ThemeManager {

    private val _primaryColor = MutableStateFlow(Color(0xFF652B88))

    val primaryColor: StateFlow<Color> = _primaryColor

    fun changeTheme(color: Color) {
        _primaryColor.value = color
    }
}
