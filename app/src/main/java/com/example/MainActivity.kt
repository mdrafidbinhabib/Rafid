package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.ui.EchoChatViewModel
import com.example.ui.screens.EchoChatApp

class MainActivity : FragmentActivity() {
  private lateinit var viewModel: EchoChatViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    viewModel = ViewModelProvider(this)[EchoChatViewModel::class.java]

    setContent {
      EchoChatApp(viewModel)
    }
  }

  override fun onStart() {
    super.onStart()
    viewModel.setUserOnlineStatus("online")
    viewModel.setAppLocked(true)
  }

  override fun onStop() {
    super.onStop()
    viewModel.setUserOnlineStatus("offline")
  }
}
