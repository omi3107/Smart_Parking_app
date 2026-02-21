
package com.example.parkkar

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.parkkar.data.DatabaseHelper
import com.example.parkkar.data.UserPreferencesRepository
import com.example.parkkar.model.UserDetails
import com.example.parkkar.ui.theme.ParkkarTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PersonalDetailsActivity : ComponentActivity() {

    private val viewModel: PersonalDetailsViewModel by viewModels {
        PersonalDetailsViewModelFactory(UserPreferencesRepository.getInstance(this), DatabaseHelper(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState(initial = false)
            ParkkarTheme(darkTheme = isDarkTheme) {
                PersonalDetailsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDetailsScreen(
    viewModel: PersonalDetailsViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val userDetails by viewModel.userDetails.collectAsState()
    val isGoogleSignIn by viewModel.isGoogleSignIn.collectAsState()
    var isEditable by remember { mutableStateOf(false) }

    var name by remember(userDetails) { mutableStateOf(userDetails?.name ?: "") }
    var username by remember(userDetails) { mutableStateOf(userDetails?.username ?: "") }
    var email by remember(userDetails) { mutableStateOf(userDetails?.email ?: "") }
    var phone by remember(userDetails) { mutableStateOf(userDetails?.phone ?: "") }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personal Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = !isEditable || isGoogleSignIn,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = !isEditable || isGoogleSignIn
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = !isEditable || isGoogleSignIn
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = !isEditable || isGoogleSignIn
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (isGoogleSignIn) {
                LaunchedEffect(Unit) {
                    Toast.makeText(context, "Details from Google Sign-In are not editable.", Toast.LENGTH_LONG).show()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (isEditable) {
                        Button(
                            onClick = {
                                viewModel.saveUserDetails(name, username, email, phone)
                                isEditable = false
                            }
                        ) {
                            Text("Save")
                        }
                    } else {
                        Button(onClick = { isEditable = true }) {
                            Text("Edit")
                        }
                    }
                }
            }
        }
    }
}

class PersonalDetailsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dbHelper: DatabaseHelper
) : ViewModel() {

    private val _userDetails = MutableStateFlow<UserDetails?>(null)
    val userDetails: StateFlow<UserDetails?> = _userDetails

    private val _isGoogleSignIn = MutableStateFlow(false)
    val isGoogleSignIn: StateFlow<Boolean> = _isGoogleSignIn

    val isDarkTheme = userPreferencesRepository.isDarkTheme
    private var userId: Int? = null

    init {
        viewModelScope.launch {
            val isGoogle = userPreferencesRepository.isGoogleSignIn.first()
            _isGoogleSignIn.value = isGoogle

            if (isGoogle) {
                val user = FirebaseAuth.getInstance().currentUser
                val name = user?.displayName ?: ""
                val email = user?.email ?: ""
                _userDetails.value = UserDetails(name, "-", email, "-")
            } else {
                val usernameOrEmail = userPreferencesRepository.userName.first()
                if (usernameOrEmail.isNotEmpty()) {
                    userId = dbHelper.getUserIdByUsername(usernameOrEmail)
                    val dbUserDetails = dbHelper.getUserDetails(usernameOrEmail)
                    _userDetails.value = dbUserDetails
                }
            }
        }
    }


    fun saveUserDetails(name: String, username: String, email: String, phone: String) {
        viewModelScope.launch {
            userId?.let {
                val success = dbHelper.updateUserDetails(it, name, username, email, phone)
                if (success) {
                    userPreferencesRepository.setUserName(username) // Update the username in preferences
                    _userDetails.value = UserDetails(name, username, email, phone)
                }
            }
        }
    }
}

class PersonalDetailsViewModelFactory(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dbHelper: DatabaseHelper
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PersonalDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PersonalDetailsViewModel(userPreferencesRepository, dbHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
