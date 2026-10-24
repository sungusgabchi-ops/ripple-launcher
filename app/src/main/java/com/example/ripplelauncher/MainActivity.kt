package com.example.ripplelauncher

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ripplelauncher.ui.LauncherScreen
import com.example.ripplelauncher.ui.RippleTheme

class MainActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            RippleTheme {
                LauncherScreen(vm)
            }
        }
    }

    // Pressing Home while the launcher is already open lands here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        vm.goHome(collapse = true)
    }
}
