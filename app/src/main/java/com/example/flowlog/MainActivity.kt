package com.example.flowlog

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.flowlog.data.remote.FlowlogCloudSync
import com.example.flowlog.data.remote.awaitResult
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.example.flowlog.notification.ReminderScheduler
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.flowlog.ui.screen.HomeScreen
import com.example.flowlog.ui.screen.TodoScreen
import com.example.flowlog.ui.theme.FlowlogTheme
import com.example.flowlog.ui.viewmodel.ActivityViewModel
import com.example.flowlog.ui.viewmodel.ActivityViewModelFactory
import com.example.flowlog.ui.viewmodel.TodoViewModel
import com.example.flowlog.ui.viewmodel.TodoViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var widgetStartCategory by mutableStateOf<String?>(null)
    private var requestedScreen by mutableStateOf(SCREEN_HOME)
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetStartCategory = intent.getStringExtra(EXTRA_START_CATEGORY)
        requestedScreen = intent.getStringExtra(EXTRA_OPEN_SCREEN) ?: SCREEN_HOME
        ReminderScheduler(applicationContext).ensureNotificationChannel()
        requestNotificationPermission()
        requestExactAlarmPermission()
        enableEdgeToEdge()
        setContent {
            FlowlogTheme {
                var currentScreen by remember { mutableStateOf(requestedScreen) }
                LaunchedEffect(requestedScreen) {
                    currentScreen = requestedScreen
                }
                val activityViewModel: ActivityViewModel = remember {
                    ViewModelProvider(
                        this@MainActivity,
                        ActivityViewModelFactory(this@MainActivity)
                    ).get(ActivityViewModel::class.java)
                }
                val todoViewModel: TodoViewModel = remember {
                    ViewModelProvider(
                        this@MainActivity,
                        TodoViewModelFactory(this@MainActivity)
                    ).get(TodoViewModel::class.java)
                }
                val auth = remember { FirebaseAuth.getInstance() }
                var signedInUser by remember { mutableStateOf(auth.currentUser) }
                var syncStatus by remember { mutableStateOf<String?>(null) }
                val scope = rememberCoroutineScope()

                DisposableEffect(auth) {
                    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        signedInUser = firebaseAuth.currentUser
                    }
                    auth.addAuthStateListener(listener)
                    onDispose {
                        auth.removeAuthStateListener(listener)
                    }
                }
                LaunchedEffect(signedInUser) {
                    if (signedInUser != null) {
                        runCatching {
                            uploadLocalFlowlogSnapshot()
                        }
                    }
                }
                val runAccountSync: () -> Unit = {
                    scope.launch {
                        if (signedInUser != null) {
                            auth.signOut()
                            syncStatus = null
                            return@launch
                        }

                        syncStatus = "Signing in..."
                        val signInResult = runCatching {
                            signInWithGoogle()
                            uploadLocalFlowlogSnapshot()
                        }
                        syncStatus = signInResult.exceptionOrNull()?.let { error ->
                            error.localizedMessage ?: "Sign-in failed"
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            "todo" -> TodoScreen(
                                viewModel = todoViewModel,
                                onStartTodo = { todo ->
                                    activityViewModel.startTodoActivity(todo.id, todo.title)
                                    currentScreen = "home"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            else -> HomeScreen(
                                viewModel = activityViewModel,
                                startCategoryRequest = widgetStartCategory,
                                onStartCategoryConsumed = { widgetStartCategory = null },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { currentScreen = "home" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentScreen == "home") Color(0xFF1976D2) else Color(0xFF90A4AE)
                                )
                            ) {
                                Text("홈")
                            }
                            Button(
                                onClick = { currentScreen = "todo" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (currentScreen == "todo") Color(0xFF1976D2) else Color(0xFF90A4AE)
                                )
                            ) {
                                Text("Todo")
                            }
                            Button(
                                onClick = runAccountSync,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (signedInUser == null) Color(0xFF00897B) else Color(0xFF455A64)
                                )
                            ) {
                                Text(accountActionLabel(signedInUser != null, syncStatus))
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerNetworkSync()
    }

    override fun onStop() {
        unregisterNetworkSync()
        super.onStop()
    }

    private suspend fun signInWithGoogle() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val credential = CredentialManager.create(this).getCredential(
            context = this,
            request = request
        ).credential
        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).awaitResult()
    }

    private suspend fun uploadLocalFlowlogSnapshot() {
        FlowlogCloudSync(applicationContext).uploadLocalSnapshot()
    }

    private fun registerNetworkSync() {
        if (networkCallback != null) return

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (FirebaseAuth.getInstance().currentUser == null) return
                lifecycleScope.launch {
                    runCatching {
                        FlowlogCloudSync(applicationContext).uploadLocalSnapshot()
                    }
                }
            }
        }
        networkCallback = callback
        connectivityManager.registerNetworkCallback(request, callback)
    }

    private fun unregisterNetworkSync() {
        val callback = networkCallback ?: return
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        runCatching {
            connectivityManager.unregisterNetworkCallback(callback)
        }
        networkCallback = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        widgetStartCategory = intent.getStringExtra(EXTRA_START_CATEGORY)
        requestedScreen = intent.getStringExtra(EXTRA_OPEN_SCREEN) ?: requestedScreen
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) return

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (alarmManager.canScheduleExactAlarms()) return

        runCatching {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        }
    }

    companion object {
        const val EXTRA_START_CATEGORY = "com.example.flowlog.extra.START_CATEGORY"
        const val EXTRA_OPEN_SCREEN = "com.example.flowlog.extra.OPEN_SCREEN"
        const val SCREEN_HOME = "home"
        const val SCREEN_TODO = "todo"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
}

private fun accountActionLabel(isSignedIn: Boolean, syncStatus: String?): String {
    return when (syncStatus) {
        "Signing in..." -> "Login..."
        null -> if (isSignedIn) "Logout" else "Login"
        else -> if (isSignedIn) "Logout" else "Login"
    }
}
