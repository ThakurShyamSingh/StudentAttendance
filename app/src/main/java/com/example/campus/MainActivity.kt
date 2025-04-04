package com.example.campus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.campus.ui.AppNavigation
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import android.util.Log
import com.google.android.gms.security.ProviderInstaller
import android.content.Context
import com.example.campus.worker.SyncManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSecurityProvider(this)

        SyncManager.startAutoSync(applicationContext)

        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        setContent {
            AppNavigation()
        }
    }

    fun installSecurityProvider(context: Context) {
        try {
            ProviderInstaller.installIfNeeded(context)
            Log.d("SecurityProvider", "Security Provider installed successfully.")
        } catch (e: Exception) {
            Log.e("SecurityProvider", "Failed to install Security Provider", e)
        }
    }
}
