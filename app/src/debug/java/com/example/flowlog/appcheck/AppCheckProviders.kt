package com.example.flowlog.appcheck

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds: the debug provider prints a debug token to logcat on first use.
 * Register that token in the Firebase console (App Check > Apps > Manage debug tokens).
 * Lives in the debug source set because `firebase-appcheck-debug` is a debugImplementation.
 */
internal fun appCheckProviderFactory(): AppCheckProviderFactory = DebugAppCheckProviderFactory.getInstance()
