package com.example.flowlog.appcheck

import android.util.Log
import com.google.firebase.appcheck.FirebaseAppCheck

/**
 * Installs the Firebase App Check provider: Play Integrity in release builds, the debug
 * provider in debug builds (chosen by source set, see AppCheckProviders.kt in
 * src/debug and src/release). It has no effect on requests until enforcement is turned
 * on in the Firebase console. Failure (for example a checkout without
 * google-services.json, where FirebaseApp is not initialized) is logged, never fatal.
 */
object FlowlogAppCheck {
    private const val TAG = "FlowlogAppCheck"

    fun install() {
        runCatching {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(appCheckProviderFactory())
        }.onFailure { Log.w(TAG, "App Check provider not installed", it) }
    }
}
