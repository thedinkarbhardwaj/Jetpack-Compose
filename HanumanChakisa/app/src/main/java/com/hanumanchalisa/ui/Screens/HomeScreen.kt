package com.hanumanchalisa.ui.Screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hanumanchakisa.R
import com.hanumanchalisa.ui.theme.AppColors

@Composable
fun HanumanChalisa(innerPadding: PaddingValues) {

    val hanumanChalisa2 = listOf<String>("1","2"
        )


    val hanumanChalisa = listOf(
        "जय हनुमान ज्ञान गुन सागर। जय कपीस तिहुं लोक उजागर।।\nराम दूत अतुलित बल धामा। अंजनि-पुत्र पवनसुत नामा।।",
        "महाबीर बिक्रम बजरंगी। कुमति निवार सुमति के संगी।।\nकंचन बरन बिराज सुबेसा। कानन कुण्डल कुँचित केसा।।",
        "हाथ बज्र औ ध्वजा बिराजे। कांधे मूंज जनेउ साजे।।\nशंकर सुवन केसरी नंदन। तेज प्रताप महा जग वंदन।।",
        "बिद्यावान गुनी अति चातुर। राम काज करिबे को आतुर।।\nप्रभु चरित्र सुनिबे को रसिया। राम लखन सीता मन बसिया।।",
        "सूक्ष्म रूप धरि सियहिं दिखावा। बिकट रूप धरि लंक जरावा।।\nभीम रूप धरि असुर संहारे। रामचन्द्र के काज संवारे।।",
        "लाय सजीवन लखन जियाये। श्री रघुबीर हरषि उर लाये।।\nरघुपति कीन्ही बहुत बड़ाई। तुम मम प्रिय भरतहि सम भाई।।",
        "सहस बदन तुम्हरो जस गावैं। अस कहि श्रीपति कण्ठ लगावैं।।\nसनकादिक ब्रह्मादि मुनीसा। नारद सारद सहित अहीसा।।",
        "जम कुबेर दिगपाल जहां ते। कबि कोबिद कहि सके कहां ते।।\nतुम उपकार सुग्रीवहिं कीन्हा। राम मिलाय राज पद दीन्हा।।",
        "तुम्हरो मंत्र बिभीषन माना। लंकेश्वर भए सब जग जाना।।\nजुग सहस्र जोजन पर भानु। लील्यो ताहि मधुर फल जानू।।",
        "प्रभु मुद्रिका मेलि मुख माहीं। जलधि लांघि गये अचरज नाहीं।।\nदुर्गम काज जगत के जेते। सुगम अनुग्रह तुम्हरे तेते।।",
        "राम दुआरे तुम रखवारे। होत न आज्ञा बिनु पैसारे।।\nसब सुख लहै तुम्हारी सरना। तुम रच्छक काहू को डर ना।।",
        "आपन तेज सम्हारो आपै। तीनों लोक हांक तें कांपै।।\nभूत पिसाच निकट नहिं आवै। महाबीर जब नाम सुनावै।।",
        "नासै रोग हरे सब पीरा। जपत निरन्तर हनुमत बीरा।।\nसंकट तें हनुमान छुड़ावै। मन क्रम बचन ध्यान जो लावै।।",
        "सब पर राम तपस्वी राजा। तिन के काज सकल तुम साजा।।\nऔर मनोरथ जो कोई लावै। सोई अमित जीवन फल पावै।।",
        "चारों जुग परताप तुम्हारा। है परसिद्ध जगत उजियारा।।\nसाधु संत के तुम रखवारे। असुर निकन्दन राम दुलारे।।",
        "अष्टसिद्धि नौ निधि के दाता। अस बर दीन जानकी माता।।\nराम रसायन तुम्हरे पासा। सदा रहो रघुपति के दासा।।",
        "तुह्मरे भजन राम को पावै। जनम जनम के दुख बिसरावै।।\nअंत काल रघुबर पुर जाई। जहां जन्म हरिभक्त कहाई।।",
        "और देवता चित्त न धरई। हनुमत सेइ सर्ब सुख करई।।\nसङ्कट कटै मिटै सब पीरा। जो सुमिरै हनुमत बलबीरा।।",
        "जय जय जय हनुमान गोसाईं। कृपा करहु गुरुदेव की नाईं।।\nजो सत बार पाठ कर कोई। छूटहि बन्दि महा सुख होई।।",
        "जो यह पढ़ै हनुमान चालीसा। होय सिद्धि साखी गौरीसा।।\nतुलसीदास सदा हरि चेरा। कीजै नाथ हृदय महं डेरा।।"
    )

    var imgList = listOf(R.drawable.hanumang,R.drawable.hanuman2,R.drawable.hanuman3)



    Column(
        modifier = Modifier.fillMaxSize().padding(bottom = 20.dp)

    ) {

        var count = remember {
            mutableIntStateOf(0)
        }

        var imgcount by remember {
            mutableStateOf(0)
        }


        Image(painter = painterResource(imgList.get(imgcount)), contentDescription = "",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().height(250.dp))
        Spacer(modifier = Modifier.weight(0.3f))
        Surface (
            modifier = Modifier.fillMaxWidth()
                .padding(20.dp)
                .border(width = 1.dp, color = AppColors.Orange,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ){
            Text(hanumanChalisa.get(count.value), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                style = TextStyle(
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                    lineHeight = 30.sp

                    )
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    if (count.value > 0) {
                        count.value--
                    }
Log.d("IMgLIST","${imgList.size -1}  ImgCount $imgcount")
                    if (imgList.size -1 > imgcount){
                        imgcount++

                    }
                    else{
                        imgcount--
                    }

                },
                shape = RoundedCornerShape(30.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 10.dp,
                    pressedElevation = 6.dp,
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Orange,
                    contentColor = Color.White
                ),
                border = BorderStroke(width = 1.dp, color = Color.Black),
                modifier = Modifier
                    .weight(1f)  // Distribute width evenly
                    .padding(8.dp)  // Add spacing between buttons
            ) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "")
                Text("Prev", modifier = Modifier.padding(start = 8.dp),
                    style = TextStyle(color = Color.White,
                        fontSize = 16.sp)
                )
            }

            Button(
                onClick = {
                    if (hanumanChalisa.size - 1 > count.value) {
                        count.value++

                        Log.d("IMgLIST2","${imgList.size -1}  ImgCount $imgcount")

                        if (imgList.size -1 > imgcount){
                            imgcount++

                        }
                        else{
                            imgcount--
                        }
                    }
                },
                shape = RoundedCornerShape(30.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 10.dp,
                    pressedElevation = 6.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Orange,
                    contentColor = Color.White
                ),
                border = BorderStroke(width = 1.dp, color = Color.Black),
                modifier = Modifier
                    .weight(1f)  // Distribute width evenly
                    .padding(8.dp)  // Add spacing between buttons
            ) {
                Text("Next", modifier = Modifier.padding(start = 8.dp),
                    style = TextStyle(color = Color.White,
                        fontSize = 16.sp)
                )
                Icon(imageVector = Icons.Filled.ArrowForward, contentDescription = "",
                    modifier = Modifier.padding(start = 8.dp))
            }
        }
    }

}