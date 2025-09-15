package com.example.parkkar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Added for back icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parkkar.data.DatabaseHelper
import com.example.parkkar.ui.theme.ParkkarTheme
import com.example.parkkar.utils.sha256
import com.example.parkkar.utils.isValidPassword
import com.example.parkkar.utils.showToast
import com.example.parkkar.R
import android.content.Intent // Already present, good.

class ForgotPasswordActivity : ComponentActivity() {
    private lateinit var dbHelper: DatabaseHelper

    @OptIn(ExperimentalMaterial3Api::class) // Needed for TopAppBar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = DatabaseHelper(this)

        setContent {
            ParkkarTheme {
                val context = LocalContext.current
                ForgotPasswordScreen(
                    onNavigateBack = { finish() }, // Action for AppBar back navigation
                    onResetPasswordClicked = { usernameOrEmail, newPassword, confirmNewPassword ->
                        if (usernameOrEmail.isBlank() || newPassword.isBlank() || confirmNewPassword.isBlank()) {
                            showToast(context, "All fields are required.")
                            return@ForgotPasswordScreen
                        }
                        if (!isValidPassword(newPassword)) {
                            showToast(context, "New password must be at least 6 characters and contain a number.")
                            return@ForgotPasswordScreen
                        }
                        if (newPassword != confirmNewPassword) {
                            showToast(context, "New passwords do not match.")
                            return@ForgotPasswordScreen
                        }
                        val userActuallyExists = dbHelper.checkUserExists(usernameOrEmail, usernameOrEmail)
                        if (!userActuallyExists) {
                             showToast(context, "User not found.")
                             return@ForgotPasswordScreen
                        }
                        val newHashedPassword = sha256(newPassword)
                        if (newHashedPassword.isEmpty()) {
                            showToast(context, "Error processing new password.")
                            return@ForgotPasswordScreen
                        }
                        val success = dbHelper.updatePasswordByUsernameOrEmail(usernameOrEmail, newHashedPassword)
                        if (success) {
                            showToast(context, "Password reset successfully.")
                            val intent = Intent(context, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                            finish()
                        } else {
                            showToast(context, "Password reset failed. Please try again.")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // Needed for TopAppBar
@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit, // Callback for back navigation from AppBar
    onResetPasswordClicked: (String, String, String) -> Unit
    // Removed onBackToLoginClicked as it's now handled by onNavigateBack
) {
    var usernameOrEmail by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmNewPassword by rememberSaveable { mutableStateOf("") }
    var newPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmNewPasswordVisible by rememberSaveable { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forgot Password") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Apply padding from Scaffold
                .padding(16.dp)      // Original content padding
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
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
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Reset Your Password",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter your username/email and new password.",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = usernameOrEmail,
                onValueChange = { usernameOrEmail = it },
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
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (newPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                        Icon(imageVector = image, if (newPasswordVisible) "Hide password" else "Show password")
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
                value = confirmNewPassword,
                onValueChange = { confirmNewPassword = it },
                label = { Text("Confirm New Password") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (confirmNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    val image = if (confirmNewPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { confirmNewPasswordVisible = !confirmNewPasswordVisible }) {
                        Icon(imageVector = image, if (confirmNewPasswordVisible) "Hide password" else "Show password")
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.LightGray
                )
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { onResetPasswordClicked(usernameOrEmail, newPassword, confirmNewPassword) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF301934))
            ) {
                Text("Reset Password", fontSize = 18.sp, color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onNavigateBack ) { // Uses onNavigateBack from AppBar
                Text("Back to Login")
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun ForgotPasswordScreenPreview() {
    ParkkarTheme {
        ForgotPasswordScreen(
            onNavigateBack = {}, // Added for preview
            onResetPasswordClicked = { _, _, _ -> /* Preview action */ }
        )
    }
}
