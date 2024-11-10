package com.todoappwithroomdb

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.todoappwithroomdb.RoomDb.Todo
import java.text.SimpleDateFormat
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TodoListPage(viewModel:TodoViewModel,modifier: PaddingValues) {

    val list by viewModel.todoLiveData.observeAsState()

    var itemData by remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                label = {
                    Text("Enter here")
                },
                value = itemData,
                onValueChange = {
                    itemData = it
                },
                modifier = Modifier.weight(1f) // This makes it fill the available space within the Row
            )

            Button(
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 10.dp,
                    pressedElevation = 6.dp,
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White
                ),
                border = BorderStroke(width = 1.dp, color = Color.Black),
                modifier = Modifier.padding(start = 10.dp),
                onClick = {

                    if (!itemData.isNullOrEmpty()){
                        viewModel.addTodo(itemData)
                        itemData = ""
                    }

                }) {
                Text("ADD")
            }
        }

        list?.let {
            LazyColumn {this
                itemsIndexed(it) { itemIndex, item ->
                    TodoItem(item, onDelete = {
                        Log.d("IDDDDDDDDDDDDDDDDDDDDDDD2","${item.id}")

                        viewModel.deleteTodo(item.id)
                    })
                }
            }

        }?: Text(
            modifier = Modifier.fillMaxWidth()
                .padding(top = 20.dp),
            text = "No items yet",
            fontSize = 30.sp,
            fontFamily = FontFamily.Cursive,
            textAlign = TextAlign.Center)

    }
}

@Composable
fun TodoItem(item: Todo, onDelete: ()-> Unit) {
    Card(shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
            .padding(10.dp))
    {

        Row(modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(text = SimpleDateFormat("HH:mm:aa, dd/mm", Locale.ENGLISH).format(item.createdAt),
                    fontSize = 12.sp, color = Color.Blue
                )
                Text(text = item.title,
                    fontSize = 18.sp,
                    color = Color.Black)
            }


            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "",
                    tint = Color.Red
                )
            }


        }


    }
}