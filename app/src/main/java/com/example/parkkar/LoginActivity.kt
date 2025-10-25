package com.example.parkkar

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.example.parkkar.data.DatabaseHelper
import com.example.parkkar.ui.theme.ParkkarTheme
import com.example.parkkar.utils.showToast
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

private const val PREFS_NAME = "ParkkarPrefs"
private const val KEY_SAVED_USERNAME = "saved_username"
private const val TAG = "LoginActivity"

class LoginActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        dbHelper = DatabaseHelper(this)
        auth = Firebase.auth

        val oneTapClient = Identity.getSignInClient(this)
        val signInRequest = com.google.android.gms.auth.api.identity.BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                com.google.android.gms.auth.api.identity.BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()

        setContent {
            ParkkarTheme {
                val context = LocalContext.current
                val savedUsername = sharedPreferences.getString(KEY_SAVED_USERNAME, "") ?: ""
                val initialRememberMe = savedUsername.isNotEmpty()

                val googleSignInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult(),
                    onResult = { result ->
                        try {
                            val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                            val idToken = credential.googleIdToken
                            if (idToken != null) {
                                firebaseAuthWithGoogle(idToken)
                            }
                        } catch (e: ApiException) {
                            Log.w(TAG, "Google sign in failed", e)
                            showToast(context, "Google Sign-In failed.")
                        }
                    }
                )

                LoginScreen(
                    initialUsername = savedUsername,
                    initialRememberMe = initialRememberMe,
                    onLoginClicked = { (username, password, rememberMe) ->
                        if (username.isBlank() || password.isBlank()) {
                            showToast(context, "Please enter username/email and password.")
                            return@LoginScreen
                        }
                        val loginSuccess = dbHelper.checkUserCredentials(username, password)
                        if (loginSuccess) {
                            handleSuccessfulLogin(username, rememberMe, isGoogleSignIn = false)
                        } else {
                            dbHelper.addLogEntry(null, "Failed Login")
                            showToast(context, "Invalid Username/E-mail/Password")
                        }
                    },
                    onSignUpClicked = { navigateTo(SignUpActivity::class.java) },
                    onForgotPasswordClicked = { navigateTo(ForgotPasswordActivity::class.java) },
                    onGoogleSignInClicked = {
                        oneTapClient.beginSignIn(signInRequest)
                            .addOnSuccessListener(this) { result ->
                                val intentSenderRequest = androidx.activity.result.IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                                googleSignInLauncher.launch(intentSenderRequest)
                            }
                            .addOnFailureListener(this) { e ->
                                Log.e(TAG, "Google Sign-In failed: ${e.localizedMessage}")
                                showToast(this, "Google Sign-In is not available at the moment.")
                            }
                    }
                )
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Log.d(TAG, "Google sign-in successful for: ${user?.displayName}")
                    // For Google Sign-In, we'll use the email as the username for consistency
                    handleSuccessfulLogin(user?.email ?: "", shouldRemember = false, isGoogleSignIn = true)
                } else {
                    Log.w(TAG, "Google sign-in with credential failed", task.exception)
                    showToast(this, "Authentication failed.")
                }
            }
    }

    private fun handleSuccessfulLogin(username: String, shouldRemember: Boolean, isGoogleSignIn: Boolean) {
        val userId = dbHelper.getUserIdByUsername(username)
        dbHelper.addLogEntry(userId, if(isGoogleSignIn) "Successful Google Login" else "Successful Login")
        showToast(this, "Login Successful")

        if (shouldRemember) {
            sharedPreferences.edit { putString(KEY_SAVED_USERNAME, username) }
        } else {
            sharedPreferences.edit { remove(KEY_SAVED_USERNAME) }
        }

        navigateTo(HomeActivity::class.java, clearTask = true)
    }

    private fun <T> navigateTo(activity: Class<T>, clearTask: Boolean = false) {
        val intent = Intent(this, activity)
        if (clearTask) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        if (clearTask) finish()
    }
}

@Composable
fun LoginScreen(
    initialUsername: String,
    initialRememberMe: Boolean,
    onLoginClicked: (Triple<String, String, Boolean>) -> Unit,
    onSignUpClicked: () -> Unit,
    onForgotPasswordClicked: () -> Unit,
    onGoogleSignInClicked: () -> Unit
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
                Text("PARK-KAR", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF4A4A4A))
                Spacer(modifier = Modifier.width(8.dp))
                Image(painter = painterResource(id = R.drawable.logo), contentDescription = "Park-Kar Logo", modifier = Modifier.height(20.dp))
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Hi! Welcome", fontSize = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Text("I'm waiting for you, please enter your detail", fontSize = 16.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username or Email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                        Text("Remember Me", fontSize = 14.sp, color = Color.DarkGray)
                    }
                    Text(
                        text = "Forgot Password?",
                        modifier = Modifier.clickable { onForgotPasswordClicked() },
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onLoginClicked(Triple(username, password, rememberMe)) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF301934))
                ) {
                    Text("Log In", fontSize = 18.sp, color = Color.White)
                }

                // --- Google Sign-In Button --- 
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(" OR ", color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp))
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onGoogleSignInClicked,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Image(painter = painterResource(id = R.drawable.ic_google_logo), contentDescription = "Google Logo", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Sign in with Google", color = Color.DarkGray, fontSize = 16.sp)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Don't have an account? ", fontSize = 14.sp, color = Color.Gray)
                Text(
                    text = "Sign Up",
                    modifier = Modifier.clickable { onSignUpClicked() },
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun LoginScreenPreview() {
    ParkkarTheme {
        LoginScreen(
            initialUsername = "previewUser",
            initialRememberMe = true,
            onLoginClicked = {},
            onSignUpClicked = {},
            onForgotPasswordClicked = {},
            onGoogleSignInClicked = {}
        )
    }
}
