package com.sunpawtechnologies.supawui.screens

import android.widget.Space
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.sunpawtechnologies.supawui.R
import com.sunpawtechnologies.supawui.naviagtion.NavigationName
import com.sunpawtechnologies.supawui.ui.theme.Primary



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavHostController) {

    var mobileNum by rememberSaveable { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue -> newValue != SheetValue.Hidden } // 👈 blocks swipe-down hide
    )

    Column(
        modifier = Modifier.fillMaxSize().background(Primary),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painter = painterResource(R.drawable.logo), "", modifier = Modifier.size(160.dp))
        Text("Inventory", style = TextStyle(color = Color.White),
            modifier = Modifier.offset(y = (-50).dp))

        ModalBottomSheet(
            onDismissRequest = { },                       // tapping the scrim does nothing
            sheetState = sheetState,
            dragHandle = null,                            // hide the drag handle (optional)
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = false          // back button won't close it
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {

                // Close icon removed, since the sheet can't be closed anymore
                Text("Login")

                Spacer(modifier = Modifier.padding(top = 10.dp))
                Text("Enter your mobile number to proceed",
                    style = TextStyle(color = Color.Gray))
                Spacer(modifier = Modifier.padding(top = 20.dp))

                OutlinedTextField(
                    value = mobileNum,
                    onValueChange = {
                        if (it.length <= 10) mobileNum = it   // 👈 fixed: check the NEW value
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    textStyle = TextStyle(color = Color.Black),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("Enter your mobile no") }
                )

                Spacer(modifier = Modifier.padding(10.dp))
                ElevatedButton(
                    onClick = {

                        navController.navigate(NavigationName.otp)
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Continue", style = TextStyle(letterSpacing = 2.sp))
                }
            }
        }
    }
}


