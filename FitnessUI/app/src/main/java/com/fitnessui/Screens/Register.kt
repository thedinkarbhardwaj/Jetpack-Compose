package com.fitnessui.Screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.fitnessui.Constants.Constanst
import com.fitnessui.Navigation.Route
import com.fitnessui.R
import com.fitnessui.ui.theme.AppColor
import com.fitnessui.ui.theme.AppFont
import com.tapadoo.alerter.Alerter
import com.tapadoo.alerter.iconPulse


class RegisterState {
    companion object {
        var fullName by mutableStateOf("")
        var phoneNO by mutableStateOf("")
        var email by mutableStateOf("")
        var password by mutableStateOf("")
        var alertMsg by mutableStateOf("")
        var isChecked by mutableStateOf(false)

        var context:Context? = null
    }
}


@Composable
fun Register(navController: NavHostController,modifier: Modifier = Modifier) {

    var showError by remember { mutableStateOf(false) }

    val context = LocalContext.current

    RegisterState.context = context
    Column(modifier = modifier.fillMaxSize()
        .verticalScroll(state = rememberScrollState())
        .padding(vertical = 20.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,) {

        Text("Hey there,", textAlign = TextAlign.Center, color = AppColor.black,
            fontSize = 16.sp, fontFamily = AppFont.poppinsRegular)
Spacer(modifier = Modifier.height(6.dp))
        Text(text = "Create an Account", fontSize = 20.sp, fontFamily = AppFont.poppinsBold, color = AppColor.black)

        Spacer(modifier = Modifier.height(20.dp))
        CustomTextField("Full Name", Icons.Default.Person,KeyboardType.Text,
            ImeAction.Next,
            RegisterState.fullName) { newValue ->
            RegisterState.fullName = newValue
        }

        // Phone no
        CustomTextField(
            "Phone Number",
            Icons.Default.Phone,
            KeyboardType.Phone,
            ImeAction.Next,
            RegisterState.phoneNO
        ) { newValue ->
            RegisterState.phoneNO = newValue
        }

        // Emaill
        CustomTextField(
            "Email",
            Icons.Default.Email,
            KeyboardType.Email,
            ImeAction.Next,
            RegisterState.email
        ) { newValue ->
            RegisterState.email = newValue
        }

        // Password field
        CustomTextField(
            "Password",
            Icons.Default.Lock,
            KeyboardType.Password,
            ImeAction.Next,
            RegisterState.password
        ) { newValue ->
            RegisterState.password = newValue
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.padding(vertical = 1.dp, horizontal = 14.dp),
            ) {


            Checkbox( checked = RegisterState.isChecked,
                onCheckedChange = { checked ->
                    // Update the state when the Checkbox is checked/unchecked
                    RegisterState.isChecked = checked
                },
                colors =  CheckboxDefaults.colors(
                    checkedColor = AppColor.Purple , // This changes the color of the checkmark and box when checked
                    uncheckedColor = AppColor.Gray
                )
            )

            ClickableTextExample()


        }

        Spacer(modifier = Modifier.height(40.dp))

        Constanst.CustomButton("Register") {

           // showError = false
            if (validations()){
                navController.navigate(Route.RegCompleteProf) {
                    popUpTo(navController.graph.startDestinationId) {
                        inclusive = true
                    }
                }

            }
            else{
                Toast.makeText(context,"Reg Button",Toast.LENGTH_SHORT).show()

                showError = true
            }

        }

        if (showError) {
            AlerterMsg(RegisterState.alertMsg,"") // Your composable to show error message
           showError = false
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth()
            .padding(vertical = 1.dp, horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Divider(
                modifier = Modifier.weight(1f)
                    .padding(end = 8.dp),
                color = Color.Gray, // Set the color of the line
                thickness = 1.dp // Set the thickness of the line
            )

            Text("Or")
            Divider(
                modifier = Modifier.weight(1f)
                    .padding(start = 8.dp),
                color = Color.Gray, // Set the color of the line
                thickness = 1.dp // Set the thickness of the line
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {

          CardOfSocial(R.drawable.google)
            Spacer(modifier = Modifier.width(20.dp))
          CardOfSocial(R.drawable.facebook)

        }

        Spacer(modifier = Modifier.height(10.dp))

        // Already have an account

        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center) {

            Text(text = "Already have an account?", color = AppColor.black, fontFamily = AppFont.poppinsRegular,
                fontSize = 14.sp)

            Text(text = "Login", color = AppColor.Purple, fontFamily = AppFont.poppinsRegular,
                fontSize = 14.sp,
                modifier = modifier.clickable {

                })

        }

    }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(
    hint: String, icon: ImageVector, keyboardType: KeyboardType,
    imeAction: ImeAction,
    inputText: String,
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

@SuppressLint("SuspiciousIndentation")
@Composable
fun CardOfSocial(image: Int) {
    // Replace with your image resource
    val image: Painter = painterResource(id = image) // Ensure you have a Google logo drawable

        Card(
            shape = RoundedCornerShape(16.dp), // Adjust corner radius as needed
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier.size(40.dp), // Sets the size of the card
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {

            Row(modifier = Modifier.fillMaxSize( ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center){
                Image(
                    painter = image,
                    contentDescription = "Google Logo",
                    modifier = Modifier
                        .size(24.dp) // Adjust size of the image as needed
                        .clip(RoundedCornerShape(12.dp)) // Optional: Add clipping if necessary
                )
            }


        }


}


@Composable
fun ClickableTextExample() {

    val context = LocalContext.current

    val annotatedText = buildAnnotatedString {
        // Regular text
        withStyle(
            style = SpanStyle(
                color = Color.LightGray,
                fontSize = 12.sp
            )
        ) {
            append("By continuing you accept our ")
        }

        // Clickable Privacy Policy
        pushStringAnnotation(tag = "PRIVACY_POLICY", annotation = "https://google.com")
        withStyle(
            style = SpanStyle(
                color = Color.Blue,
                fontSize = 12.sp,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append("Privacy Policy")
        }
        pop() // End annotation for Privacy Policy

        // Regular text
        withStyle(
            style = SpanStyle(
                color = Color.LightGray,
                fontSize = 12.sp
            )
        ) {
            append(" and ")
        }

        // Clickable Terms of Use
        pushStringAnnotation(tag = "TERMS_TAG", annotation = "https://example.com/terms")
        withStyle(
            style = SpanStyle(
                color = Color.Blue,
                fontSize = 12.sp,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append("Term of Use")
        }
        pop() // End annotation for Terms of Use
    }

    // ClickableText to handle clicks
    ClickableText(
        text = annotatedText,
        onClick = { offset ->
            // Check which string annotation was clicked
            annotatedText.getStringAnnotations(start = offset, end = offset).firstOrNull()?.let { annotation ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(annotation.item)
                }
                context.startActivity(intent)
            }
        }
    )
}



fun validations():Boolean{

    var valid = false
    if (RegisterState.fullName.isBlank()){

     RegisterState.alertMsg = "Please enter your full name"

        return valid
    }
    else if (RegisterState.phoneNO.isBlank()){
        RegisterState.alertMsg = "Please enter your phone number"

        return valid
    }

    else if (RegisterState.phoneNO.length <10){
        RegisterState.alertMsg = "Please enter atleast 10 digits"

        return valid
    }
    else if (RegisterState.email.isBlank()){

        RegisterState.alertMsg = "Please enter your email"

        return valid
    }

    else if (!RegisterState.email.contains("@")){

        RegisterState.alertMsg = "Please enter your correct email"

        return valid
    }

    else if (!RegisterState.password.isBlank()){

        RegisterState.alertMsg = "Please enter password"

        return valid
    }

    else if (RegisterState.password.length <6){

        RegisterState.alertMsg = "Please enter atleast 6 password"

        return valid
    }

    return true
}

@Composable
fun AlerterMsg(title: String, desc: String) {

    Log.d("CheckkkAlertMsg","CheckkkAlertMsg")
    var showAlert by remember { mutableStateOf(false) }

     Alerter(isShown = true, onChanged = { showAlert = true },
         backgroundColor = AppColor.Red,
         enableVibration = true,
         disableOutsideTouch = true
     ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = "",
                    tint = Color.White,
                    modifier = Modifier.padding(start = 12.dp).iconPulse()
                )

                Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text(text = "$title", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                  //  Text(text = "$desc", color = Color.White, fontSize = 14.sp)
                }
            }
        }

}
