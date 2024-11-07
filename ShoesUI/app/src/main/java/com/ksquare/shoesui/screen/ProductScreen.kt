package com.ksquare.shoesui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ksquare.shoesui.Model.ProductData
import com.ksquare.shoesui.R

@Composable
fun ProductScreen(modifier: Modifier = Modifier) {

    var product = remember {
        getShoesList()
    }

    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.padding(8.dp)) {

        items(product) {

            productItem(product = it)
        }

    }
}

@Preview
@Composable
fun productItem(
    product: ProductData = ProductData(
        id = "1",
        name = "Air Max",
        color = 0xFFFF5733.toInt(), // Example color
        price = "$120",
        discount = 10.0f,
        size = 42,
        rating = 4.5f,
        image = R.drawable.s1 // Replace with actual drawable resource
    )
) {

    var colorrr by remember {
        mutableStateOf(product.color)
    }

    var isFav by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier
            .padding(20.dp)
            .size(168.dp, 210.dp)
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.2f)
                .background(
                    color = Color(colorrr),
                    shape = RoundedCornerShape(8.dp)
                ) // Use any shape you want here
        )

        IconButton(
            onClick = {
                isFav = !isFav
            },
            modifier = Modifier.align(Alignment.TopStart)
        ) {
           Icon(imageVector = if (isFav){
               Icons.Rounded.Favorite
           }else{
               Icons.Rounded.FavoriteBorder
           }, contentDescription = null)
        }

        Text(text = product.size.toString(), fontSize = 80.sp,
            fontWeight = FontWeight.Bold,
            color = Color(colorrr).copy(alpha = .3f),
            modifier = Modifier.align(Alignment.Center))

        Image(painter = painterResource(id = product.image), contentDescription = null,
            modifier = Modifier.fillMaxSize(1f)
                .align(Alignment.Center)
                .rotate(-30f)
                .offset(30.dp,-10.dp))

        Column(
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {

            Text(text = "Rs. ${product.discount}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(end = 8.dp))

            Text(text = "Rs. ${product.price}",
                fontSize = 12.sp,
                modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                    .align(Alignment.End),
                style = TextStyle(
                    textDecoration = TextDecoration.LineThrough
                )
            )
        }

    }
}

fun getShoesList(): List<ProductData> {
    return listOf(
        ProductData(
            id = "1",
            name = "Air Max",
            color = 0xFFFF5733.toInt(),
            price = "1200",
            discount = 1000.0f,
            size = 42,
            rating = 4.5f,
            image = R.drawable.s5
        ),
        ProductData(
            id = "2",
            name = "Jordan High",
            color = 0xFFC70039.toInt(),
            price = "1500",
            discount = 1200f,
            size = 44,
            rating = 4.7f,
            image = R.drawable.s6
        ),
        ProductData(
            id = "3",
            name = "Yeezy Boost",
            color = 0xFF900C3F.toInt(),
            price = "2000", // Updated price
            discount = 1500.0f,
            size = 43,
            rating = 4.8f,
            image = R.drawable.s6
        ),
        ProductData(
            id = "4",
            name = "Puma RS-X",
            color = 0xFFFFC300.toInt(),
            price = "900", // Updated price
            discount = 700.0f,
            size = 41,
            rating = 4.3f,
            image = R.drawable.s5
        ),
        ProductData(
            id = "5",
            name = "Adidas Superstar",
            color = 0xFFDAF7A6.toInt(),
            price = "1000", // Updated price
            discount = 800.0f,
            size = 40,
            rating = 4.6f,
            image = R.drawable.s5
        ),
        ProductData(
            id = "6",
            name = "Nike Revolution",
            color = 0xFF4A90E2.toInt(),
            price = "800", // Updated price
            discount = 600.0f,
            size = 42,
            rating = 4.2f,
            image = R.drawable.s6
        )
    )

}
