package com.smartattend.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartattend.app.ui.attendance.AttendanceScreen
import com.smartattend.app.ui.attendance.ShortageCalculatorScreen
import com.smartattend.app.ui.attendance.SubjectDetailScreen
import com.smartattend.app.ui.components.DemoBanner
import com.smartattend.app.ui.dashboard.DashboardScreen
import com.smartattend.app.ui.history.HistoryScreen
import com.smartattend.app.ui.inbox.InboxScreen
import com.smartattend.app.ui.inbox.NoticeDetailScreen
import com.smartattend.app.ui.login.LoginScreen
import com.smartattend.app.ui.more.AboutDemoScreen
import com.smartattend.app.ui.more.CorrectionDetailScreen
import com.smartattend.app.ui.more.CorrectionFormScreen
import com.smartattend.app.ui.more.CorrectionListScreen
import com.smartattend.app.ui.more.DeviceRegistrationScreen
import com.smartattend.app.ui.more.MoreScreen
import com.smartattend.app.ui.more.SettingsScreen
import com.smartattend.app.ui.navigation.StudentRoutes
import com.smartattend.app.ui.profile.ProfileScreen
import com.smartattend.app.ui.schedule.ScheduleDetailScreen
import com.smartattend.app.ui.schedule.ScheduleScreen

private data class PrimaryDestination(val route: String, val label: String, val icon: ImageVector)

private val primaryDestinations = listOf(
    PrimaryDestination(StudentRoutes.DASHBOARD, "Dashboard", Icons.Rounded.Dashboard),
    PrimaryDestination(StudentRoutes.ATTENDANCE, "Attendance", Icons.Rounded.PieChart),
    PrimaryDestination(StudentRoutes.SCHEDULE, "Schedule", Icons.Rounded.CalendarMonth),
    PrimaryDestination(StudentRoutes.INBOX, "Inbox", Icons.Rounded.Notifications),
    PrimaryDestination(StudentRoutes.MORE, "More", Icons.Rounded.GridView),
)

@Composable
fun SmartAttendApp(viewModel: SmartAttendViewModel) {
    val state = viewModel.uiState
    if (!state.isLoggedIn) {
        LoginScreen(error = state.loginError, onLogin = viewModel::login)
        return
    }
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (primaryDestinations.any { it.route == currentRoute }) {
                NavigationBar {
                    primaryDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = { navController.openPrimary(destination.route) },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            DemoBanner(applyStatusInset = false)
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.fatalError != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Offline data could not be opened")
                        Text(state.fatalError)
                        Button(onClick = viewModel::retryOfflineData, modifier = Modifier.fillMaxWidth()) { Text("Retry") }
                    }
                }
                else -> StudentNavHost(navController, state, viewModel, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun StudentNavHost(navController: NavHostController, state: SmartAttendUiState, viewModel: SmartAttendViewModel, modifier: Modifier) {
    NavHost(navController, startDestination = StudentRoutes.DASHBOARD, modifier = modifier) {
        composable(StudentRoutes.DASHBOARD) {
            DashboardScreen(
                state,
                onAttendance = { navController.openPrimary(StudentRoutes.ATTENDANCE) },
                onSchedule = { navController.openPrimary(StudentRoutes.SCHEDULE) },
                onInbox = { navController.openPrimary(StudentRoutes.INBOX) },
                onCalculator = { navController.navigate(StudentRoutes.SHORTAGE_CALCULATOR) },
                onSimulatePresence = viewModel::simulateBlePresence,
                onSimulateLoss = viewModel::simulateBleSignalLoss,
                onCalculateSession = viewModel::finishDemoSession,
            )
        }
        composable(StudentRoutes.ATTENDANCE) {
            AttendanceScreen(
                state.subjects,
                onSubject = { navController.navigate(StudentRoutes.subject(it)) },
                onCalculator = { navController.navigate(StudentRoutes.SHORTAGE_CALCULATOR) },
                onHistory = { navController.navigate(StudentRoutes.HISTORY) },
            )
        }
        composable(StudentRoutes.SCHEDULE) {
            ScheduleScreen(state.timetable, state.selectedScheduleDate, viewModel::setScheduleDate) { navController.navigate(StudentRoutes.scheduleDetail(it)) }
        }
        composable(StudentRoutes.INBOX) {
            InboxScreen(state.notices, state.noticeFilter, viewModel::setNoticeFilter, { navController.navigate(StudentRoutes.notice(it)) }, viewModel::markAllNoticesRead)
        }
        composable(StudentRoutes.MORE) {
            MoreScreen(
                state,
                onProfile = { navController.navigate(StudentRoutes.PROFILE) },
                onCorrections = { navController.navigate(StudentRoutes.CORRECTIONS) },
                onDevice = { navController.navigate(StudentRoutes.DEVICE) },
                onSettings = { navController.navigate(StudentRoutes.SETTINGS) },
                onAbout = { navController.navigate(StudentRoutes.ABOUT_DEMO) },
            )
        }
        composable(StudentRoutes.SUBJECT_PATTERN) { entry ->
            val id = entry.arguments?.getString("id")
            SubjectDetailScreen(state.subjects.firstOrNull { it.code == id }, state.history, navController::popBackStack, { navController.navigate(StudentRoutes.SHORTAGE_CALCULATOR) }) {
                navController.navigate(StudentRoutes.correctionForm(it))
            }
        }
        composable(StudentRoutes.SHORTAGE_CALCULATOR) {
            ShortageCalculatorScreen(state, navController::popBackStack, viewModel::setCalculatorAttended, viewModel::setCalculatorTotal, viewModel::setCalculatorTarget, viewModel::calculateShortage)
        }
        composable(StudentRoutes.HISTORY) {
            HistoryScreen(state.history, state.historySubjectFilter, state.historyStatusFilter, viewModel::setHistorySubjectFilter, viewModel::setHistoryStatusFilter, { navController.navigate(StudentRoutes.correctionForm(it)) }, navController::popBackStack)
        }
        composable(StudentRoutes.SCHEDULE_DETAIL_PATTERN) { entry ->
            ScheduleDetailScreen(state.timetable.firstOrNull { it.id == entry.arguments?.getString("id") }, navController::popBackStack)
        }
        composable(StudentRoutes.NOTICE_PATTERN) { entry ->
            val notice = state.notices.firstOrNull { it.id == entry.arguments?.getString("id") }
            NoticeDetailScreen(notice, navController::popBackStack) { read -> notice?.let { viewModel.setNoticeRead(it.id, read) } }
        }
        composable(StudentRoutes.CORRECTIONS) {
            CorrectionListScreen(state.corrections, navController::popBackStack) { navController.navigate(StudentRoutes.correctionDetail(it)) }
        }
        composable(StudentRoutes.CORRECTION_FORM_PATTERN) { entry ->
            val record = state.history.firstOrNull { it.id == entry.arguments?.getString("id") }
            CorrectionFormScreen(record, navController::popBackStack) { status, reason -> record?.let { viewModel.submitCorrection(it.id, status, reason) } }
        }
        composable(StudentRoutes.CORRECTION_DETAIL_PATTERN) { entry ->
            CorrectionDetailScreen(state.corrections.firstOrNull { it.id == entry.arguments?.getString("id") }, navController::popBackStack)
        }
        composable(StudentRoutes.DEVICE) {
            DeviceRegistrationScreen(state.device, state.bleDecision?.let { "${it.kind}: ${it.reason}" }, navController::popBackStack, viewModel::registerDevice, viewModel::simulateBlePresence)
        }
        composable(StudentRoutes.SETTINGS) {
            SettingsScreen(state.settings, state.syncState, state.pendingOperations, navController::popBackStack, viewModel::setNotificationsEnabled, viewModel::setReminderMinutes, viewModel::setTheme, viewModel::resetDemoData)
        }
        composable(StudentRoutes.PROFILE) { ProfileScreen(state.student, navController::popBackStack, viewModel::logout) }
        composable(StudentRoutes.ABOUT_DEMO) { AboutDemoScreen(navController::popBackStack) }
    }
}

private fun NavHostController.openPrimary(route: String) {
    navigate(route) {
        popUpTo(StudentRoutes.DASHBOARD) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
