package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.EchoChatViewModel
import com.example.ui.screens.EchoChatApp

class MainActivity : ComponentActivity() {
  private lateinit var viewModel: EchoChatViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    viewModel = ViewModelProvider(this)[EchoChatViewModel::class.java]
    
    // Start background notification and calling service
    val serviceIntent = android.content.Intent(this, com.example.service.EchoNotificationService::class.java)
    try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    setContent {
      EchoChatApp(viewModel)
    }
  }

  override fun onStart() {
    super.onStart()
    viewModel.setUserOnlineStatus("online")
  }

  override fun onStop() {
    super.onStop()
    viewModel.setUserOnlineStatus("offline")
  }
}
