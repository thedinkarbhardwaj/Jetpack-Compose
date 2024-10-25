package com.shayriapp.ui.routing

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.shayriapp.ui.Screens.CategoryScreen
import com.shayriapp.ui.Screens.ShayariListScreen
import com.shayriapp.ui.Screens.SplashScreen

@Composable
fun ShayriRouting(navHostController: NavHostController,modifier: Modifier = Modifier) {

    NavHost(navController = navHostController, startDestination = ShayriRoutingNames.splashScreen.route){

        composable(ShayriRoutingNames.splashScreen.route) {
            SplashScreen(navHostController)
        }

        composable(ShayriRoutingNames.categoryScreen.route){
            CategoryScreen(navHostController)
        }

        composable(ShayriRoutingNames.shayariScreen.route + "/{title}"){

            val title = it.arguments?.getString("title")
            ShayariListScreen(navHostController,title)
        }

    }

}