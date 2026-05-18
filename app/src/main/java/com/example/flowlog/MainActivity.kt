package com.example.flowlog

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.flowlog.notification.ReminderScheduler
import androidx.lifecycle.ViewModelProvider
import com.example.flowlog.ui.screen.HomeScreen
import com.example.flowlog.ui.screen.TodoScreen
import com.example.flowlog.ui.theme.FlowlogTheme
import com.example.flowlog.ui.viewmodel.ActivityViewModel
import com.example.flowlog.ui.viewmodel.ActivityViewModelFactory
import com.example.flowlog.ui.viewmodel.TodoViewModel
import com.example.flowlog.ui.viewmodel.TodoViewModelFactory

class MainActivity : ComponentActivity() {
    private var widgetStartCategory by mutableStateOf<String?>(null)
    private var requestedScreen by mutableStateOf(SCREEN_HOME)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetStartCategory = intent.getStringExtra(EXTRA_START_CATEGORY)
        requestedScreen = intent.getStringExtra(EXTRA_OPEN_SCREEN) ?: SCREEN_HOME
        ReminderScheduler(applicationContext).ensureNotificationChannel()
        requestNotificationPermission()
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
                        }
                    }
                }
            }
        }
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

    companion object {
        const val EXTRA_START_CATEGORY = "com.example.flowlog.extra.START_CATEGORY"
        const val EXTRA_OPEN_SCREEN = "com.example.flowlog.extra.OPEN_SCREEN"
        const val SCREEN_HOME = "home"
        const val SCREEN_TODO = "todo"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }
}
