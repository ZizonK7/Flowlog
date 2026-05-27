package com.example.flowlog

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Switch
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.flowlog.data.local.UserRole
import com.example.flowlog.data.local.UserRoleStore
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class MainActivity : ComponentActivity() {
    private var requestedScreen by mutableStateOf(SCREEN_HOME)
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

                val userRoleStore = remember { UserRoleStore(this@MainActivity) }
                val isDeveloper = remember(signedInUser) {
                    userRoleStore.roleForUid(signedInUser?.uid) == UserRole.DEVELOPER
                }
                var isDeveloperMode by remember { mutableStateOf(userRoleStore.isDeveloperMode()) }
                LaunchedEffect(isDeveloper) {
                    if (!isDeveloper && isDeveloperMode) {
                        isDeveloperMode = false
                        userRoleStore.setDeveloperMode(false)
                    }
                }

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
                val openStatsSite: () -> Unit = {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://flowlog.pfkfks.org/statistics/")
                        )
                    )
                }
                val openDeveloperBlog: () -> Unit = {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://blog.pfkfks.org/blog/")
                        )
                    )
                }

                val pagerState = rememberPagerState(
                    initialPage = if (currentScreen == "todo") 1 else 0,
                    pageCount = { 2 }
                )

                LaunchedEffect(pagerState.currentPage) {
                    currentScreen = if (pagerState.currentPage == 0) "home" else "todo"
                }

                LaunchedEffect(currentScreen) {
                    val targetPage = if (currentScreen == "todo") 1 else 0
                    if (pagerState.currentPage != targetPage) {
                        pagerState.animateScrollToPage(targetPage)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                    )
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f)
                        ) { page ->
                            if (page == 1) {
                                TodoScreen(
                                    viewModel = todoViewModel,
                                    isDeveloperMode = isDeveloperMode,
                                    onStartTodo = { todo ->
                                        activityViewModel.startTodoActivity(todo.id, todo.title)
                                        currentScreen = "home"
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                HomeScreen(
                                    viewModel = activityViewModel,
                                    topActions = {
                                        HeaderActions(
                                            isSignedIn = signedInUser != null,
                                            syncStatus = syncStatus,
                                            profilePhotoUrl = signedInUser?.photoUrl?.toString(),
                                            onStatsClick = openStatsSite,
                                            onBlogClick = openDeveloperBlog,
                                            onAccountClick = runAccountSync,
                                            isDeveloper = isDeveloper,
                                            isDeveloperMode = isDeveloperMode,
                                            onToggleDevMode = {
                                                val newMode = !isDeveloperMode
                                                isDeveloperMode = newMode
                                                userRoleStore.setDeveloperMode(newMode)
                                            }
                                        )
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        FlowlogBottomBar(
                            currentScreen = currentScreen,
                            onHomeClick = { currentScreen = "home" },
                            onTodoClick = { currentScreen = "todo" }
                        )
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
        const val EXTRA_OPEN_SCREEN = "com.example.flowlog.extra.OPEN_SCREEN"
        const val SCREEN_HOME = "home"
        const val SCREEN_TODO = "todo"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
}

@Composable
private fun HeaderActions(
    isSignedIn: Boolean,
    syncStatus: String?,
    profilePhotoUrl: String?,
    onStatsClick: () -> Unit,
    onBlogClick: () -> Unit,
    onAccountClick: () -> Unit,
    isDeveloper: Boolean = false,
    isDeveloperMode: Boolean = false,
    onToggleDevMode: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onStatsClick,
            modifier = Modifier.size(52.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0ECFF))
                    .border(1.dp, Color(0xFFE0D7FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = "통계",
                    tint = Color(0xFF5140D8),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(52.dp)
            ) {
                ProfileAvatar(
                    profilePhotoUrl = profilePhotoUrl,
                    isSignedIn = isSignedIn
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(accountActionLabel(isSignedIn, syncStatus)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onAccountClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("개발자 블로그") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Article,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onBlogClick()
                    }
                )
                if (isDeveloper) {
                    DropdownMenuItem(
                        text = { Text("개발자 모드") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Build,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            Switch(
                                checked = isDeveloperMode,
                                onCheckedChange = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleDevMode()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    profilePhotoUrl: String?,
    isSignedIn: Boolean
) {
    var profileImage by remember(profilePhotoUrl) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(profilePhotoUrl) {
        profileImage = null
        val url = profilePhotoUrl ?: return@LaunchedEffect
        profileImage = withContext(Dispatchers.IO) {
            runCatching {
                URL(url).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color(0xFFF0ECFF))
            .border(1.dp, Color(0xFFE0D7FF), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val image = profileImage
        if (isSignedIn && image != null) {
            Image(
                bitmap = image,
                contentDescription = "구글 프로필",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = "기본 프로필",
                tint = Color(0xFF5140D8),
                modifier = Modifier.size(31.dp)
            )
        }
    }
}

@Composable
private fun FlowlogBottomBar(
    currentScreen: String,
    onHomeClick: () -> Unit,
    onTodoClick: () -> Unit
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    NavigationBar(
        modifier = Modifier
            .padding(bottom = bottomInset)
            .height(68.dp),
        containerColor = Color.White,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) {
        NavigationBarItem(
            selected = currentScreen == "home",
            onClick = onHomeClick,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "홈"
                )
            },
            label = { Text("홈") }
        )
        NavigationBarItem(
            selected = currentScreen == "todo",
            onClick = onTodoClick,
            icon = {
                Icon(
                    imageVector = Icons.Filled.CheckBox,
                    contentDescription = "Todo"
                )
            },
            label = { Text("Todo") }
        )
    }
}

private fun accountActionLabel(isSignedIn: Boolean, syncStatus: String?): String {
    return when (syncStatus) {
        "Signing in..." -> "로그인 중..."
        null -> if (isSignedIn) "로그아웃" else "로그인"
        else -> if (isSignedIn) "로그아웃" else "로그인"
    }
}
