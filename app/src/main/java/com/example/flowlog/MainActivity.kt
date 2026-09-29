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
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.res.stringResource
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
import com.example.flowlog.data.sync.FirebaseCalendarPullDataSource
import com.example.flowlog.data.sync.FirebaseRestoreDataSource
import com.example.flowlog.data.sync.FirebaseSyncAlarmScheduler
import com.example.flowlog.data.sync.FirebaseSyncCoordinator
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.example.flowlog.notification.ReminderScheduler
import com.example.flowlog.notification.AutoButtonScheduler
import com.example.flowlog.notification.PlannedTodoReminderScheduler
import com.example.flowlog.notification.StudyPlanAutoStartScheduler
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Calendar
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
import com.example.flowlog.data.model.MainButtonConfig
import com.example.flowlog.data.model.RecommendationStatus
import com.example.flowlog.ui.component.displayCategory
import com.example.flowlog.ui.viewmodel.AiMessengerUiState

class MainActivity : ComponentActivity() {
    private var requestedScreen by mutableStateOf(SCREEN_HOME)
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var calendarDataSource: FirebaseCalendarPullDataSource? = null
    private var calendarSubscription: FirebaseCalendarPullDataSource.CalendarSubscription? = null
    private var calendarSubscriptionUserId: String? = null
    private var calendarRolloverJob: Job? = null
    // 로그인·네트워크 복구 시 uploadLocalFlowlogSnapshot이 동시에 여러 번 호출되는 것을 방지
    private val syncMutex = Mutex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
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
                val activityUiState by activityViewModel.uiState.collectAsState()
                LaunchedEffect(activityViewModel) {
                    activityViewModel.attachStudyDao(FlowlogDatabase.getInstance(applicationContext).studyDao())
                }
                val isFocusFireActive = activityUiState.isRunning && activityUiState.isFocusModeActive
                val promotedButtons by activityViewModel.promotedButtons.collectAsState()
                val isNotificationSoundEnabled by activityViewModel.isNotificationSoundEnabled.collectAsState()
                val isInactivityReminderEnabled by activityViewModel.isInactivityReminderEnabled.collectAsState()
                val isTimetableAutoPlaceEnabled by activityViewModel.isTimetableAutoPlaceEnabled.collectAsState()
                val isTimetableReminderEnabled by activityViewModel.isTimetableReminderEnabled.collectAsState()
                var homeMainTimerScrollRequest by remember { mutableStateOf(0) }
                fun showHomeMainTimer() {
                    currentScreen = "home"
                    homeMainTimerScrollRequest += 1
                }
                val routineTimerCategories = remember(promotedButtons) {
                    val base = MainButtonConfig.ALL_SELECTABLE_CATEGORIES
                    if (promotedButtons.isNotEmpty()) {
                        base.take(6) + promotedButtons.asReversed() + base.drop(6)
                    } else {
                        base
                    }.distinct()
                }
                LaunchedEffect(activityViewModel, todoViewModel) {
                    launch {
                        activityViewModel.dailyCueGoalReachedEvents.collect { event ->
                            todoViewModel.completeDailyCue(event.cueId)
                        }
                    }
                    activityViewModel.recommendedTodoCompletionEvents.collect { event ->
                        val block = event.block
                        if (block.petiteId != null) {
                            todoViewModel.completePetiteById(block.petiteId)
                        } else {
                            todoViewModel.dismissPetiteLinkedToTodo(block.todoId)
                        }
                    }
                }
                val auth = remember { FirebaseAuth.getInstance() }
                var signedInUser by remember { mutableStateOf(auth.currentUser) }
                val conflictDao = remember { FlowlogDatabase.getInstance(applicationContext).activityRevisionDao() }
                val conflictScope = rememberCoroutineScope()
                var syncConflicts by remember(signedInUser?.uid) { mutableStateOf(emptyList<com.example.flowlog.data.local.entity.ActivityConflictEntity>()) }
                var dismissedConflicts by remember(signedInUser?.uid) { mutableStateOf(emptySet<String>()) }
                var resolvingConflict by remember { mutableStateOf(false) }
                var conflictError by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(signedInUser?.uid) {
                    val owner = signedInUser?.uid ?: return@LaunchedEffect
                    conflictDao.observeConflicts(owner).collect { syncConflicts = it }
                }
                val conflict = syncConflicts.firstOrNull { it.activityId !in dismissedConflicts }
                if (conflict != null) {
                    fun resolveConflict(useRemote: Boolean) {
                        if (resolvingConflict || auth.currentUser?.uid != conflict.userId) return
                        resolvingConflict = true
                        conflictError = null
                        conflictScope.launch {
                            runCatching {
                                if (useRemote) {
                                    val result = FirebaseRestoreDataSource(applicationContext).restoreActivities(
                                        conflict.userId, conflict.activityId, conflict.remoteRevision, conflict.localUpdatedAt
                                    )
                                    check(result.inserted == 1 && result.failed == 0) { "기록이 다시 변경되었거나 불러오지 못했습니다. 동기화 후 다시 선택해 주세요." }
                                } else {
                                    check(conflictDao.rebaseLocal(conflict.userId, conflict.activityId, conflict.localUpdatedAt, conflict.remoteRevision) == 1) {
                                        "앱 기록이 다시 변경되었습니다. 동기화 후 다시 선택해 주세요."
                                    }
                                    conflictDao.clearConflict(conflict.userId, conflict.activityId)
                                    FirebaseSyncCoordinator(applicationContext).syncAll(conflict.userId)
                                }
                            }.onFailure { conflictError = it.message }
                            resolvingConflict = false
                        }
                    }
                    AlertDialog(
                        onDismissRequest = { if (!resolvingConflict) dismissedConflicts = dismissedConflicts + conflict.activityId },
                        title = { Text("기록 수정 충돌") },
                        text = { Text("앱 기록\n${conflict.localDescription}\n\n웹 기록\n${conflict.remoteDescription}\n\n${conflict.reason}\n\n수업 연결 구간 변경은 웹의 기록 상세에서 조정할 수 있어요." + (conflictError?.let { "\n\n$it" } ?: "")) },
                        confirmButton = { TextButton(enabled = !resolvingConflict, onClick = { resolveConflict(false) }) { Text("앱 기록 유지") } },
                        dismissButton = { TextButton(enabled = !resolvingConflict, onClick = { resolveConflict(true) }) { Text("웹 기록 사용") } }
                    )
                }
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
                        startCalendarSubscription(user.uid)
                    } else {
                        stopCalendarSubscription()
                    }
                }
                val runAccountSync: () -> Unit = {
                    scope.launch {
                        if (signedInUser != null) {
                            auth.signOut()
                            syncStatus = null
                            Toast.makeText(this@MainActivity, getString(R.string.main_activity_logged_out), Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        syncStatus = "Signing in..."
                        Toast.makeText(this@MainActivity, getString(R.string.main_activity_google_signin_start), Toast.LENGTH_SHORT).show()
                        val signInResult = runCatching {
                            signInWithGoogle()
                            uploadLocalFlowlogSnapshot()
                        }
                        syncStatus = signInResult.fold(
                            onSuccess = {
                                Toast.makeText(this@MainActivity, getString(R.string.main_activity_logged_in), Toast.LENGTH_SHORT).show()
                                null
                            },
                            onFailure = { error ->
                                val message = error.localizedMessage ?: "Sign-in failed"
                                Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                                message
                            }
                        )
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
                            result?.deferred == true -> getString(R.string.main_activity_sync_deferred)
                            result == null -> getString(R.string.main_activity_sync_failed)
                            else -> buildFirebaseSyncMessage(result)
                        }
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
                val regenerateRecommendedTimePlan: () -> Unit = {
                    todoViewModel.refreshSort()
                    Toast.makeText(this@MainActivity, getString(R.string.main_activity_recommended_plan_regenerated), Toast.LENGTH_SHORT).show()
                }

                val restoreTodosLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri == null) return@rememberLauncherForActivityResult
                    scope.launch {
                        val message = runCatching {
                            val jsonText = withContext(Dispatchers.IO) {
                                this@MainActivity.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                                    ?: error(getString(R.string.main_activity_restore_file_open_failed))
                            }
                            val restoredCount = todoViewModel.restoreTodosFromBackup(jsonText)
                            getString(R.string.main_activity_todo_restored_count, restoredCount)
                        }.getOrElse { error ->
                            error.message ?: getString(R.string.main_activity_todo_restore_failed)
                        }
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }

                val pagerState = rememberPagerState(
                    initialPage = when {
                        currentScreen == "todo" -> 1
                        else -> 0
                    },
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
                    containerColor = if (isFocusFireActive && currentScreen == "home") {
                        Color(0xFFFFF0E6)
                    } else {
                        Color(0xFFF8F8F9)
                    },
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
                            when {
                                page == 1 -> TodoScreen(
                                    viewModel = todoViewModel,
                                    isDeveloperMode = isDeveloperMode,
                                    onStartTodo = { todo ->
                                        activityViewModel.startTodoActivity(todo.id, todo.title, todo.calendarSourceId)
                                        showHomeMainTimer()
                                    },
                                    onStartDailyCueRoutine = { cueId, title, goalMillis, category ->
                                        activityViewModel.startDailyCueRoutineActivity(cueId, title, goalMillis, category)
                                        showHomeMainTimer()
                                    },
                                    onStartYesterdayRoutine = { cueId, title, goalMillis, category ->
                                        activityViewModel.startDailyCueRoutineActivity(
                                            cueId, title, goalMillis, category,
                                            targetDateKey = todoViewModel.yesterdayDateKey()
                                        )
                                        showHomeMainTimer()
                                    },
                                    onStartExamStudy = { todoId, subjectTitle, dValue ->
                                        activityViewModel.startExamStudyActivity(todoId, subjectTitle, dValue)
                                        showHomeMainTimer()
                                    },
                                    onStartCalendarPetite = { item ->
                                        activityViewModel.startCalendarPetiteActivity(item)
                                        showHomeMainTimer()
                                    },
                                    routineTimerCategories = routineTimerCategories,
                                    modifier = Modifier.fillMaxSize()
                                )
                                else -> HomeScreen(
                                    viewModel = activityViewModel,
                                    isDeveloperMode = isDeveloperMode,
                                    topActions = {
                                        HeaderActions(
                                            isFocusFireActive = isFocusFireActive,
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
                                            },
                                            isInactivityReminderEnabled = isInactivityReminderEnabled,
                                            onToggleInactivityReminder = {
                                                activityViewModel.toggleInactivityReminder()
                                            },
                                            isTimetableAutoPlaceEnabled = isTimetableAutoPlaceEnabled,
                                            onToggleTimetableAutoPlace = {
                                                activityViewModel.toggleTimetableAutoPlace()
                                            },
                                            isTimetableReminderEnabled = isTimetableReminderEnabled,
                                            onToggleTimetableReminder = {
                                                activityViewModel.toggleTimetableReminder()
                                            }
                                        )
                                    },
                                    scrollToMainTimerRequest = homeMainTimerScrollRequest,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        FlowlogBottomBar(
                            currentScreen = currentScreen,
                            isFocusFireActive = isFocusFireActive && currentScreen == "home",
                            onHomeClick = { currentScreen = "home" },
                            onTodoClick = { currentScreen = "todo" }
                        )
                    }
                if (aiMessengerUiState.showSheet) {
                    AiMessengerSheet(
                        uiState = aiMessengerUiState,
                        onAccept = { id -> activityViewModel.acceptMainButtonRecommendation(id) },
                        onDismiss = { id -> activityViewModel.dismissMainButtonRecommendation(id) },
                        onClose = { activityViewModel.closeAiMessenger() },
                        mainButtons = activityUiState.mainButtonConfig.buttons,
                        existingButtonCallbacks = ExistingButtonCardCallbacks(
                            onShown = activityViewModel::onExistingButtonRecommendationShown,
                            onUse = { id -> activityViewModel.useExistingButtonRecommendation(id, activityViewModel::startActivity) },
                            onAdd = activityViewModel::addExistingButtonRecommendation,
                            onReplace = activityViewModel::replaceWithExistingButtonRecommendation,
                            onDismiss = activityViewModel::dismissExistingButtonRecommendation,
                            onDisable = activityViewModel::disableExistingButtonRecommendation
                        ),
                    )
                }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        registerNetworkSync()
        FirebaseAuth.getInstance().currentUser?.uid?.let(::startCalendarSubscription)
    }

    override fun onStop() {
        stopCalendarSubscription()
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
                return FirebaseSyncCoordinator(applicationContext).syncAllWithTodayCalendar(uid)
            }
            return null
        } finally {
            syncMutex.unlock()
        }
    }

    private fun startCalendarSubscription(userId: String) {
        if (calendarSubscription != null && calendarSubscriptionUserId == userId) return
        stopCalendarSubscription()

        val dataSource = FirebaseCalendarPullDataSource(applicationContext)
        calendarDataSource = dataSource
        calendarSubscriptionUserId = userId
        calendarSubscription = dataSource.listenTodayCalendar(userId)

        calendarRolloverJob = lifecycleScope.launch {
            val now = System.currentTimeMillis()
            val nextMidnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            delay((nextMidnight - now).coerceAtLeast(1L))
            if (FirebaseAuth.getInstance().currentUser?.uid == userId) {
                calendarSubscription?.remove()
                calendarSubscription = null
                calendarSubscriptionUserId = null
                startCalendarSubscription(userId)
            }
        }
    }

    private fun stopCalendarSubscription() {
        calendarRolloverJob?.cancel()
        calendarRolloverJob = null
        calendarSubscription?.remove()
        calendarSubscription = null
        calendarSubscriptionUserId = null
        calendarDataSource = null
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

    private fun buildFirebaseSyncMessage(result: com.example.flowlog.data.sync.SyncOutcome): String {
        val uploadPart = getString(R.string.main_activity_upload_result, result.successCount, result.failureCount)
        val pull = result.calendarPull
        val calendarPart = when {
            pull == null -> null
            pull.failed -> getString(R.string.main_activity_calendar_pull_failed)
            pull.pulledCalendarTodoCount + pull.pulledLectureInfoCount + pull.pulledGeneralEventCount == 0 -> null
            else -> buildString {
                if (pull.pulledCalendarTodoCount > 0) append(getString(R.string.main_activity_calendar_todo_count, pull.pulledCalendarTodoCount))
                if (pull.pulledLectureInfoCount > 0) {
                    if (isNotEmpty()) append(", ")
                    append(getString(R.string.main_activity_calendar_lecture_count, pull.pulledLectureInfoCount))
                }
                if (pull.pulledGeneralEventCount > 0) {
                    if (isNotEmpty()) append(", ")
                    append(getString(R.string.main_activity_calendar_event_count, pull.pulledGeneralEventCount))
                }
            }
        }
        return if (calendarPart != null) {
            getString(R.string.main_activity_sync_complete_with_calendar, uploadPart, calendarPart)
        } else {
            getString(R.string.main_activity_sync_complete, uploadPart)
        }
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
    isFocusFireActive: Boolean = false,
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
    onToggleNotificationSound: () -> Unit = {},
    isInactivityReminderEnabled: Boolean = true,
    onToggleInactivityReminder: () -> Unit = {},
    isTimetableAutoPlaceEnabled: Boolean = true,
    onToggleTimetableAutoPlace: () -> Unit = {},
    isTimetableReminderEnabled: Boolean = true,
    onToggleTimetableReminder: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val signingInLabel = stringResource(R.string.main_activity_signing_in_status)
    val logoutLabel = stringResource(R.string.main_activity_logout_label)
    val loginLabel = stringResource(R.string.main_activity_login_label)

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onStatsClick,
            modifier = Modifier.size(52.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isFocusFireActive) Color(0xFFFFE8D8) else Color(0xFFF0ECFF))
                    .border(
                        1.dp,
                        if (isFocusFireActive) Color(0xFFFFB17A) else Color(0xFFE0D7FF),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = stringResource(R.string.main_activity_stats_content_desc),
                    tint = if (isFocusFireActive) Color(0xFFFF7A2F) else Color(0xFF5140D8),
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
                    .background(if (isFocusFireActive) Color(0xFFFFE8D8) else Color(0xFFF0ECFF))
                    .border(
                        1.dp,
                        if (isFocusFireActive) Color(0xFFFFB17A) else Color(0xFFE0D7FF),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = stringResource(R.string.main_activity_ai_messenger_content_desc),
                    tint = if (isFocusFireActive) Color(0xFFFF7A2F) else Color(0xFF5140D8),
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
            if (isSignedIn) {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(52.dp)
                ) {
                    ProfileAvatar(
                        profilePhotoUrl = profilePhotoUrl,
                        isSignedIn = true
                    )
                }
            } else {
                Button(
                    onClick = onAccountClick,
                    modifier = Modifier.height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5140D8),
                        contentColor = Color.White
                    )
                ) {
                    Text(accountActionLabel(isSignedIn, syncStatus, signingInLabel, logoutLabel, loginLabel))
                }
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                containerColor = Color.White
            ) {
                DropdownMenuItem(
                    text = { Text(accountActionLabel(isSignedIn, syncStatus, signingInLabel, logoutLabel, loginLabel), color = Color(0xFF10182C)) },
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
                    text = { Text(stringResource(R.string.main_activity_settings), color = Color(0xFF10182C)) },
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
                    text = { Text(stringResource(R.string.main_activity_developer_blog), color = Color(0xFF10182C)) },
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
                        text = { Text(stringResource(R.string.main_activity_firebase_sync), color = Color(0xFF10182C)) },
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
                        text = { Text(stringResource(R.string.main_activity_regenerate_recommended_plan_menu), color = Color(0xFF10182C)) },
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
                        text = { Text(stringResource(R.string.main_activity_restore_todo_menu), color = Color(0xFF10182C)) },
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
                        text = { Text(stringResource(R.string.main_activity_developer_mode), color = Color(0xFF10182C)) },
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
                    text = stringResource(R.string.main_activity_settings),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFF10182C)
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.main_activity_notification_settings),
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
                                text = stringResource(R.string.main_activity_notification_sound),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10182C)
                            )
                            Text(
                                text = if (isNotificationSoundEnabled) stringResource(R.string.main_activity_on) else stringResource(R.string.main_activity_off),
                                fontSize = 12.sp,
                                color = Color(0xFF9E9E9E)
                            )
                        }
                        Switch(
                            checked = isNotificationSoundEnabled,
                            onCheckedChange = { onToggleNotificationSound() }
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isInactivityReminderEnabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                            contentDescription = null,
                            tint = if (isInactivityReminderEnabled) Color(0xFF5140D8) else Color(0xFF9E9E9E),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.main_activity_inactivity_reminder),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10182C)
                            )
                            Text(
                                text = if (isInactivityReminderEnabled) stringResource(R.string.main_activity_inactivity_reminder_desc) else stringResource(R.string.main_activity_off),
                                fontSize = 12.sp,
                                color = Color(0xFF9E9E9E)
                            )
                        }
                        Switch(
                            checked = isInactivityReminderEnabled,
                            onCheckedChange = { onToggleInactivityReminder() }
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.main_activity_timetable_plan_settings),
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
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = if (isTimetableAutoPlaceEnabled) Color(0xFF5140D8) else Color(0xFF9E9E9E),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.main_activity_timetable_auto_place),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10182C)
                            )
                            Text(
                                text = if (isTimetableAutoPlaceEnabled) stringResource(R.string.main_activity_timetable_auto_place_desc) else stringResource(R.string.main_activity_off),
                                fontSize = 12.sp,
                                color = Color(0xFF9E9E9E)
                            )
                        }
                        Switch(
                            checked = isTimetableAutoPlaceEnabled,
                            onCheckedChange = { onToggleTimetableAutoPlace() }
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (isTimetableReminderEnabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                            contentDescription = null,
                            tint = if (isTimetableReminderEnabled) Color(0xFF5140D8) else Color(0xFF9E9E9E),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.main_activity_timetable_reminder),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10182C)
                            )
                            Text(
                                text = if (isTimetableReminderEnabled) stringResource(R.string.main_activity_timetable_reminder_desc) else stringResource(R.string.main_activity_off),
                                fontSize = 12.sp,
                                color = Color(0xFF9E9E9E)
                            )
                        }
                        Switch(
                            checked = isTimetableReminderEnabled,
                            onCheckedChange = { onToggleTimetableReminder() }
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.main_activity_focus_mode),
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
                                text = stringResource(R.string.main_activity_dnd_permission),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10182C)
                            )
                            Text(
                                text = if (hasDndAccess) stringResource(R.string.main_activity_dnd_granted) else stringResource(R.string.main_activity_dnd_desc),
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
                    Text(stringResource(R.string.main_activity_close), fontWeight = FontWeight.ExtraBold)
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
                contentDescription = stringResource(R.string.main_activity_google_profile_content_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = stringResource(R.string.main_activity_default_profile_content_desc),
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
    mainButtons: List<com.example.flowlog.data.model.MainButtonItem> = emptyList(),
    existingButtonCallbacks: ExistingButtonCardCallbacks = ExistingButtonCardCallbacks(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pendingRecommendations = uiState.messages
        .filterIsInstance<AiMessage.MainButtonRecommendation>()
        .filter { it.status == RecommendationStatus.PENDING }
    val pendingExistingButtons = uiState.messages
        .filterIsInstance<AiMessage.ExistingButtonRecommendation>()
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
                        text = stringResource(R.string.main_activity_pattern_suggestion_title),
                        fontSize = 12.sp,
                        color = Color(0xFF697386)
                    )
                }
            }

            pendingExistingButtons.forEach { recommendation ->
                ExistingButtonSuggestionCard(
                    recommendation = recommendation,
                    mainButtons = mainButtons,
                    callbacks = existingButtonCallbacks,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            if (pendingRecommendations.isEmpty() && pendingExistingButtons.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.main_activity_no_suggestion_yet),
                            fontSize = 14.sp,
                            color = Color(0xFF697386),
                            textAlign = TextAlign.Center
                        )
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

private data class ExistingButtonCardCallbacks(
    val onShown: (messageId: String) -> Unit = {},
    val onUse: (messageId: String) -> Unit = {},
    val onAdd: (messageId: String) -> Unit = {},
    val onReplace: (messageId: String, oldCategory: String) -> Unit = { _, _ -> },
    val onDismiss: (messageId: String) -> Unit = {},
    val onDisable: (messageId: String) -> Unit = {}
)

@Composable
private fun ExistingButtonSuggestionCard(
    recommendation: AiMessage.ExistingButtonRecommendation,
    mainButtons: List<com.example.flowlog.data.model.MainButtonItem>,
    callbacks: ExistingButtonCardCallbacks,
    modifier: Modifier = Modifier
) {
    // 실제로 렌더링된 카드만 노출(SHOWN)로 기록
    androidx.compose.runtime.LaunchedEffect(recommendation.id) {
        callbacks.onShown(recommendation.id)
    }
    val selectedOld = androidx.compose.runtime.remember(recommendation.id) {
        androidx.compose.runtime.mutableStateOf<String?>(null)
    }
    val name = displayCategory(recommendation.category)
    val isOnMain = mainButtons.any { it.category == recommendation.category }
    val needsReplacement = !isOnMain &&
        mainButtons.size >= com.example.flowlog.data.model.MainButtonConfig.MAX_BUTTONS
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFE8E8EE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "최근 4주 중 ${recommendation.evidenceDays}일, 이 요일·시간대에 '$name'(으)로 기록했어요.",
                fontSize = 14.sp,
                color = Color(0xFF10182C)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { callbacks.onDismiss(recommendation.id) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE8E8EE)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF697386))
                ) {
                    Text(stringResource(R.string.main_activity_later), fontSize = 13.sp)
                }
                Button(
                    onClick = { callbacks.onUse(recommendation.id) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5140D8),
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isOnMain) "지금 시작" else "이번만 시작", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (!isOnMain && !needsReplacement) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { callbacks.onAdd(recommendation.id) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF5140D8)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF5140D8))
                ) {
                    Text(stringResource(R.string.main_activity_add_button), fontSize = 13.sp)
                }
            }
            if (needsReplacement) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "메인 버튼이 10개예요. 교체할 버튼을 직접 선택하세요.",
                    fontSize = 12.sp,
                    color = Color(0xFF697386)
                )
                Spacer(modifier = Modifier.height(8.dp))
                mainButtons.filter { !it.isPinned }.sortedBy { it.order }.forEach { button ->
                    val isSelected = selectedOld.value == button.category
                    OutlinedButton(
                        onClick = { selectedOld.value = if (isSelected) null else button.category },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF5140D8) else Color(0xFFE8E8EE)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isSelected) Color(0xFF5140D8) else Color(0xFF697386)
                        )
                    ) {
                        Text(displayCategory(button.category), fontSize = 13.sp)
                    }
                }
                Button(
                    onClick = { selectedOld.value?.let { callbacks.onReplace(recommendation.id, it) } },
                    enabled = selectedOld.value != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5140D8),
                        contentColor = Color.White
                    )
                ) {
                    Text("선택한 버튼을 '$name'(으)로 교체", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { callbacks.onDisable(recommendation.id) },
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFFE8E8EE)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF697386))
            ) {
                Text("이 추천 받지 않기", fontSize = 12.sp)
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
                text = stringResource(R.string.main_activity_category_suggestion, name),
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
                    Text(stringResource(R.string.main_activity_later), fontSize = 13.sp)
                }
                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5140D8),
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.main_activity_add_button), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FlowlogBottomBar(
    currentScreen: String,
    isFocusFireActive: Boolean = false,
    onHomeClick: () -> Unit,
    onTodoClick: () -> Unit
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    NavigationBar(
        modifier = Modifier
            .padding(bottom = bottomInset)
            .height(68.dp),
        containerColor = if (isFocusFireActive) Color(0xFFFFF5EE) else Color.White,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)
    ) {
        NavigationBarItem(
            selected = currentScreen == "home",
            onClick = onHomeClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = if (isFocusFireActive) Color(0xFFFF7A2F) else Color(0xFF5140D8),
                selectedTextColor = if (isFocusFireActive) Color(0xFFFF7A2F) else Color(0xFF5140D8),
                indicatorColor = if (isFocusFireActive) Color(0xFFFFE8D8) else Color(0xFFEDE9FF)
            ),
            icon = {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = stringResource(R.string.main_activity_home_content_desc)
                )
            },
            label = { Text(stringResource(R.string.main_activity_home_content_desc)) }
        )
        NavigationBarItem(
            selected = currentScreen == "todo",
            onClick = onTodoClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF5140D8),
                selectedTextColor = Color(0xFF5140D8),
                indicatorColor = Color(0xFFEDE9FF)
            ),
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

private fun accountActionLabel(
    isSignedIn: Boolean,
    syncStatus: String?,
    signingInLabel: String,
    logoutLabel: String,
    loginLabel: String
): String {
    return when (syncStatus) {
        "Signing in..." -> signingInLabel
        null -> if (isSignedIn) logoutLabel else loginLabel
        else -> if (isSignedIn) logoutLabel else loginLabel
    }
}
