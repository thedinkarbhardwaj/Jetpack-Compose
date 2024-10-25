package com.shayriapp.ui.Screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.shayriapp.MainActivity
import com.shayriapp.Model.ShayriModel
import com.shayriapp.ui.routing.ShayriRoutingNames
import com.shayriapp.ui.theme.orangeColor
import com.shayriapp.ui.theme.primaryColor

var mainact:MainActivity?=null
lateinit var navHostControler: NavHostController
@Composable
fun CategoryScreen(navHostController: NavHostController,context: MainActivity?=null,modifier: Modifier = Modifier) {

    navHostControler = navHostController
    mainact = context
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(color = primaryColor),
        topBar = {
            TopBar(modifier = Modifier)
        },
        content = { contentPadding ->
            // Pass contentPadding to the main content to avoid being overlapped by system bars
            MainContent(modifier = Modifier.padding(contentPadding))
        }
    )
}

@Composable
fun MainContent(modifier: Modifier) {
    // Apply the modifier to ensure the content respects padding
    Column(

        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
       LazyColumn() {
           items(getList()){

               Card(shape = RoundedCornerShape(20.dp),
                   colors = CardDefaults.cardColors(containerColor = orangeColor),
                   border = BorderStroke(width = 1.dp, color = Color.Black),
                   modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                       .clickable {
                         //  Toast.makeText(mainact,it.title,Toast.LENGTH_SHORT).show()
                           navHostControler.navigate(ShayriRoutingNames.shayariScreen.route + "/${it.title}")
                       }
               ) {
                   Text("${it.title}", style = TextStyle(
                       color = Color.White,

                       fontSize = 16.sp,
                       fontWeight = FontWeight.Medium,
                       textAlign = TextAlign.Center
                   ),
                       modifier= Modifier.fillMaxWidth().padding(16.dp)
                   )

               }

           }
       }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(modifier: Modifier) {
    MainToolBar(modifier = modifier, title = "Category", onclick = {})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainToolBar(modifier: Modifier, title: String, onclick: () -> Unit) {
    TopAppBar(
        modifier = Modifier.padding(top = 1.dp),
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
                style = TextStyle(fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(start = 10.dp) // Correct modifier usage
            )
        },
        navigationIcon = {
            Card(
                modifier = Modifier
                    .size(40.dp)
                    .padding(start = 2.dp)
                    .clickable { onclick.invoke() },
                colors = CardDefaults.cardColors(containerColor = orangeColor),
                shape = RoundedCornerShape(200.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center, // Centers the icon in the Card
                    modifier = Modifier.fillMaxSize() // Ensures the Box fills the Card's size
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp) // Adjust icon size as needed
                    )
                }
            }
        }
    )
}


fun getList(): List<ShayriModel> {
    return listOf(
        ShayriModel(
            title = "Love Shayari",
            shayriList = listOf(
                "Tumhe paane ki koshish mein hum khud ko bhool gaye.",
                "Kuch is tarah se tune dekha hai mujhe, ek pal mein dil ko jeet gaya tu.",
                "Meri zindagi ka sabse haseen lamha woh tha, jab tum paas aaye aur hum dono khamosh the.",
                "Mohabbat ka jazba bhi ajeeb hota hai, jo tumhare saath ho toh har lamha naseeb hota hai.",
                "Tumse mil kar laga mujhe zindagi ka har khwab sach hai.",
                "Tumhe yaad kar ke bas khud ko sambhalte hai hum.",
                "Tere bina zindagi mein khushiyan adhoori lagti hai.",
                "Tumse pyaar itna hai ki tujhe khone ka darr rehta hai.",
                "Aaj bhi dil kehta hai bas tumse mohabbat karte chalein.",
                "Dil se aata hai sirf ek hi khayal, tum ho toh zindagi ka har pal kamaal."
            )
        ),
        ShayriModel(
            title = "Friendship Shayari",
            shayriList = listOf(
                "Dosti woh nahi jo zindagi ke saath chale, dosti toh woh hai jo zindagi bhul ke bhi saath na chhode.",
                "Zindagi mein apni jo jagah bana le, dosti usi ko kehte hain.",
                "Dosti wahi jo saath nibhaye, dosti wo jo hamesha yaad aaye.",
                "Dosti ke bina zindagi ka safar adhoora hai.",
                "Dost woh hai jo mushkil waqt mein saath ho.",
                "Dosti ka rishta hai dil se, har pal mein apne sath hai.",
                "Kuch log zindagi mein bas dosti ke liye bane hote hain.",
                "Dosti ek ehsaas hai jo sirf humare paas hai.",
                "Zindagi ki raahon mein dost hamesha saath chalte hain.",
                "Dosti ek khoobsurat rishta hai, jo dil se judta hai."
            )
        ),
        ShayriModel(
            title = "Sad Shayari",
            shayriList = listOf(
                "Dil todne wale ko kabhi khushi nahi milti.",
                "Kabhi kabhi hum chup rehte hai, par dil rota hai.",
                "Aansu chhupaye nahi chhupe, bas dikhaye nahi jaate.",
                "Dil ke armaan aansuon mein beh gaye.",
                "Jisne dil toda uska bhi dil kabhi zaroor toota hoga.",
                "Dil se pyar kiya aur dil hi tor diya.",
                "Akele rehna sikh gaye hain hum, ab kisi ki zaroorat nahi.",
                "Zindagi mein bas gham hi gham hai.",
                "Hum woh hai jo dard mein bhi muskurate hai.",
                "Jab dil toot jaye toh awaz nahi hoti."
            )
        ),
        ShayriModel(
            title = "Inspirational Shayari",
            shayriList = listOf(
                "Sapne woh nahi jo sote hue dekhe jaate hain, sapne woh hain jo aapko sone nahi dete.",
                "Koshish karne walon ki kabhi haar nahi hoti.",
                "Jeevan mein safalta tabhi milti hai jab aap khud par vishwas rakhte hain.",
                "Zindagi mein kabhi haar mat maan na, kyunki badi jeet abhi baaki hai.",
                "Kamyabi ka raasta mushkil hai, par namumkin nahi.",
                "Jeevan mein kabhi rukna mat, chalte rehna hi jeet hai.",
                "Safalta unhi ko milti hai jo khud par bharosa karte hain.",
                "Sapne sach hote hain agar unhe poora karne ki lagan ho.",
                "Mushkilein aati hain par jeetne ka jazba banaye rakho.",
                "Sooraj ke jaise chamakna hai toh raat ki tarah andheron ka saamna karna padega."
            )
        ),
        ShayriModel(
            title = "Birthday Shayari",
            shayriList = listOf(
                "Aapka har din khushiyon se bhara ho, happy birthday!",
                "Aapke saath yeh din bar-bar aaye, aur zindagi mein khushiyan laaye.",
                "Janmadin mubarak ho! Khush rahein aap sadaa.",
                "Aapke is khaas din pe hamesha aapke saath hai hum.",
                "Har khushi mile aapko, yehi dua hai humari.",
                "Happy birthday! Aap hamesha haste muskurate rahein.",
                "Zindagi mein aapko woh sab mile jo aapne socha hai.",
                "Janmadin ki shubhkamnayein! Aapka safar khoobsurat rahe.",
                "Aapka ye janmadin khoobsurat aur yaadon se bhara ho.",
                "Aapko janmadin ki dher saari badhayi aur pyar."
            )
        )
    )
}


