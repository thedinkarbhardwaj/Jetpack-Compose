package com.ksquare.simpletodoapp

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.ksquare.simpletodoapp.screens.TodoListPage
import com.ksquare.simpletodoapp.ui.theme.SimpleTodoAppTheme
import com.ksquare.simpletodoapp.viewModel.TodoViewModel

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var todoViewModel = ViewModelProvider(this).get(TodoViewModel::class.java)


       // enableEdgeToEdge()
        setContent {
            SimpleTodoAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TodoListPage(todoViewModel,innerPadding)
                }
            }
        }
    }
}
