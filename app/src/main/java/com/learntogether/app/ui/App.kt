package com.learntogether.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.learntogether.app.data.AppViewModel
import com.learntogether.app.ui.screens.HomeScreen
import com.learntogether.app.ui.screens.LessonScreen
import com.learntogether.app.ui.screens.LessonsScreen
import com.learntogether.app.ui.screens.ProgressScreen
import com.learntogether.app.ui.screens.SettingsScreen
import com.learntogether.app.ui.screens.SubjectScreen
import com.learntogether.app.ui.screens.WelcomeScreen
import com.learntogether.app.ui.theme.LocalAppExtras
import com.learntogether.app.voice.VoiceListener
import com.learntogether.app.voice.VoiceRouter

object Routes {
    const val WELCOME = "welcome"
    const val HOME = "home"
    const val LESSONS = "lessons"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"
    const val SUBJECT = "subject/{id}"
    const val LESSON = "lesson/{id}"

    fun subject(id: String) = "subject/$id"
    fun lesson(id: String) = "lesson/$id"
}

/**
 * Switches to one of the four main tabs. Home is reached by going back to it, which always works,
 * even when Settings was opened on top of it from the accessibility icon.
 */
fun NavHostController.navigateToTab(route: String) {
    if (route == Routes.HOME) {
        if (!popBackStack(Routes.HOME, inclusive = false)) {
            navigate(Routes.HOME) { launchSingleTop = true }
        }
    } else {
        navigate(route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.LESSONS, "Lessons", Icons.AutoMirrored.Filled.MenuBook),
    Tab(Routes.PROGRESS, "Progress", Icons.Filled.BarChart),
    Tab(Routes.SETTINGS, "Settings", Icons.Filled.Settings)
)

@Composable
fun LearnTogetherApp(vm: AppViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val startDestination = remember { if (vm.onboarded) Routes.HOME else Routes.WELCOME }
    val extras = LocalAppExtras.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val router = remember(nav) { VoiceRouter(vm, nav) }

    val showBottomBar = tabs.any { it.route == route }
    val showTopBar = route != null && route != Routes.WELCOME

    val title = when (route) {
        Routes.HOME -> "Learnmate"
        Routes.LESSONS -> "Lessons"
        Routes.PROGRESS -> "My Progress"
        Routes.SETTINGS -> "Settings"
        Routes.SUBJECT -> vm.subject(backStack?.arguments?.getString("id").orEmpty())?.title ?: "Subject"
        Routes.LESSON -> vm.lesson(backStack?.arguments?.getString("id").orEmpty())?.title ?: "Lesson"
        else -> ""
    }
    val canGoBack = route == Routes.SUBJECT || route == Routes.LESSON
    val goBack: () -> Unit = { nav.popBackStack() }

    fun openLesson(id: String) {
        vm.openLesson(id)
        nav.navigate(Routes.lesson(id))
    }

    // ---- Voice commands ----
    fun startListening() {
        if (vm.voice.state != VoiceListener.State.Idle) {
            vm.voice.cancel()
            return
        }
        if (!vm.voice.available) {
            vm.announce("Voice commands are not available on this device.")
            return
        }
        vm.stopSpeaking() // so the microphone doesn't hear the app talking
        haptic.performHapticFeedback(HapticFeedbackType.LongPress) // a touch cue that it is listening
        vm.voice.start(
            language = vm.voiceLanguage,
            onResult = router::handle,
            onError = vm::announce
        )
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening()
        else vm.announce("Microphone permission is needed for voice commands. You can allow it in Android settings.")
    }

    fun onVoiceBarClick() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) startListening() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    // Shake / twist shortcut: starts listening just like tapping the voice bar.
    LaunchedEffect(vm.motionTick) {
        if (!vm.consumeMotionRequest()) return@LaunchedEffect
        if (route == null || route == Routes.WELCOME) return@LaunchedEffect
        if (vm.voice.state != VoiceListener.State.Idle) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) startListening()
        else vm.announce("Tap the voice button once to allow the microphone. Then try again.")
    }

    Scaffold(
        topBar = {
            if (showTopBar) {
                AppTopBar(
                    title = title,
                    onBack = if (canGoBack) goBack else null,
                    onAccessibility = {
                        if (route != Routes.SETTINGS) nav.navigate(Routes.SETTINGS) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        },
        bottomBar = {
            Column {
                if (showTopBar) VoiceBar(vm.voice, onClick = ::onVoiceBarClick)
                if (showBottomBar) {
                    NavigationBar(containerColor = extras.topBar, contentColor = extras.onTopBar) {
                        tabs.forEach { tab ->
                            val selected = backStack?.destination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { nav.navigateToTab(tab.route) },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = extras.onIndicator,
                                    selectedTextColor = extras.onTopBar,
                                    indicatorColor = extras.indicator,
                                    unselectedIconColor = extras.onTopBar,
                                    unselectedTextColor = extras.onTopBar
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.WELCOME) {
                WelcomeScreen(onStart = {
                    vm.finishOnboarding()
                    nav.navigate(Routes.HOME) { popUpTo(Routes.WELCOME) { inclusive = true } }
                })
            }
            composable(Routes.HOME) {
                HomeScreen(
                    vm = vm,
                    onSubject = { nav.navigate(Routes.subject(it)) },
                    onProgress = { nav.navigate(Routes.PROGRESS) { launchSingleTop = true } }
                )
            }
            composable(Routes.LESSONS) {
                LessonsScreen(vm = vm, onLesson = ::openLesson)
            }
            composable(Routes.PROGRESS) { ProgressScreen(vm) }
            composable(Routes.SETTINGS) { SettingsScreen(vm) }
            composable(
                Routes.SUBJECT,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                SubjectScreen(
                    vm = vm,
                    subjectId = entry.arguments?.getString("id").orEmpty(),
                    onLesson = ::openLesson
                )
            }
            composable(
                Routes.LESSON,
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { entry ->
                LessonScreen(
                    vm = vm,
                    lessonId = entry.arguments?.getString("id").orEmpty(),
                    onFinish = goBack
                )
            }
        }
    }
}
