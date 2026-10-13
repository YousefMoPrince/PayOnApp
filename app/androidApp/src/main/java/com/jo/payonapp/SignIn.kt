package com.jo.payonapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.jo.payonapp.intent.SignInIntent
import com.jo.payonapp.sideeffect.SignInSideEffect
import com.jo.payonapp.state.SignInState
import com.jo.payonapp.ui.theme.PayOnAppTheme
import com.jo.payonapp.viewmodel.SignInViewModel
import org.koin.android.ext.android.inject

class SignInActivity : ComponentActivity() {

    private val signInViewModel: SignInViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PayOnAppTheme {
                val state by signInViewModel.state.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    signInViewModel.sideEffect.collect { effect ->
                        when (effect) {
                            is SignInSideEffect.NavigateToMainActivity -> {
                                val intent = Intent(this@SignInActivity, MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            is SignInSideEffect.ShowToast -> {
                                Toast.makeText(this@SignInActivity, effect.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                SignInScreen(
                    state = state,
                    onIntent = { intent -> signInViewModel.processIntent(intent) },
                    onSignUpClick = {
                        val intent = Intent(this@SignInActivity, SignUpActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun SignInScreen(
    state: SignInState,
    onIntent: (SignInIntent) -> Unit,
    onSignUpClick: () -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }

    val outfitFontFamily = FontFamily(
        Font(R.font.outfit_semibold, FontWeight.SemiBold)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF447AE3)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.logowithtext),
            contentDescription = "Logo with text",
            modifier = Modifier
                .padding(top = 40.dp)
                .size(width = 157.dp, height = 146.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Sign-In",
                    fontFamily = outfitFontFamily,
                    fontSize = 23.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 25.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Username",
                        fontFamily = outfitFontFamily,
                        fontSize = 17.sp,
                        color = Color(0xFF787777)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = { onIntent(SignInIntent.UserNameChanged(it)) },
                        placeholder = { Text("enter username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF447AE3),
                            unfocusedBorderColor = Color.LightGray,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Password",
                        fontFamily = outfitFontFamily,
                        fontSize = 17.sp,
                        color = Color(0xFF787777)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { onIntent(SignInIntent.PasswordChanged(it)) },
                        placeholder = { Text("enter password") },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            val iconRes = if (isPasswordVisible) R.drawable.visibility else R.drawable.visibility_off
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = "Toggle password visibility",
                                    tint = Color.Unspecified
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF447AE3),
                            unfocusedBorderColor = Color.LightGray,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                }

                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage!!,
                        color = Color.Red,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .align(Alignment.Start)
                    )
                }

                Spacer(modifier = Modifier.height(30.dp))

                Button(
                    onClick = { onIntent(SignInIntent.SignInClicked) },
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF447AE3))
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "Sign In",
                            fontFamily = outfitFontFamily,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Don't have an account? ",
                        fontFamily = outfitFontFamily,
                        fontSize = 17.sp,
                        color = Color(0xFFB3B2B2)
                    )
                    Text(
                        text = "Sign Up",
                        fontFamily = outfitFontFamily,
                        fontSize = 17.sp,
                        color = Color(0xFF447AE3),
                        modifier = Modifier.clickable { onSignUpClick() }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, device = Devices.PIXEL_6)
@Composable
fun SignInScreenPreview() {
    PayOnAppTheme {
        SignInScreen(
            state = SignInState(
                username = "",
                password = "",
                isLoading = false,
                errorMessage = null
            ),
            onIntent = {},
            onSignUpClick = {}
        )
    }
}