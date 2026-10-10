package com.smartattend.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.smartattend.app.ui.attendance.AttendanceScreen
import com.smartattend.app.ui.components.DemoBanner
import com.smartattend.app.ui.history.HistoryScreen
import com.smartattend.app.ui.home.HomeScreen
import com.smartattend.app.ui.login.LoginScreen
import com.smartattend.app.ui.profile.ProfileScreen
import com.smartattend.app.ui.timetable.TimetableScreen

@Composable
fun SmartAttendApp(viewModel: SmartAttendViewModel) {
    val state = viewModel.uiState
    if (!state.isLoggedIn) {
        LoginScreen(error = state.loginError, onLogin = viewModel::login)
        return
    }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                AppDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = state.destination == destination,
                        onClick = { viewModel.select(destination) },
                        icon = { Text(destination.shortLabel) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            DemoBanner()
            when (state.destination) {
                AppDestination.HOME -> HomeScreen(
                    student = state.student,
                    session = state.session,
                    sessionState = state.sessionState,
                    inside = state.insideClassroom,
                    confirmedMinutes = state.confirmedMinutes,
                    result = state.finalResult,
                    onEnter = viewModel::enterClassroom,
                    onExit = viewModel::exitClassroom,
                    onFinish = viewModel::finishDemoSession,
                )
                AppDestination.ATTENDANCE -> AttendanceScreen(state.subjects)
                AppDestination.HISTORY -> HistoryScreen(state.history)
                AppDestination.TIMETABLE -> TimetableScreen(state.timetable)
                AppDestination.PROFILE -> ProfileScreen(state.student, viewModel::logout)
            }
        }
    }
}
