package com.example.portfolioapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.portfolioapp.ui.theme.PortfolioAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PortfolioAppTheme {
                Portfolio()
            }
        }
    }
}

@Composable
fun Portfolio() {
    Surface(
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFBB86FC), // or any other color from MaterialTheme
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        // Content here


        Column(horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)) {


        Image(painter = painterResource(id = R.drawable.profile), contentDescription = "",
            modifier = Modifier.size(60.dp))
        Spacer(modifier = Modifier.height(height = 12.dp))
        Divider()
            Spacer(modifier = Modifier.height(height = 8.dp))
        Text(text = "Dinkar Bhardwaj", style = TextStyle(color = Color.White,
            fontSize = 20.sp, fontWeight = FontWeight.Bold
        ))
            Spacer(modifier = Modifier.height(height = 4.dp))

            Text(text = "Android Developer", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(height = 8.dp))

            Row {
                Image(painter = painterResource(id = R.drawable.instagram), contentDescription = "",
                    modifier = Modifier.size(20.dp))
                Text(text = "/thedinkarbhardwaj", style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp))
            }
            Spacer(modifier = Modifier.height(height = 4.dp))

            Row {
                Image(painter = painterResource(id = R.drawable.github), contentDescription = "",
                    modifier = Modifier.size(20.dp))
                Text(text = "/thedinkarbhardwaj", style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp))
            }
            Spacer(modifier = Modifier.height(height = 12.dp))
            Button(onClick = { /*TODO*/ }) {
                Text(text = "My Projects")
            }

            LazyColumn{
                items(getProjectList()){

                    ProjectItem(it)
                }
            }
        }
    }
}

@Composable
fun ProjectItem(project: Project) {

    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Image(painter = painterResource(id = R.drawable.profile), contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape))

        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(text = project.name, style = MaterialTheme.typography.bodyMedium)
            Text(text = project.desc, style = MaterialTheme.typography.bodySmall)
        }
    }
    
}

fun getProjectList():List<Project>{

    return listOf(
        Project("Social Media App","Connect with your friends"),
        Project("Taxi Booking","Book Taxi within 10 seconds")
    )
}

data class Project(var name: String,val desc:String)


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PortfolioAppTheme {
        Greeting("Android")
    }
}