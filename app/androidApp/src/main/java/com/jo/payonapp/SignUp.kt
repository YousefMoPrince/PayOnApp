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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.jo.payonapp.intent.SignUpIntent
import com.jo.payonapp.sideeffect.SignUpSideEffect
import com.jo.payonapp.state.SignUpState
import com.jo.payonapp.ui.theme.PayOnAppTheme
import com.jo.payonapp.viewmodel.SignUpViewModel
import org.koin.android.ext.android.inject

class SignUpActivity : ComponentActivity() {

    private val signUpViewModel: SignUpViewModel by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PayOnAppTheme {
                val state by signUpViewModel.state.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    signUpViewModel.sideEffect.collect { effect ->
                        when (effect) {
                            is SignUpSideEffect.NavigateToUploadThumbnail -> {
                                val intent = Intent(this@SignUpActivity, UploadThumbnailActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            is SignUpSideEffect.ShowToast -> {
                                Toast.makeText(this@SignUpActivity, effect.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                SignUpScreen(
                    state = state,
                    onIntent = { intent -> signUpViewModel.processIntent(intent) },
                    onSignInClick = {
                        val intent = Intent(this@SignUpActivity, SignInActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun SignUpScreen(
    state: SignUpState,
    onIntent: (SignUpIntent) -> Unit,
    onSignInClick: () -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isVerifyPasswordVisible by remember { mutableStateOf(false) }

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
                .padding(top = 30.dp)
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
                    text = "Sign-Up",
                    fontFamily = outfitFontFamily,
                    fontSize = 23.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(top = 25.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage!!,
                        color = Color.Red,
                        fontFamily = outfitFontFamily,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                SignUpInputField(
                    label = "Username",
                    value = state.username,
                    placeholder = "Enter your username",
                    onValueChange = { onIntent(SignUpIntent.UsernameChanged(it)) },
                    fontFamily = outfitFontFamily
                )
                Spacer(modifier = Modifier.height(10.dp))

                SignUpInputField(
                    label = "Full Name",
                    value = state.fullName,
                    placeholder = "Enter full name",
                    onValueChange = { onIntent(SignUpIntent.FullNameChanged(it)) },
                    fontFamily = outfitFontFamily
                )
                Spacer(modifier = Modifier.height(10.dp))

                SignUpInputField(
                    label = "E-mail",
                    value = state.email,
                    placeholder = "Enter email",
                    keyboardType = KeyboardType.Email,
                    onValueChange = { onIntent(SignUpIntent.EmailChanged(it)) },
                    fontFamily = outfitFontFamily
                )
                Spacer(modifier = Modifier.height(10.dp))

                SignUpInputField(
                    label = "Phone",
                    value = state.phoneNumber,
                    placeholder = "Enter phone number",
                    keyboardType = KeyboardType.Phone,
                    onValueChange = { onIntent(SignUpIntent.PhoneNumberChanged(it)) },
                    fontFamily = outfitFontFamily
                )
                Spacer(modifier = Modifier.height(10.dp))

                SignUpPasswordField(
                    label = "Password",
                    value = state.password,
                    placeholder = "Enter password",
                    isPasswordVisible = isPasswordVisible,
                    onVisibilityToggle = { isPasswordVisible = !isPasswordVisible },
                    onValueChange = { onIntent(SignUpIntent.PasswordChanged(it)) },
                    fontFamily = outfitFontFamily
                )
                Spacer(modifier = Modifier.height(10.dp))

                SignUpPasswordField(
                    label = "Verify-Password",
                    value = state.verifyPassword,
                    placeholder = "Enter password again",
                    isPasswordVisible = isVerifyPasswordVisible,
                    onVisibilityToggle = { isVerifyPasswordVisible = !isVerifyPasswordVisible },
                    onValueChange = { onIntent(SignUpIntent.VerifyPasswordChanged(it)) },
                    fontFamily = outfitFontFamily
                )

                Spacer(modifier = Modifier.height(30.dp))

                Button(
                    onClick = { onIntent(SignUpIntent.SignUpClicked) },
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
                            text = "Sign Up",
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
                        text = "Already have an account? ",
                        fontFamily = outfitFontFamily,
                        fontSize = 17.sp,
                        color = Color(0xFFB3B2B2)
                    )
                    Text(
                        text = "Login",
                        fontFamily = outfitFontFamily,
                        fontSize = 17.sp,
                        color = Color(0xFF447AE3),
                        modifier = Modifier.clickable { onSignInClick() }
                    )
                }
            }
        }
    }
}

@Composable
fun SignUpInputField(
    label: String,
    value: String,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit,
    fontFamily: FontFamily
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontFamily = fontFamily,
            fontSize = 17.sp,
            color = Color(0xFF787777)
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
}

@Composable
fun SignUpPasswordField(
    label: String,
    value: String,
    placeholder: String,
    isPasswordVisible: Boolean,
    onVisibilityToggle: () -> Unit,
    onValueChange: (String) -> Unit,
    fontFamily: FontFamily
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontFamily = fontFamily,
            fontSize = 17.sp,
            color = Color(0xFF787777)
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val iconRes = if (isPasswordVisible) R.drawable.visibility else R.drawable.visibility_off
                IconButton(onClick = onVisibilityToggle) {
                    Icon(
                        painter = painterResource(iconRes),
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
}

@Preview(showBackground = true, showSystemUi = true, device = Devices.PIXEL_6A)
@Composable
fun SignUpScreenPreview() {
    PayOnAppTheme {
        SignUpScreen(
            state = SignUpState(username = "Yousef", email = "test@test.com"),
            onIntent = {},
            onSignInClick = {}
        )
    }
}