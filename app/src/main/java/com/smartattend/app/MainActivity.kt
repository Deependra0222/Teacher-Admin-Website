package com.smartattend.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartattend.app.ui.SmartAttendApp
import com.smartattend.app.ui.SmartAttendViewModel
import com.smartattend.app.ui.theme.SmartAttendTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: SmartAttendViewModel = viewModel()
            SmartAttendTheme(themePreference = viewModel.uiState.settings.theme) {
                SmartAttendApp(viewModel)
            }
        }
    }
}
