package com.example.flowlog

import android.Manifest
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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.flowlog.data.account.AccountDeletionPolicy
import com.example.flowlog.data.account.AccountDeletionRemote
import com.example.flowlog.data.account.AccountDeletionState
import com.example.flowlog.data.account.DeviceDataWiper
import com.google.firebase.auth.FirebaseUser
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import com.example.flowlog.appcheck.FlowlogAppCheck
import com.example.flowlog.data.sync.RemoteTimerSync
import com.google.firebase.firestore.ListenerRegistration
import com.example.flowlog.notification.ExactAlarmPermission
import com.example.flowlog.notification.ExactAlarmStatus
import com.example.flowlog.notification.FocusDndController
import com.example.flowlog.data.model.AiMessage
import com.example.flowlog.data.model.MainButtonConfig
import com.example.flowlog.data.model.RecommendationStatus
import com.example.flowlog.ui.component.displayCategory
import com.example.flowlog.shortcut.TimerShortcutPublisher
import com.example.flowlog.shortcut.TimerShortcutRules
import com.example.flowlog.ui.viewmodel.ActivityExportFormat
import com.example.flowlog.ui.viewmodel.ActivityExportFormatter
import com.example.flowlog.ui.viewmodel.ActivityImportResult
import com.example.flowlog.ui.viewmodel.ActivityImportSummary
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.FileDownload
import java.util.TimeZone
import com.example.flowlog.ui.viewmodel.AiMessengerUiState

class MainActivity : ComponentActivity() {
    private var requestedScreen by mutableStateOf(SCREEN_HOME)
    // Category from a home-screen "start timer" shortcut, consumed once by the UI.
    private var pendingShortcutCategory by mutableStateOf<String?>(null)
    // 정확 알람 권한: 설정 창에 보일 현재 상태와, 처음 한 번만 띄우는 설명 다이얼로그.
    private var exactAlarmStatus by mutableStateOf(ExactAlarmStatus.NOT_REQUIRED)
    private var showExactAlarmPrompt by mutableStateOf(false)
    // 로그인한 계정에 삭제 예약이 있어 사용자의 답을 기다리는 동안 업로드·캘린더 구독을 멈춘다.
    @Volatile private var accountDeletionGate = false
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var calendarDataSource: FirebaseCalendarPullDataSource? = null
    private var calendarSubscription: FirebaseCalendarPullDataSource.CalendarSubscription? = null
    private var calendarSubscriptionUserId: String? = null
    private var calendarRolloverJob: Job? = null
    // Shared running timer with the PC web app (users/{uid}/flowlog/timer), followed while
    // the app is in the foreground and signed in, on the same lifecycle as the calendar.
    private var remoteTimerRegistration: ListenerRegistration? = null
    private var remoteTimerUserId: String? = null
    // 로그인·네트워크 복구 시 uploadLocalFlowlogSnapshot이 동시에 여러 번 호출되는 것을 방지
    private val syncMutex = Mutex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // FirebaseApp is initialized by FirebaseInitProvider before any Activity starts.
        FlowlogAppCheck.install()
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        requestedScreen = intent.getStringExtra(EXTRA_OPEN_SCREEN) ?: SCREEN_HOME
        if (savedInstanceState == null) {
            pendingShortcutCategory = shortcutCategoryFrom(intent)
        }
        ReminderScheduler(applicationContext).ensureNotificationChannel()
        FirebaseSyncAlarmScheduler.scheduleNextMidnightSync(applicationContext)
        lifecycleScope.launch {
            runCatching { AutoButtonScheduler(applicationContext).rescheduleAll() }
            runCatching { PlannedTodoReminderScheduler(applicationContext).rescheduleAll() }
        }
        requestNotificationPermission()
        exactAlarmStatus = ExactAlarmPermission.status(this)
        showExactAlarmPrompt = ExactAlarmPermission.shouldPrompt(this)
        // The UI is designed for a light palette only (hard-coded surface colors), so keep
        // dark status/navigation icons even when the system is in dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            // Light palette without dynamic color: most surfaces use fixed light colors, and a
            // system dark or dynamic scheme only recolored Material defaults (switches, chips),
            // producing a mixed light/dark screen.
            FlowlogTheme(darkTheme = false, dynamicColor = false) {
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
                LaunchedEffect(pendingShortcutCategory) {
                    val category = pendingShortcutCategory ?: return@LaunchedEffect
                    pendingShortcutCategory = null
                    if (activityViewModel.uiState.value.isRunning) {
                        Toast.makeText(
                            this@MainActivity,
                            getString(R.string.shortcut_timer_already_running),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        activityViewModel.startActivity(category)
                    }
                    showHomeMainTimer()
                }
                val shortcutCategories = remember(activityUiState.mainButtonConfig) {
                    TimerShortcutRules.shortcutCategories(activityUiState.mainButtonConfig)
                }
                LaunchedEffect(shortcutCategories) {
                    withContext(Dispatchers.Default) {
                        TimerShortcutPublisher.publish(applicationContext, shortcutCategories)
                    }
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
                // 로그인한 계정에 삭제 예약이 있으면 (uid, 상태). 답할 때까지 평소 로그인 작업을 미룬다.
                var deletionPrompt by remember { mutableStateOf<Pair<String, AccountDeletionState>?>(null) }
                var resolvingDeletion by remember { mutableStateOf(false) }
                val runSignedInSetup: suspend (FirebaseUser) -> Unit = { user ->
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
                }
                val signOutCompletely: suspend () -> Unit = {
                    auth.signOut()
                    runCatching {
                        CredentialManager.create(this@MainActivity)
                            .clearCredentialState(ClearCredentialStateRequest())
                    }
                }
                LaunchedEffect(signedInUser) {
                    val user = signedInUser
                    if (user != null) {
                        // 삭제 예약 확인을 평소 작업(익명 기록 이관·복원·업로드)보다 먼저 한다.
                        // 읽지 못하면(오프라인 등) 평소대로 진행하고, 다음 실행 때 다시 확인한다.
                        val requestedAt = runCatching { AccountDeletionRemote.readRequestedAt(user.uid) }.getOrNull()
                        val deletionState = AccountDeletionPolicy.state(requestedAt, System.currentTimeMillis())
                        if (deletionState != AccountDeletionState.None) {
                            accountDeletionGate = true
                            stopCalendarSubscription()
                            deletionPrompt = user.uid to deletionState
                            return@LaunchedEffect
                        }
                        runSignedInSetup(user)
                    } else {
                        accountDeletionGate = false
                        deletionPrompt = null
                        stopCalendarSubscription()
                    }
                }
                deletionPrompt?.let { (deletionUid, deletionState) ->
                    AccountDeletionPendingDialog(
                        state = deletionState,
                        busy = resolvingDeletion,
                        onCancelAndRestore = {
                            if (resolvingDeletion) return@AccountDeletionPendingDialog
                            resolvingDeletion = true
                            scope.launch {
                                val cancelled = runCatching { AccountDeletionRemote.cancelDeletion(deletionUid) }.isSuccess
                                if (cancelled && auth.currentUser?.uid == deletionUid) {
                                    runCatching { FirebaseRestoreDataSource(applicationContext).restoreFromFirestore(deletionUid) }
                                    accountDeletionGate = false
                                    deletionPrompt = null
                                    auth.currentUser?.let { runSignedInSetup(it) }
                                    Toast.makeText(this@MainActivity, getString(R.string.account_deletion_restored), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@MainActivity, getString(R.string.account_deletion_cancel_failed), Toast.LENGTH_LONG).show()
                                    deletionPrompt = null
                                    signOutCompletely()
                                }
                                resolvingDeletion = false
                            }
                        },
                        onSignOut = {
                            deletionPrompt = null
                            scope.launch { signOutCompletely() }
                        }
                    )
                }
                val deleteAccountAndData: () -> Unit = {
                    val uid = auth.currentUser?.uid
                    if (uid != null && !resolvingDeletion) {
                        resolvingDeletion = true
                        Toast.makeText(this@MainActivity, getString(R.string.account_delete_in_progress), Toast.LENGTH_SHORT).show()
                        scope.launch {
                            // 서버 예약이 먼저다. 실패하면 폰 데이터도 그대로 둔다.
                            val reserved = runCatching { AccountDeletionRemote.requestDeletion(uid) }.isSuccess
                            if (!reserved) {
                                resolvingDeletion = false
                                Toast.makeText(this@MainActivity, getString(R.string.account_delete_failed), Toast.LENGTH_LONG).show()
                                return@launch
                            }
                            accountDeletionGate = true
                            stopCalendarSubscription()
                            // 알람 조회가 현재 계정의 행을 읽으므로 로그아웃 전에 지운다.
                            runCatching { DeviceDataWiper.wipe(applicationContext) }
                            signOutCompletely()
                            Toast.makeText(applicationContext, getString(R.string.account_delete_scheduled), Toast.LENGTH_LONG).show()
                            // 화면과 ViewModel의 메모리 상태(진행 중 타이머 등)를 버리고 새로 시작한다.
                            startActivity(
                                Intent(applicationContext, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            )
                            finish()
                        }
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
                            // 삭제 예약이 있는 계정이면 로그인 확인 창이 답을 받을 때까지 올리지 않는다.
                            val uid = auth.currentUser?.uid
                            val pendingDeletion = uid != null && runCatching {
                                AccountDeletionRemote.readRequestedAt(uid) != null
                            }.getOrDefault(false)
                            if (!pendingDeletion) uploadLocalFlowlogSnapshot()
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
                val exportActivities: (ActivityExportFormat, Uri) -> Unit = { format, uri ->
                    activityViewModel.exportActivities(uri, format) { count ->
                        val message = if (count != null) {
                            getString(R.string.export_activities_success, count)
                        } else {
                            getString(R.string.export_activities_failed)
                        }
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
                var pendingImport by remember { mutableStateOf<ActivityImportResult.Ready?>(null) }
                val importActivities: (Uri) -> Unit = { uri ->
                    activityViewModel.previewActivityImport(uri) { result ->
                        when {
                            result == null -> Toast.makeText(this@MainActivity, getString(R.string.import_activities_read_failed), Toast.LENGTH_SHORT).show()
                            result !is ActivityImportResult.Ready -> Toast.makeText(this@MainActivity, getString(R.string.import_activities_not_json), Toast.LENGTH_SHORT).show()
                            result.summary.newCount == 0 -> Toast.makeText(
                                this@MainActivity,
                                getString(R.string.import_activities_nothing_new, result.summary.duplicateCount, result.summary.invalidCount),
                                Toast.LENGTH_LONG
                            ).show()
                            else -> pendingImport = result
                        }
                    }
                }
                val regenerateRecommendedTimePlan: () -> Unit = {
                    todoViewModel.refreshSort()
                    Toast.makeText(this@MainActivity, getString(R.string.main_activity_recommended_plan_regenerated), Toast.LENGTH_SHORT).show()
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
                                            },
                                            onExportActivities = exportActivities,
                                            onImportActivities = importActivities,
                                            onDeleteAccount = deleteAccountAndData,
                                            exactAlarmStatus = exactAlarmStatus,
                                            onOpenExactAlarmSettings = {
                                                ExactAlarmPermission.openSettings(this@MainActivity)
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
                pendingImport?.let { preview ->
                    ActivityImportConfirmDialog(
                        summary = preview.summary,
                        onConfirm = {
                            pendingImport = null
                            activityViewModel.commitActivityImport(preview.toInsert) { count ->
                                val message = if (count != null) {
                                    getString(R.string.import_activities_success, count)
                                } else {
                                    getString(R.string.import_activities_save_failed)
                                }
                                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDismiss = { pendingImport = null }
                    )
                }
                if (showExactAlarmPrompt) {
                    ExactAlarmPromptDialog(
                        onOpenSettings = { answerExactAlarmPrompt(openSettings = true) },
                        onLater = { answerExactAlarmPrompt(openSettings = false) }
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

    override fun onResume() {
        super.onResume()
        // 시스템 설정에서 돌아왔을 때 설정 창의 "정확한 알림" 상태를 새로 읽는다.
        exactAlarmStatus = ExactAlarmPermission.status(this)
        if (exactAlarmStatus != ExactAlarmStatus.DENIED) showExactAlarmPrompt = false
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
        if (accountDeletionGate) return null
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
        if (accountDeletionGate) return null
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
        if (accountDeletionGate) return
        startRemoteTimerSync(userId)
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

    private fun startRemoteTimerSync(userId: String) {
        if (remoteTimerRegistration != null && remoteTimerUserId == userId) return
        stopRemoteTimerSync()
        remoteTimerUserId = userId
        remoteTimerRegistration = runCatching { RemoteTimerSync.listen(applicationContext, userId) }.getOrNull()
    }

    private fun stopRemoteTimerSync() {
        remoteTimerRegistration?.remove()
        remoteTimerRegistration = null
        remoteTimerUserId = null
    }

    private fun stopCalendarSubscription() {
        stopRemoteTimerSync()
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
        shortcutCategoryFrom(intent)?.let { pendingShortcutCategory = it }
    }

    private fun shortcutCategoryFrom(intent: Intent?): String? {
        if (intent == null) return null
        return TimerShortcutRules.parseStartCategory(
            action = intent.action,
            category = runCatching { intent.getStringExtra(TimerShortcutRules.EXTRA_CATEGORY) }.getOrNull(),
            launchedFromHistory = (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
        )
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

    private fun answerExactAlarmPrompt(openSettings: Boolean) {
        ExactAlarmPermission.markPrompted(this)
        showExactAlarmPrompt = false
        if (openSettings) ExactAlarmPermission.openSettings(this)
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
    onToggleDevMode: () -> Unit = {},
    isNotificationSoundEnabled: Boolean = true,
    onToggleNotificationSound: () -> Unit = {},
    isInactivityReminderEnabled: Boolean = true,
    onToggleInactivityReminder: () -> Unit = {},
    isTimetableAutoPlaceEnabled: Boolean = true,
    onToggleTimetableAutoPlace: () -> Unit = {},
    isTimetableReminderEnabled: Boolean = true,
    onToggleTimetableReminder: () -> Unit = {},
    onExportActivities: (ActivityExportFormat, Uri) -> Unit = { _, _ -> },
    onImportActivities: (Uri) -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    exactAlarmStatus: ExactAlarmStatus = ExactAlarmStatus.NOT_REQUIRED,
    onOpenExactAlarmSettings: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    // 계정 삭제: 0 = 닫힘, 1 = 첫 확인(설명), 2 = 마지막 확인('삭제' 입력)
    var accountDeleteStep by remember { mutableStateOf(0) }
    val csvExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ActivityExportFormat.CSV.mimeType)
    ) { uri -> uri?.let { onExportActivities(ActivityExportFormat.CSV, it) } }
    val jsonExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ActivityExportFormat.JSON.mimeType)
    ) { uri -> uri?.let { onExportActivities(ActivityExportFormat.JSON, it) } }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(onImportActivities) }
    val launchExport: (ActivityExportFormat) -> Unit = { format ->
        val fileName = ActivityExportFormatter.defaultFileName(
            nowMillis = System.currentTimeMillis(),
            format = format,
            timeZone = TimeZone.getDefault()
        )
        val launcher = if (format == ActivityExportFormat.CSV) csvExportLauncher else jsonExportLauncher
        runCatching { launcher.launch(fileName) }
    }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Signed-out users have no profile menu, so Settings (notifications,
                    // export) gets its own entry point; the app works offline without login.
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.main_activity_settings),
                            tint = if (isFocusFireActive) Color(0xFFFF7A2F) else Color(0xFF5140D8),
                            modifier = Modifier.size(24.dp)
                        )
                    }
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

    if (accountDeleteStep == 1) {
        AlertDialog(
            onDismissRequest = { accountDeleteStep = 0 },
            containerColor = Color.White,
            title = {
                Text(
                    text = stringResource(R.string.account_delete_confirm1_title),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFF10182C)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.account_delete_confirm1_message),
                    fontSize = 14.sp,
                    color = Color(0xFF3C4457)
                )
            },
            confirmButton = {
                TextButton(onClick = { accountDeleteStep = 2 }) {
                    Text(stringResource(R.string.account_delete_continue), color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountDeleteStep = 0 }) {
                    Text(stringResource(R.string.account_delete_cancel), color = Color(0xFF697386))
                }
            }
        )
    }
    if (accountDeleteStep == 2) {
        var typed by remember { mutableStateOf("") }
        val confirmed = AccountDeletionPolicy.isConfirmationTyped(typed)
        AlertDialog(
            onDismissRequest = { accountDeleteStep = 0 },
            containerColor = Color.White,
            title = {
                Text(
                    text = stringResource(R.string.account_delete_confirm2_title),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFF10182C)
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.account_delete_confirm2_message),
                        fontSize = 14.sp,
                        color = Color(0xFF3C4457)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        singleLine = true,
                        placeholder = { Text(AccountDeletionPolicy.CONFIRMATION_WORD) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = confirmed,
                    onClick = {
                        accountDeleteStep = 0
                        showSettingsDialog = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFC62828),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.account_delete_execute), fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountDeleteStep = 0 }) {
                    Text(stringResource(R.string.account_delete_cancel), color = Color(0xFF697386))
                }
            }
        )
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
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                    if (exactAlarmStatus != ExactAlarmStatus.NOT_REQUIRED) {
                        val exactAllowed = exactAlarmStatus == ExactAlarmStatus.ALLOWED
                        Spacer(modifier = Modifier.height(14.dp))
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (exactAllowed) Icons.Filled.Alarm else Icons.Filled.AlarmOff,
                                contentDescription = null,
                                tint = if (exactAllowed) Color(0xFF5140D8) else Color(0xFF9E9E9E),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.size(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_exact_alarm_title),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10182C)
                                )
                                Text(
                                    text = if (exactAllowed) stringResource(R.string.settings_exact_alarm_on) else stringResource(R.string.settings_exact_alarm_off),
                                    fontSize = 12.sp,
                                    color = Color(0xFF9E9E9E)
                                )
                            }
                            // 특수 권한은 시스템 설정에서만 바꿀 수 있다. 돌아오면 onResume에서 상태를 다시 읽는다.
                            TextButton(onClick = onOpenExactAlarmSettings) {
                                Text(
                                    text = stringResource(R.string.settings_exact_alarm_open),
                                    color = Color(0xFF5140D8),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
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
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.export_activities_section),
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
                            imageVector = Icons.Filled.FileDownload,
                            contentDescription = null,
                            tint = Color(0xFF5140D8),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.export_activities_title),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10182C)
                            )
                            Text(
                                text = stringResource(R.string.export_activities_desc),
                                fontSize = 12.sp,
                                color = Color(0xFF9E9E9E)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { launchExport(ActivityExportFormat.CSV) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.export_activities_csv), color = Color(0xFF5140D8), fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { launchExport(ActivityExportFormat.JSON) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(stringResource(R.string.export_activities_json), color = Color(0xFF5140D8), fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            runCatching { importLauncher.launch(arrayOf(ActivityExportFormat.JSON.mimeType)) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.import_activities_button), color = Color(0xFF5140D8), fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = stringResource(R.string.import_activities_desc),
                        fontSize = 12.sp,
                        color = Color(0xFF9E9E9E),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    if (isSignedIn) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = stringResource(R.string.account_delete_section),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5140D8)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.account_delete_title),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                                Text(
                                    text = stringResource(R.string.account_delete_desc),
                                    fontSize = 12.sp,
                                    color = Color(0xFF9E9E9E)
                                )
                            }
                            TextButton(onClick = { accountDeleteStep = 1 }) {
                                Text(
                                    text = stringResource(R.string.account_delete_button),
                                    color = Color(0xFFC62828),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
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
private fun AccountDeletionPendingDialog(
    state: AccountDeletionState,
    busy: Boolean,
    onCancelAndRestore: () -> Unit,
    onSignOut: () -> Unit
) {
    val pending = state as? AccountDeletionState.Pending
    AlertDialog(
        // 답하기 전에는 닫히지 않는다: 답에 따라 복원하거나 로그아웃해야 한다.
        onDismissRequest = {},
        containerColor = Color.White,
        title = {
            Text(
                text = stringResource(
                    if (pending != null) R.string.account_deletion_pending_title else R.string.account_deletion_expired_title
                ),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = Color(0xFF10182C)
            )
        },
        text = {
            Text(
                text = if (pending != null) {
                    stringResource(R.string.account_deletion_pending_message, pending.daysLeft)
                } else {
                    stringResource(R.string.account_deletion_expired_message)
                },
                fontSize = 14.sp,
                color = Color(0xFF3C4457)
            )
        },
        confirmButton = {
            if (pending != null) {
                Button(
                    enabled = !busy,
                    onClick = onCancelAndRestore,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF5140D8),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.account_deletion_cancel_restore), fontWeight = FontWeight.ExtraBold)
                }
            } else {
                TextButton(onClick = onSignOut) {
                    Text(stringResource(R.string.account_deletion_ok), color = Color(0xFF5140D8), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (pending != null) {
                TextButton(enabled = !busy, onClick = onSignOut) {
                    Text(stringResource(R.string.account_deletion_keep), color = Color(0xFF697386))
                }
            }
        }
    )
}

@Composable
private fun ActivityImportConfirmDialog(
    summary: ActivityImportSummary,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { java.text.SimpleDateFormat("yyyy.MM.dd", java.util.Locale.KOREA) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                text = stringResource(R.string.import_activities_title),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = Color(0xFF10182C)
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.import_activities_confirm_message, summary.newCount, summary.duplicateCount),
                    fontSize = 14.sp,
                    color = Color(0xFF3C4457)
                )
                if (summary.invalidCount > 0) {
                    Text(
                        text = stringResource(R.string.import_activities_invalid, summary.invalidCount),
                        fontSize = 14.sp,
                        color = Color(0xFF3C4457)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.import_activities_total, summary.total),
                    fontSize = 12.sp,
                    color = Color(0xFF697386)
                )
                if (summary.firstStartMillis != null && summary.lastEndMillis != null) {
                    Text(
                        text = stringResource(
                            R.string.import_activities_period,
                            dateFormat.format(java.util.Date(summary.firstStartMillis)),
                            dateFormat.format(java.util.Date(summary.lastEndMillis))
                        ),
                        fontSize = 12.sp,
                        color = Color(0xFF697386)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5140D8),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.import_activities_confirm), fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.import_activities_cancel), color = Color(0xFF697386))
            }
        }
    )
}

@Composable
private fun ExactAlarmPromptDialog(
    onOpenSettings: () -> Unit,
    onLater: () -> Unit
) {
    AlertDialog(
        // 바깥을 눌러 닫아도 "나중에"로 본다(다시 묻지 않음).
        onDismissRequest = onLater,
        containerColor = Color.White,
        icon = {
            Icon(
                imageVector = Icons.Filled.Alarm,
                contentDescription = null,
                tint = Color(0xFF5140D8)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.exact_alarm_prompt_title),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = Color(0xFF10182C)
            )
        },
        text = {
            Text(
                text = stringResource(R.string.exact_alarm_prompt_message),
                fontSize = 14.sp,
                color = Color(0xFF3C4457)
            )
        },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF5140D8),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.exact_alarm_prompt_open_settings), fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onLater) {
                Text(stringResource(R.string.main_activity_later), color = Color(0xFF697386))
            }
        }
    )
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
                        text = stringResource(R.string.main_activity_suggestion_sheet_title),
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
