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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.runtime.collectAsState
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
import com.example.flowlog.data.local.db.FlowlogDatabase
import com.example.flowlog.data.remote.awaitResult
import com.example.flowlog.data.sync.FirebaseRestoreDataSource
import com.example.flowlog.data.sync.FirebaseSyncAlarmScheduler
import com.example.flowlog.data.sync.FirebaseSyncCoordinator
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.example.flowlog.notification.ActivityTimerNotifier
import com.example.flowlog.notification.ReminderScheduler
import com.example.flowlog.notification.AutoButtonScheduler
import com.example.flowlog.notification.PlannedTodoReminderScheduler
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.flowlog.ui.screen.DevTimetableScreen
import com.example.flowlog.ui.screen.HomeScreen
import com.example.flowlog.ui.screen.TodoScreen
import com.example.flowlog.ui.theme.FlowlogTheme
import com.example.flowlog.ui.viewmodel.ActivityViewModel
import com.example.flowlog.ui.viewmodel.ActivityViewModelFactory
import com.example.flowlog.ui.viewmodel.TodoViewModel
import com.example.flowlog.ui.viewmodel.TodoViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.URL
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DoNotDisturb
import com.example.flowlog.notification.FocusDndController
import com.example.flowlog.data.model.AiMessage
import com.example.flowlog.data.model.RecommendationStatus
import com.example.flowlog.ui.component.displayCategory
import com.example.flowlog.ui.viewmodel.AiMessengerUiState

class MainActivity : ComponentActivity() {
    private var requestedScreen by mutableStateOf(SCREEN_HOME)
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    // 로그인·네트워크 복구 시 uploadLocalFlowlogSnapshot이 동시에 여러 번 호출되는 것을 방지
    private val syncMutex = Mutex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedScreen = intent.getStringExtra(EXTRA_OPEN_SCREEN) ?: SCREEN_HOME
        ReminderScheduler(applicationContext).ensureNotificationChannel()
        FirebaseSyncAlarmScheduler.scheduleNextMidnightSync(applicationContext)
        lifecycleScope.launch {
            runCatching { AutoButtonScheduler(applicationContext).rescheduleAll() }
            runCatching { PlannedTodoReminderScheduler(applicationContext).rescheduleAll() }
        }
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
                val promotedButtons by activityViewModel.promotedButtons.collectAsState()
                val isNotificationSoundEnabled by activityViewModel.isNotificationSoundEnabled.collectAsState()
                val routineTimerCategories = remember(promotedButtons) {
                    val base = listOf("SLEEP", "REST", "WORK", "STUDY", "EXERCISE", "WASH", "MEAL", "ETC")
                    if (promotedButtons.isNotEmpty()) {
                        base.take(6) + promotedButtons.asReversed() + base.drop(6)
                    } else {
                        base
                    }.distinct()
                }
                LaunchedEffect(activityViewModel, todoViewModel) {
                    val notifier = ActivityTimerNotifier(this@MainActivity)
                    activityViewModel.dailyCueGoalReachedEvents.collect { event ->
                        todoViewModel.completeDailyCue(event.cueId)
                        notifier.showRoutineGoalAlert(event.title, event.category)
                    }
                }
                val auth = remember { FirebaseAuth.getInstance() }
                var signedInUser by remember { mutableStateOf(auth.currentUser) }
                var syncStatus by remember { mutableStateOf<String?>(null) }
                val scope = rememberCoroutineScope()
                val aiMessengerUiState by activityViewModel.aiMessengerUiState.collectAsState()

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
                    val user = signedInUser
                    if (user != null) {
                        runCatching {
                            // anonymous로 저장된 Room 데이터를 실제 uid로 교체
                            withContext(Dispatchers.IO) {
                                val db = FlowlogDatabase.getInstance(applicationContext)
                                db.activityDao().reassignAnonymousUser(user.uid)
                                db.autoButtonScheduleDao().reassignAnonymousUser(user.uid)
                                db.todoDao().reassignAnonymousUser(user.uid)
                                db.eventLogDao().reassignAnonymousUser(user.uid)
                            }
                        }
                        runCatching {
                            // 신규 설치·재설치 감지: 로컬에 activity/todo 데이터가 모두 없으면 Firebase에서 복원
                            val shouldRestore = withContext(Dispatchers.IO) {
                                val db = FlowlogDatabase.getInstance(applicationContext)
                                val activityCount = db.activityDao().getActiveActivitiesCount(user.uid)
                                val todoCount = db.todoDao().getActiveTodosCount(user.uid)
                                activityCount == 0 && todoCount == 0
                            }
                            if (shouldRestore) {
                                FirebaseRestoreDataSource(applicationContext).restoreFromFirestore(user.uid)
                            }
                        }
                        runCatching {
                            uploadLocalFlowlogSnapshot()
                        }
                        activityViewModel.handleLoginMainButtonSync()
                        runCatching {
                            AutoButtonScheduler(applicationContext).rescheduleAll()
                        }
                        runCatching {
                            PlannedTodoReminderScheduler(applicationContext).rescheduleAll()
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
                val runFirebaseUpload: () -> Unit = {
                    scope.launch {
                        val result = uploadAllPendingFlowlogSnapshot()
                        val message = when {
                            result?.deferred == true -> "진행 중인 Activity가 있어 업로드를 보류했습니다."
                            result == null -> "Firebase 업로드에 실패했습니다."
                            else -> "Firebase 업로드 완료: 성공 ${result.successCount}건, 실패 ${result.failureCount}건"
                        }
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
                val regenerateRecommendedTimePlan: () -> Unit = {
                    activityViewModel.regenerateTodayRecommendedTimePlan()
                    Toast.makeText(this@MainActivity, "오늘 추천 시간 계획을 다시 만들었습니다.", Toast.LENGTH_SHORT).show()
                }

                val restoreTodosLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    scope.launch {
                        val message = runCatching {
                            val jsonText = withContext(Dispatchers.IO) {
                                this@MainActivity.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                                    ?: error("복원 파일을 열 수 없습니다.")
                            }
                            val restoredCount = todoViewModel.restoreTodosFromBackup(jsonText)
                            "Todo ${restoredCount}개를 복원했습니다."
                        }.getOrElse { error ->
                            error.message ?: "Todo 복원에 실패했습니다."
                        }
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }

                val pagerState = rememberPagerState(
                    initialPage = when {
                        isDeveloperMode && currentScreen == "stats" -> 1
                        currentScreen == "todo" -> if (isDeveloperMode) 2 else 1
                        else -> 0
                    },
                    pageCount = { if (isDeveloperMode) 3 else 2 }
                )

                LaunchedEffect(pagerState.currentPage, isDeveloperMode) {
                    currentScreen = if (isDeveloperMode) {
                        when (pagerState.currentPage) { 1 -> "stats"; 2 -> "todo"; else -> "home" }
                    } else {
                        if (pagerState.currentPage == 0) "home" else "todo"
                    }
                }

                LaunchedEffect(currentScreen, isDeveloperMode) {
                    if (!isDeveloperMode && currentScreen == "stats") {
                        currentScreen = "home"
                        return@LaunchedEffect
                    }
                    val targetPage = if (isDeveloperMode) {
                        when (currentScreen) { "stats" -> 1; "todo" -> 2; else -> 0 }
                    } else {
                        if (currentScreen == "todo") 1 else 0
                    }
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
                            val todoPage = if (isDeveloperMode) 2 else 1
                            when {
                                page == todoPage -> TodoScreen(
                                    viewModel = todoViewModel,
                                    isDeveloperMode = isDeveloperMode,
                                    isAiOrganizerAllowed = isDeveloper,
                                    onStartTodo = { todo ->
                                        activityViewModel.startTodoActivity(todo.id, todo.title)
                                        currentScreen = "home"
                                    },
                                    onStartDailyCueRoutine = { cueId, title, goalMillis, category ->
                                        activityViewModel.startDailyCueRoutineActivity(cueId, title, goalMillis, category)
                                        currentScreen = "home"
                                    },
                                    onStartExamStudy = { todoId, subjectTitle, dValue ->
                                        activityViewModel.startExamStudyActivity(todoId, subjectTitle, dValue)
                                        currentScreen = "home"
                                    },
                                    routineTimerCategories = routineTimerCategories,
                                    modifier = Modifier.fillMaxSize()
                                )
                                isDeveloperMode && page == 1 -> DevTimetableScreen(
                                    modifier = Modifier.fillMaxSize()
                                )
                                else -> HomeScreen(
                                    viewModel = activityViewModel,
                                    isDeveloperMode = isDeveloperMode,
                                    topActions = {
                                        HeaderActions(
                                            isSignedIn = signedInUser != null,
                                            syncStatus = syncStatus,
                                            profilePhotoUrl = signedInUser?.photoUrl?.toString(),
                                            onStatsClick = openStatsSite,
                                            onBlogClick = openDeveloperBlog,
                                            onAccountClick = runAccountSync,
                                            onAiMessengerClick = {
                                                activityViewModel.openAiMessenger()
                                            },
                                            hasUnreadAiMessages = aiMessengerUiState.hasUnread,
                                            isDeveloper = isDeveloper,
                                            isDeveloperMode = isDeveloperMode,
                                            onFirebaseUploadClick = runFirebaseUpload,
                                            onRegenerateRecommendedTimePlanClick = regenerateRecommendedTimePlan,
                                            onRestoreTodosClick = {
                                                restoreTodosLauncher.launch(arrayOf("application/json", "text/plain", "text/json"))
                                            },
                                            onToggleDevMode = {
                                                val newMode = !isDeveloperMode
                                                isDeveloperMode = newMode
                                                userRoleStore.setDeveloperMode(newMode)
                                            },
                                            isNotificationSoundEnabled = isNotificationSoundEnabled,
                                            onToggleNotificationSound = {
                                                activityViewModel.toggleNotificationSound()
                                            }
                                        )
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        FlowlogBottomBar(
                            currentScreen = currentScreen,
                            isDeveloperMode = isDeveloperMode,
                            onHomeClick = { currentScreen = "home" },
                            onStatsClick = { currentScreen = "stats" },
                            onTodoClick = { currentScreen = "todo" }
                        )
                    }
                if (aiMessengerUiState.showSheet) {
                    AiMessengerSheet(
                        uiState = aiMessengerUiState,
                        onAccept = { id -> activityViewModel.acceptMainButtonRecommendation(id) },
                        onDismiss = { id -> activityViewModel.dismissMainButtonRecommendation(id) },
                        onClose = { activityViewModel.closeAiMessenger() },
                        isDeveloperMode = isDeveloperMode,
                        onDebugInject = { activityViewModel.debugInjectMainButtonRecommendation() },
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

    private suspend fun uploadLocalFlowlogSnapshot(): com.example.flowlog.data.sync.SyncOutcome? {
        if (!syncMutex.tryLock()) return null
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                return FirebaseSyncCoordinator(applicationContext).syncEligible(uid)
            }
            return null
        } finally {
            syncMutex.unlock()
        }
    }

    private suspend fun uploadAllPendingFlowlogSnapshot(): com.example.flowlog.data.sync.SyncOutcome? {
        if (!syncMutex.tryLock()) return null
        try {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                return FirebaseSyncCoordinator(applicationContext).syncAll(uid)
            }
            return null
        } finally {
            syncMutex.unlock()
        }
    }

    private fun registerNetworkSync() {
        if (networkCallback != null) return

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
                lifecycleScope.launch {
                    runCatching { FirebaseSyncCoordinator(applicationContext).syncEligible(uid) }
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
    onAiMessengerClick: () -> Unit = {},
    hasUnreadAiMessages: Boolean = false,
    isDeveloper: Boolean = false,
    isDeveloperMode: Boolean = false,
    onFirebaseUploadClick: () -> Unit = {},
    onRegenerateRecommendedTimePlanClick: () -> Unit = {},
    onRestoreTodosClick: () -> Unit = {},
    onToggleDevMode: () -> Unit = {},
    isNotificationSoundEnabled: Boolean = true,
    onToggleNotificationSound: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

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

        IconButton(
            onClick = onAiMessengerClick,
            modifier = Modifier.size(52.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF0ECFF))
                    .border(1.dp, Color(0xFFE0D7FF), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "AI 메신저",
                    tint = Color(0xFF5140D8),
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                )
                if (hasUnreadAiMessages) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                    )
                }
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
                onDismissRequest = { menuExpanded = false },
                containerColor = Color.White
            ) {
                DropdownMenuItem(
                    text = { Text(accountActionLabel(isSignedIn, syncStatus), color = Color(0xFF10182C)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            tint = Color(0xFF5140D8)
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onAccountClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("설정", color = Color(0xFF10182C)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                            tint = Color(0xFF5140D8)
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        showSettingsDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("개발자 블로그", color = Color(0xFF10182C)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Article,
                            contentDescription = null,
                            tint = Color(0xFF5140D8)
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onBlogClick()
                    }
                )
                if (isDeveloperMode) {
                    DropdownMenuItem(
                        text = { Text("Firebase 업로드", color = Color(0xFF10182C)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.CheckBox,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onFirebaseUploadClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("오늘 추천 시간 재생성", color = Color(0xFF10182C)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onRegenerateRecommendedTimePlanClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Todo 복원", color = Color(0xFF10182C)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.CheckBox,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onRestoreTodosClick()
                        }
                    )
                }
                if (isDeveloper) {
                    DropdownMenuItem(
                        text = { Text("개발자 모드", color = Color(0xFF10182C)) },
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

    if (showSettingsDialog) {
        var hasDndAccess by remember { mutableStateOf(FocusDndController.hasPolicyAccess(context)) }
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "설정",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFF10182C)
                )
            },
            text = {
                Column {
                    Text(
                        text = "알림 설정",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5140D8)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isNotificationSoundEnabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                            contentDescription = null,
                            tint = if (isNotificationSoundEnabled) Color(0xFF5140D8) else Color(0xFF9E9E9E),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "알림 소리",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10182C)
                            )
                            Text(
                                text = if (isNotificationSoundEnabled) "켜짐" else "꺼짐",
                                fontSize = 12.sp,
                                color = Color(0xFF9E9E9E)
                            )
                        }
                        Switch(
                            checked = isNotificationSoundEnabled,
                            onCheckedChange = { onToggleNotificationSound() }
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "집중 모드",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5140D8)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DoNotDisturb,
                            contentDescription = null,
                            tint = if (hasDndAccess) Color(0xFF5140D8) else Color(0xFF9E9E9E),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "방해금지 액세스 권한",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10182C)
                            )
                            Text(
                                text = if (hasDndAccess) "허용됨 · 시스템 설정에서 변경 가능" else "집중 모드 시 방해금지를 함께 켤 수 있어요",
                                fontSize = 12.sp,
                                color = Color(0xFF9E9E9E)
                            )
                        }
                        Switch(
                            checked = hasDndAccess,
                            onCheckedChange = {
                                // 특수 권한은 시스템 설정에서만 변경 가능
                                hasDndAccess = FocusDndController.hasPolicyAccess(context)
                                FocusDndController.openPolicyAccessSettings(context)
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSettingsDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5140D8),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("닫기", fontWeight = FontWeight.ExtraBold)
                }
            }
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiMessengerSheet(
    uiState: AiMessengerUiState,
    onAccept: (messageId: String) -> Unit,
    onDismiss: (messageId: String) -> Unit,
    onClose: () -> Unit,
    isDeveloperMode: Boolean = false,
    onDebugInject: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pendingRecommendations = uiState.messages
        .filterIsInstance<AiMessage.MainButtonRecommendation>()
        .filter { it.status == RecommendationStatus.PENDING }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF0ECFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF5140D8),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Flowlog AI",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF10182C)
                    )
                    Text(
                        text = "활동 패턴 기반 제안",
                        fontSize = 12.sp,
                        color = Color(0xFF697386)
                    )
                }
            }

            if (pendingRecommendations.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "아직 제안할 내용이 없어요.\n기록이 조금 더 쌓이면 Flowlog가 도와줄게요.",
                            fontSize = 14.sp,
                            color = Color(0xFF697386),
                            textAlign = TextAlign.Center
                        )
                        if (isDeveloperMode) {
                            TextButton(onClick = onDebugInject) {
                                Text("[DEBUG] 테스트 추천 삽입", fontSize = 12.sp, color = Color(0xFFAAAAAA))
                            }
                        }
                    }
                }
            } else {
                pendingRecommendations.forEach { recommendation ->
                    AiSuggestionCard(
                        recommendation = recommendation,
                        onAccept = { onAccept(recommendation.id) },
                        onDismiss = { onDismiss(recommendation.id) },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AiSuggestionCard(
    recommendation: AiMessage.MainButtonRecommendation,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val name = displayCategory(recommendation.category)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFE8E8EE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "최근 $name 기록이 자주 보여요. $name 버튼을 메인에 추가해볼까요?",
                fontSize = 14.sp,
                color = Color(0xFF10182C)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE8E8EE)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF697386))
                ) {
                    Text("나중에", fontSize = 13.sp)
                }
                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5140D8),
                        contentColor = Color.White
                    )
                ) {
                    Text("추가하기", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FlowlogBottomBar(
    currentScreen: String,
    isDeveloperMode: Boolean = false,
    onHomeClick: () -> Unit,
    onStatsClick: () -> Unit = {},
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
        if (isDeveloperMode) {
            NavigationBarItem(
                selected = currentScreen == "stats",
                onClick = onStatsClick,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.BarChart,
                        contentDescription = "통계"
                    )
                },
                label = { Text("통계") }
            )
        }
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
