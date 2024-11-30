package com.fitnessui.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.fitnessui.Constants.Constanst
import com.fitnessui.R
import com.fitnessui.ui.theme.AppColor


object RegCompl{
    var dateOfBirth by mutableStateOf("")

}

@Composable
fun RegCompleteProf(navController: NavHostController,modifier: Modifier = Modifier) {

    Column(modifier = modifier.fillMaxSize()) {

        Image(painter = painterResource(R.drawable.reg_image), contentDescription = "",
            modifier = Modifier.fillMaxWidth()
                .height(300.dp))

        Constanst.HeadingText("Let’s complete your profile") { }

        Constanst.DescText("It will help us to know more about you!") { }

        GenderDropDown()

        CustomTextFields("Date of Birth",Icons.Default.DateRange, KeyboardType.Text,
            inputText = RegCompl.dateOfBirth,
            imeAction = ImeAction.Next) {
                newValue ->
            RegCompl.dateOfBirth = newValue
        }
    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderDropDown() {
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf("Select an option") }

    val options = listOf("Option 1", "Option 2", "Option 3")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        // Text field for the dropdown
        OutlinedTextField(
            value = selectedOptionText,
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color(0x54E0E0E0), // Background color
                focusedBorderColor = AppColor.Purple,
                unfocusedBorderColor = AppColor.lightPurple
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .padding(vertical = 10.dp, horizontal = 14.dp),
        )

        // Dropdown menu items
        ExposedDropdownMenu(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 14.dp),
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = {
                        Text(
                            selectionOption,
                            modifier = Modifier.padding(start = 16.dp) // Start padding for the text
                        )
                    },
                    onClick = {
                        selectedOptionText = selectionOption
                        expanded = false
                    }
                )
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextFields(
    hint: String, icon: ImageVector, keyboardType: KeyboardType,
    inputText: String,
    imeAction:ImeAction,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth()
            .padding(vertical = 10.dp, horizontal = 14.dp),
        value = inputText,
        onValueChange = onValueChange,
        placeholder = {
            Text(text = hint, color = AppColor.Gray)
        },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = "", tint = AppColor.Gray)
        },
        trailingIcon = {

        },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            containerColor = Color(0x54E0E0E0), // Background color
            focusedBorderColor = AppColor.Purple,
            unfocusedBorderColor = AppColor.lightPurple
        ),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = imeAction
        )

    )
}


