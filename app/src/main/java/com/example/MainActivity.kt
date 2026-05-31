package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AdminMainDashboard
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.UserMainDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkMode.collectAsStateWithLifecycle()
            
            MyApplicationTheme(darkTheme = isDarkTheme) {
                // Toast notifications supervisor
                val context = LocalContext.current
                val toastMsg by viewModel.toastMessage.collectAsStateWithLifecycle()

                LaunchedEffect(toastMsg) {
                    toastMsg?.let { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        viewModel.clearToast()
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

                        if (currentUser == null) {
                            LoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = {}
                            )
                        } else {
                            val activeUser = currentUser!!
                            if (activeUser.role == "ADMIN") {
                                AdminMainDashboard(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                UserMainDashboard(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
