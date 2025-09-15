package com.example.parkkar

import android.content.Context // Keep for LocalContext if needed, or if other direct context uses remain
import android.content.Intent
import android.os.Bundle
// import android.widget.Toast // Removed as showToast will be imported
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parkkar.data.DatabaseHelper
import com.example.parkkar.utils.sha256
import com.example.parkkar.utils.isValidPassword // Import from utils
import com.example.parkkar.utils.showToast // Import from utils
import com.example.parkkar.ui.theme.ParkkarTheme // Import centralized theme
import com.example.parkkar.R // Import R class for resources
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SignUpActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ParkkarTheme { // Uses imported ParkkarTheme
                val context = LocalContext.current
                val dbHelper = remember { DatabaseHelper(context) } 

                SignUpScreen(
                    onSignUpClicked = { emailOrPhone, fullName, username, password, confirmPassword ->
                        if (emailOrPhone.isBlank() || fullName.isBlank() || username.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
                            showToast(context, "All fields are required.")
                            return@SignUpScreen
                        }

                        if (!isValidPassword(password)) {
                            showToast(context, "Password must be at least 6 characters and contain a number.")
                            return@SignUpScreen
                        }

                        if (password != confirmPassword) {
                            showToast(context, "Passwords do not match.")
                            return@SignUpScreen
                        }

                        if (dbHelper.checkUserExists(username, emailOrPhone)) {
                            showToast(context, "Username or Email/Phone already exists.")
                            return@SignUpScreen
                        }

                        val hashedPassword = sha256(password)
                        if (hashedPassword.isEmpty()) {
                             showToast(context, "Error hashing password. Please try again.")
                             return@SignUpScreen
                        }

                        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val sdfTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        val currentDate = sdfDate.format(Date())
                        val currentTimestamp = sdfTimestamp.format(Date())

                        val newUserId = dbHelper.addUser(emailOrPhone, fullName, username, hashedPassword, currentDate, currentTimestamp)

                        if (newUserId > -1) { 
                            showToast(context, "Account Created Successfully")
                            val intent = Intent(context, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                            finish()
                        } else {
                            showToast(context, "Sign up failed. Please try again.")
                        }
                    },
                    onLoginClicked = {
                        finish()
                    }
                )
            }
        }
    }
}

// isValidPassword function was REMOVED from here (moved to utils.Crypto.kt)
// showToast function was REMOVED from here (moved to utils.Crypto.kt)

@Composable
fun SignUpScreen(
    onSignUpClicked: (String, String, String, String, String) -> Unit,
    onLoginClicked: () -> Unit
) {
    var emailOrPhone by rememberSaveable { mutableStateOf("") }
    var fullName by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
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
                    .weight(1f, fill = false)
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
                    text = "Let's create an account",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = emailOrPhone,
                    onValueChange = { emailOrPhone = it },
                    label = { Text("Email or Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
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
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (passwordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, description)
                        }
                    },
                    supportingText = { Text("Must contain a number and least of 6 characters") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirm Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (confirmPasswordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(imageVector = image, description)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onSignUpClicked(emailOrPhone, fullName, username, password, confirmPassword) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF301934))
                ) {
                    Text("Sign Up", fontSize = 18.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Have an account? ", fontSize = 14.sp, color = Color.Gray)
                ClickableText(
                    text = AnnotatedString("Log In"),
                    onClick = { onLoginClicked() },
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A4A4A) 
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun SignUpScreenPreview() {
    ParkkarTheme { // Uses imported ParkkarTheme
        val context = LocalContext.current
        SignUpScreen(
            onSignUpClicked = { _, _, _, _, _ -> showToast(context, "Preview: Sign Up Clicked") },
            onLoginClicked = { showToast(context, "Preview: Login Clicked") }
        )
    }
}
