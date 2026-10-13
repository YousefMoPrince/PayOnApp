package com.jo.payonapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jo.payonapp.ui.theme.PayOnAppTheme
import kotlinx.coroutines.delay

class Splash : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PayOnAppTheme {

                SplashScreen(onTimeout = {
                    val intent = Intent(this, SignInActivity::class.java)
                    startActivity(intent)
                    finish()
                })

            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(3000)
        onTimeout()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF447AE3)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logowithtext),
            contentDescription = "Logo with text",
            modifier = Modifier.size(width = 219.dp, height = 180.dp)
        )
    }
}

@Preview(showBackground = true,showSystemUi = true, device = Devices.PIXEL_6A)
@Composable
fun GreetingPreview2() {
    PayOnAppTheme {
        SplashScreen(onTimeout = {})
    }
}