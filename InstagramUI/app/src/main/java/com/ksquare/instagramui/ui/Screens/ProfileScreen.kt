package com.ksquare.instagramui.ui.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ksquare.instagramui.R

@Composable
fun ProfileScreen() {

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopBar(modifier = Modifier)
        },
        content = { contentPadding ->
            MainContent(modifier = Modifier.padding(contentPadding))
        }
    )

}

@Composable
fun MainContent(modifier: Modifier) {

    Column(modifier = modifier.fillMaxSize()) {

        var selectedTabIndex by remember {
            mutableIntStateOf(0)
        }

        ProfileSection(Modifier)
        Spacer(modifier = Modifier.padding(horizontal = 20.dp))
        PostTabView(
            onTabSelected = { index ->
                selectedTabIndex = index

            },
        )

        when(selectedTabIndex){
            0->PostSection()
        }
    }
}

@Composable
fun PostSection() {
    var posts = listOf(
        painterResource(R.drawable.car1),
        painterResource(R.drawable.car2)
    )

    LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier, content = {
       items(posts.size){
           Image(painter = posts[it],contentDescription = null, contentScale = ContentScale.Fit,
               modifier = Modifier.aspectRatio(1f)
                   .border(
                       width = 1.dp,
                       color = Color.White
                   ))
       }
    })
}

@Composable
fun PostTabView(
    modifier: Modifier = Modifier,
    onTabSelected:(slctIndex:Int) ->Unit
) {

    var selectedTabIndex by remember {
        mutableIntStateOf(0)
    }

    var tabIcon = listOf(
        TabRowIcon(R.drawable.posts),
        TabRowIcon(R.drawable.reel),
        TabRowIcon(R.drawable.tag),
    )

    TabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier
    ) {

        tabIcon.forEachIndexed{index, tabRowIcon ->

            Tab(
                selected = index == selectedTabIndex,
                onClick = {
                    selectedTabIndex = index
                    onTabSelected(index)
                },
                icon = {
                    Icon(
                        painter = painterResource(id = tabRowIcon.icon),
                        contentDescription = null, // or provide a meaningful description
                        modifier = Modifier.size(20.dp),
                        tint = if (selectedTabIndex == index) Color.Black else Color.Gray
                    )
                },
               
                selectedContentColor = Color.Black,
                unselectedContentColor = Color.Gray
            )



        }

    }

}

@Composable
fun ProfileSection(modifier: Modifier.Companion) {

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.padding(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {

            ImageBuilder(
                image = painterResource(
                    R.drawable.man
                ),
                modifier = modifier
                    .size(90.dp)
                    .weight(3f)
            )

            // FOllowStatus Bar
            FollowStatusBar(modifier = Modifier.weight(7f))

        }

        Spacer(modifier = Modifier.height(8.dp))
        Biosection(
            name = "Dinkar Bhardwaj || Android Developer",
            activityLabel = "Programming",
            desctription = "Want to make your app? Send me msg\non linkedin.",
            link = "www.youtube.com",
            followers = buildAnnotatedString {

                val boldStyle = SpanStyle(
                    fontWeight = FontWeight.Bold
                )
                append(text = "Followed by ")
                pushStyle(boldStyle)
                append("mhk.dev")
                pop()
                append(",")
                pushStyle(boldStyle)
                append("coding.dev")
                pop()
                append("and ")
                pushStyle(boldStyle)
                append("43k others")
            }
        )

        Spacer(modifier = Modifier.height(20.dp))
        ButtonSection(modifier = modifier)
        Spacer(modifier = Modifier.height(20.dp))
        HighlightSection(
            stories = listOf<StoryHighlights>(
                StoryHighlights(painterResource(R.drawable.linkedin),"LinkedIn"),
                StoryHighlights(painterResource(R.drawable.youtube),"Youtube"),
            ),
            modifier = modifier
        )


    }
}

@Composable
fun HighlightSection(stories: List<StoryHighlights>, modifier: Modifier) {
    LazyRow(modifier = modifier.padding(horizontal = 20.dp)) {
        items(stories.size){
            Column(
                modifier = modifier.padding(end = 15.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                ImageBuilder(image = stories[it].image, modifier = Modifier.size(50.dp))
                Spacer(modifier = modifier.padding(2.dp))
                Text(text = stories[it].title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ButtonSection(modifier: Modifier = Modifier) {
    var minwidth = 105.dp
    var height = 30.dp

    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp), // Adds space between buttons
        modifier = modifier.padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        SampleButton(modifier.defaultMinSize(minwidth).height(height), "Following", Icons.Default.KeyboardArrowDown)
        SampleButton(modifier.defaultMinSize(minwidth).height(height), "Message", null)
        SampleButton(modifier.defaultMinSize(minwidth).height(height), "Email", null)
    }
}

@Composable
fun SampleButton(modifier: Modifier,
                 text:String,
                 icon:ImageVector? = null
) {

    Row(horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(Color.LightGray)
            .clickable {

            }) {

        if (text != null){
            Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, overflow = TextOverflow.Ellipsis)
        }

        if (icon != null){
            Icon(imageVector = icon, contentDescription = text, tint = Color.Black)
        }
    }

}

@Composable
fun Biosection(
    name: String,
    activityLabel: String,
    desctription: String,
    link: String,
    followers: AnnotatedString
) {

    var letterSpacing = 0.5.sp
    var lineheight = 20.sp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {

        Text(
            text = name,
            fontWeight = FontWeight.Bold,
            letterSpacing = letterSpacing,
            lineHeight = lineheight
        )
        Text(
            text = activityLabel,
            fontWeight = FontWeight.Medium,
            letterSpacing = letterSpacing,
            lineHeight = lineheight,
            color = Color.Gray
        )
        Text(
            text = desctription,
            fontWeight = FontWeight.W500,
            letterSpacing = letterSpacing,
            lineHeight = lineheight,
            fontSize = 14.sp
        )
        Text(
            text = link,
            fontWeight = FontWeight.Medium,
            letterSpacing = letterSpacing,
            lineHeight = lineheight,
            color = Color(0xFF65A2E7)
        )
        Text(
            text = followers,
            fontWeight = FontWeight.Medium,
            letterSpacing = letterSpacing,
            lineHeight = lineheight,
            fontSize = 12.sp
        )

    }


}

@Composable
fun FollowStatusBar(modifier: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier
    ) {

        FollowSection(number = "3", label = "Posts", modifier = modifier)
        FollowSection(number = "48k", label = "Followers", modifier = modifier)
        FollowSection(number = "800", label = "Following", modifier = modifier)
    }
}

@Composable
fun FollowSection(number: String, label: String, modifier: Modifier) {

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {

        Text(text = number, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)

    }

}

@Composable
fun ImageBuilder(image: Painter, modifier: Modifier) {

    Image(
        painter = image,
        contentDescription = "",
        modifier = modifier
            .aspectRatio(1f, matchHeightConstraintsFirst = true)
            .border(width = 2.dp, color = Color.LightGray, shape = CircleShape)
            .clip(shape = CircleShape)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(modifier: Modifier) {

    TopAppBar(title = {
        Text(
            "dinkar.dev", modifier = Modifier.padding(start = 20.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    },
        navigationIcon = {
            Icon(
                imageVector = Icons.Default.ArrowBack, contentDescription = "back",
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(30.dp),
            )
        },

        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notification",
                    modifier = Modifier.size(30.dp),
                    tint = Color.Black
                )
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    modifier = Modifier.size(30.dp),
                    tint = Color.Black
                )
            }
        }

    )

}
