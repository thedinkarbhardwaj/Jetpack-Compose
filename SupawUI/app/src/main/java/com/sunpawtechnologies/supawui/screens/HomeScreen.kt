package com.sunpawtechnologies.supawui.screens


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.sunpawtechnologies.supawui.R
import com.sunpawtechnologies.supawui.model.OptionModel
import com.sunpawtechnologies.supawui.naviagtion.NavigationName

@Composable
fun HomeScreen(navController: NavHostController) {

    var imageUrl by remember { mutableStateOf("https://media.istockphoto.com/id/814423752/photo/eye-of-model-with-colorful-art-make-up-close-up.jpg") }

    Column(modifier = Modifier
        .fillMaxSize()
        .safeDrawingPadding()
        .padding(10.dp)) {

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

            Column() {
                Text("Hello", style = TextStyle(fontSize = 16.sp, color = Color.Gray,
                    ))
                Text("Rahul Sharma", style = TextStyle(
                    fontSize = 20.sp, color = Color.Black,
                    fontWeight = FontWeight.Bold
                ))
            }

            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {

                Icon(Icons.Default.Notifications,"",
                    modifier = Modifier.size(30.dp))
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .border(
                            2.dp,
                            Color.Gray,
                            CircleShape
                        ),
                    contentScale = ContentScale.Crop
                )
            }


        }

        Spacer(Modifier.height(20.dp))

        OptionList(navController)
        

    }


}

@Composable

fun OptionList(navController: NavHostController){

    var list = listOf<OptionModel>(
        OptionModel("Add product", R.drawable.logo),
        OptionModel("Inventory", R.drawable.logo),
        OptionModel("Add Store", R.drawable.logo),
        OptionModel("Settings", R.drawable.logo)
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

        items(list){item ->

            OptionUi(item, onClick = {
                // item click

                when(item.title){
                    "Add product"-> navController.navigate("${NavigationName.addProduct}/${item.title}")
                    else -> {  println(item.title)}
                }

            })
        }

    }

}

@Composable
fun OptionUi(item: OptionModel, onClick: () -> Unit) {

    Card(onClick = onClick, modifier = Modifier
        .size(100.dp)
        .background(color = Color.White),
        border = BorderStroke(width = 2.dp, color = Color.Black)
    ) {

        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painter = painterResource(item.image), "")

            Text(item.title, style = TextStyle(fontSize = 12.sp))
        }

    }
}