package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.ui.EchoChatViewModel
import com.example.ui.screens.EchoChatApp

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val viewModel = ViewModelProvider(this)[EchoChatViewModel::class.java]
    setContent {
      EchoChatApp(viewModel)
    }
  }
}
