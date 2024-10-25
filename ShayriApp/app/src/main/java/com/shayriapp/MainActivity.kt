package com.shayriapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.shayriapp.ui.Screens.CategoryScreen
import com.shayriapp.ui.routing.ShayriRouting
import com.shayriapp.ui.theme.ShayriAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContent {
            ShayriAppTheme {
                val navHostController = rememberNavController()
                ShayriRouting(navHostController = navHostController)
            }
        }
    }
}



