package com.sunpawtechnologies.supawui.screens

import android.R
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sunpawtechnologies.supawui.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(navController: NavHostController, productName: String) {

    var expanded by remember { mutableStateOf(false) }
    val items = listOf("Food", "Drinks", "Other")
    var selectedItem by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 10.dp) .background(color = Color.LightGray), verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = {}){
                Icon(Icons.Default.ArrowBack,"")

            }
            Text("$productName", style = TextStyle(color = Color.Black))
            Spacer(Modifier.width(1.dp))

        }

        HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 2.dp,
            color = Color.Gray)

        Column() {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

                Text("Select Store", style = TextStyle(
                    color = Color.Black,
                    fontSize = 14.sp
                ))

                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    IconButton(onClick = {}, modifier = Modifier.size(20.dp)) {
                        Icon(Icons.Default.Add,"", tint = Primary
                           )

                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Add New", style = TextStyle(color = Primary))
                }

            }

            LabelledtextField("Select Store")

            ExposedDropdownMenuBox(
                modifier = Modifier.fillMaxWidth(),
                expanded = expanded,
                onExpandedChange = {
                    expanded = !expanded
                }) {

                OutlinedTextField(value = selectedItem, onValueChange = {

                }, readOnly = true,
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    label = { Text("Category")})

                ExposedDropdownMenu(expanded = expanded, onDismissRequest = {
                    expanded = false
                }) {

                    items.forEach { item->

                        DropdownMenuItem(
                            text = {
                                Text(item)
                            },
                            onClick = {
                                selectedItem = item
                                expanded = false
                            }
                        )
                    }
                }

            }

        }
    }
}

@Composable
fun LabelledtextField(title: String) {

    Spacer(Modifier.height(6.dp))
    Text(title, style = TextStyle(
        color = Color.Gray,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    ))
    Spacer(Modifier.height(6.dp))
}