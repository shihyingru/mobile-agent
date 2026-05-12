package com.luna.morningagent

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.luna.morningagent.data.secure.TokenStore
import com.luna.morningagent.worker.BriefingScheduler
import com.luna.morningagent.worker.MorningAgentWorker

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* user choice, no follow-up */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // System splash hands off cleanly to LaunchScreen — no white flash
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Notification channel + KEEP-policy schedule. Both idempotent so this
        // is safe on every launch; honors the user's toggle in Settings.
        MorningAgentWorker.ensureNotificationChannel(this)
        val store = TokenStore(this)
        if (store.getDailyBriefingEnabled()) {
            BriefingScheduler.ensure(this)
            maybeRequestNotificationPermission()
        }

        setContent {
            MorningAgentApp()
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
