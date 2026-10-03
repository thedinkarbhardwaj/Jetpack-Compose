package com.sunpawtechnologies.supawui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
fun AddProductScreen(navController: NavHostController, productName: String) {

    Text("Addporduct Screen $productName")
}