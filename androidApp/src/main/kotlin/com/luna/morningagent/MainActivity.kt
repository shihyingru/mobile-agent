package com.luna.morningagent

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.luna.morningagent.data.secure.TokenStore
import com.luna.morningagent.worker.BriefingScheduler
import com.luna.morningagent.worker.EveningScheduler
import com.luna.morningagent.worker.MorningAgentWorker

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* user choice, no follow-up */ }

    // Incremented on every intent that carries EXTRA_FROM_NOTIFICATION. Read by
    // MorningAgentApp to (a) skip the Launch screen on cold start, and (b) snap
    // back to Home if the user was on Settings when the notification fired.
    private var notificationTick by mutableIntStateOf(0)

    override fun attachBaseContext(newBase: Context) {
        val store = TokenStore(newBase)
        super.attachBaseContext(LocaleHelper.applyLocale(newBase, store.getAppLanguage()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // System splash hands off cleanly to LaunchScreen — no white flash
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Notification channel + KEEP-policy schedule. Both idempotent so this
        // is safe on every launch; honors each toggle in Settings.
        MorningAgentWorker.ensureNotificationChannel(this)
        val store = TokenStore(this)
        val morningOn = store.getDailyBriefingEnabled()
        val eveningOn = store.getDailyEveningEnabled()
        if (morningOn) BriefingScheduler.ensure(this)
        if (eveningOn) EveningScheduler.ensure(this)
        if (morningOn || eveningOn) maybeRequestNotificationPermission()

        consumeNotificationIntent(intent)

        setContent {
            MorningAgentApp(notificationTick = notificationTick)
        }
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)
        intent = newIntent
        consumeNotificationIntent(newIntent)
    }

    private fun consumeNotificationIntent(intent: Intent?) {
        val fromNotification = intent
            ?.getBooleanExtra(MorningAgentWorker.EXTRA_FROM_NOTIFICATION, false)
            ?: false
        if (fromNotification) notificationTick++
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
