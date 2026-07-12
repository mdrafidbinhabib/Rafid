package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.ui.EchoChatViewModel
import com.example.ui.screens.EchoChatApp
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
  private lateinit var viewModel: EchoChatViewModel

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    viewModel = ViewModelProvider(this)[EchoChatViewModel::class.java]

    lifecycleScope.launch {
      viewModel.isAppLockEnabled.collect { enabled ->
        if (enabled) {
          window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        } else {
          window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
      }
    }

    setContent {
      EchoChatApp(viewModel)
    }
  }

  override fun onStart() {
    super.onStart()
    viewModel.setUserOnlineStatus("online")
    viewModel.onAppStartResume()
  }

  override fun onStop() {
    super.onStop()
    viewModel.setUserOnlineStatus("offline")
    viewModel.onAppBackground()
  }
}
