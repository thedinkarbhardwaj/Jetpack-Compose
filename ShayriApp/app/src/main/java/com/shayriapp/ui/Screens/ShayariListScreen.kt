package com.shayriapp.ui.Screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.shayriapp.Model.ShayriModel
import com.shayriapp.R
import com.shayriapp.ui.routing.ShayriRoutingNames
import com.shayriapp.ui.theme.orangeColor
import com.shayriapp.ui.theme.primaryColor
import kotlinx.coroutines.launch

@Composable
fun ShayariListScreen(navHostController: NavHostController, title: String?) {


    Surface() {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(Color.LightGray)
        ){

            Column {
                MainToolBar(modifier = Modifier, title = "$title", onclick = {

                    navHostController.popBackStack()
                })
                val myFilteredList = getList().filter {
                    it.title == title
                }

                val onlyShayriList = myFilteredList[0]

              ShayariList(onlyShayriList)
            }

        }
    }

}


@Composable
fun ShayariList(onlyShayriList: ShayriModel) {
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope() // CoroutineScope for Snackbar

    Box {
        LazyColumn {
            onlyShayriList.shayriList?.let { list ->
                items(list) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Column {
                            Text(
                                text = item,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontStyle = FontStyle.Italic
                            )

                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 20.dp, bottom = 10.dp)
                            ) {
                                // Share Icon
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share"
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Copy Icon with Clickable Action
                                Icon(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(item))
                                            // Launch Snackbar in a coroutine
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Shayari copied to clipboard!")
                                            }
                                        },
                                    painter = painterResource(R.drawable.copyclipboard),
                                    contentDescription = "Copy to Clipboard"
                                )
                            }
                        }
                    }
                }
            }
        }

        // SnackbarHost to show Snackbar messages
        SnackbarHost(hostState = snackbarHostState)
    }
}


