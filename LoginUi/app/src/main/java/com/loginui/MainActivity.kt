package com.loginui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.loginui.ui.Screens.LoginUI
import com.loginui.ui.theme.LoginUiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       // enableEdgeToEdge()
        setContent {
            LoginUiTheme {
                LoginUI(this)

            }
        }
    }
}

