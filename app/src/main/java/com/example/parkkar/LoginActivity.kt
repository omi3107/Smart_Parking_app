package com.example.parkkar

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parkkar.data.DatabaseHelper
import com.example.parkkar.utils.sha256
import com.example.parkkar.HomeActivity
import com.example.parkkar.ui.theme.ParkkarTheme // IMPORT CENTRALIZED THEME
import com.example.parkkar.utils.showToast // IMPORT CENTRALIZED TOAST
import com.example.parkkar.R // IMPORT R CLASS

// SharedPreferences Constants
private const val PREFS_NAME = "ParkkarPrefs"
private const val KEY_SAVED_USERNAME = "saved_username"

// ParkkarTheme local definition was REMOVED from here.

class LoginActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        dbHelper = DatabaseHelper(this)

        setContent {
            ParkkarTheme { // Uses imported ParkkarTheme
                val context = LocalContext.current
                val savedUsername = sharedPreferences.getString(KEY_SAVED_USERNAME, "") ?: ""
                val initialRememberMe = savedUsername.isNotEmpty()

                LoginScreen(
                    initialUsername = savedUsername,
                    initialRememberMe = initialRememberMe,
                    onLoginClicked = { usernameInput, passwordInput, rememberMeChecked ->
                        if (usernameInput.isBlank() || passwordInput.isBlank()) {
                            showToast(context, "Please enter username/email and password.")
                            return@LoginScreen
                        }

                        val loginSuccess = dbHelper.checkUserCredentials(usernameInput, passwordInput)

                        if (loginSuccess) {
                            showToast(context, "Login Successful")
                            if (rememberMeChecked) {
                                sharedPreferences.edit().putString(KEY_SAVED_USERNAME, usernameInput).apply()
                            } else {
                                sharedPreferences.edit().remove(KEY_SAVED_USERNAME).apply()
                            }
                            val intent = Intent(context, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                            finish()
                        } else {
                            showToast(context, "Invalid Username/E-mail/Phone Number or Password")
                        }
                    },
                    onSignUpClicked = {
                        val intent = Intent(context, SignUpActivity::class.java)
                        context.startActivity(intent)
                    },
                    onForgotPasswordClicked = {
                        val intent = Intent(context, ForgotPasswordActivity::class.java) // Navigate to ForgotPasswordActivity
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    initialUsername: String,
    initialRememberMe: Boolean,
    onLoginClicked: (String, String, Boolean) -> Unit,
    onSignUpClicked: () -> Unit,
    onForgotPasswordClicked: () -> Unit
) {
    var username by rememberSaveable { mutableStateOf(initialUsername) }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var rememberMe by rememberSaveable { mutableStateOf(initialRememberMe) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PARK-KAR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF4A4A4A) 
                )
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Park-Kar Logo",
                    modifier = Modifier.height(20.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Hi! Welcome",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "I'm waiting for you, please enter your detail",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username, Email or Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (passwordVisible)
                            Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff
                        val description = if (passwordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, description)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text("Remember Me", fontSize = 14.sp, color = Color.DarkGray)
                    }
                    ClickableText(
                        text = AnnotatedString("Forgot Password?"),
                        onClick = { onForgotPasswordClicked() },
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onLoginClicked(username, password, rememberMe) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF301934))
                ) {
                    Text("Log In", fontSize = 18.sp, color = Color.White)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Don\'t have an account? ", fontSize = 14.sp, color = Color.Gray)
                ClickableText(
                    text = AnnotatedString("Sign Up"),
                    onClick = { onSignUpClicked() },
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun LoginScreenPreview() {
    ParkkarTheme { // Uses imported ParkkarTheme
        LoginScreen(
            initialUsername = "previewUser",
            initialRememberMe = true,
            onLoginClicked = { _, _, _ -> /* Preview: No action */ },
            onSignUpClicked = { /* Preview: No action */ },
            onForgotPasswordClicked = { /* Preview: No action */ }
        )
    }
}
