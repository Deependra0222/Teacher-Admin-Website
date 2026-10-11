package com.smartattend.app.ui.login

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun credentialFieldsStartEmptyAndActionsAreDistinct() {
        var submitted: Pair<String, String>? = null
        showScreen(onFirebaseLogin = { email, password -> submitted = email to password })

        composeRule.onNodeWithText("Sign in with Firebase").assertExists()
        composeRule.onNodeWithText("Continue in Demo Mode").assertExists()
        composeRule.onNodeWithText("Sign in with Firebase").performClick()
        assertEquals("" to "", submitted)
    }

    @Test
    fun firebaseButtonOnlyCallsFirebaseActionWithEnteredCredentials() {
        var firebaseCredentials: Pair<String, String>? = null
        var demoCalls = 0
        showScreen(
            onFirebaseLogin = { email, password -> firebaseCredentials = email to password },
            onDemoMode = { demoCalls += 1 },
        )

        composeRule.onNodeWithTag("login_email").performTextInput("student@example.edu")
        composeRule.onNodeWithTag("login_password").performTextInput("secret value")
        composeRule.onNodeWithText("Sign in with Firebase").performClick()

        assertEquals("student@example.edu" to "secret value", firebaseCredentials)
        assertEquals(0, demoCalls)
    }

    @Test
    fun demoButtonOnlyCallsExplicitDemoAction() {
        var firebaseCalls = 0
        var demoCalls = 0
        showScreen(
            onFirebaseLogin = { _, _ -> firebaseCalls += 1 },
            onDemoMode = { demoCalls += 1 },
        )

        composeRule.onNodeWithText("Continue in Demo Mode").performClick()

        assertEquals(0, firebaseCalls)
        assertEquals(1, demoCalls)
    }

    @Test
    fun busyStateDisablesBothAuthenticationActions() {
        showScreen(isBusy = true)

        composeRule.onNodeWithText("Signing in…").assertIsNotEnabled()
        composeRule.onNodeWithText("Continue in Demo Mode").assertIsNotEnabled()
    }

    @Test
    fun passwordResetUsesEnteredEmail() {
        var resetEmail: String? = null
        showScreen(onPasswordReset = { resetEmail = it })

        composeRule.onNodeWithTag("login_email").performTextInput("student@example.edu")
        composeRule.onNodeWithText("Forgot password?").performClick()

        assertEquals("student@example.edu", resetEmail)
    }

    @Test
    fun errorIsShownWithoutEnteringDemoMode() {
        var demoCalls = 0
        showScreen(
            error = "Incorrect email or password.",
            onDemoMode = { demoCalls += 1 },
        )

        composeRule.onNodeWithText("Incorrect email or password.").assertExists()
        assertEquals(0, demoCalls)
    }

    private fun showScreen(
        error: String? = null,
        message: String? = null,
        isBusy: Boolean = false,
        onFirebaseLogin: (String, String) -> Unit = { _, _ -> },
        onDemoMode: () -> Unit = {},
        onPasswordReset: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            MaterialTheme {
                LoginScreen(
                    error = error,
                    message = message,
                    isBusy = isBusy,
                    onFirebaseLogin = onFirebaseLogin,
                    onDemoMode = onDemoMode,
                    onPasswordReset = onPasswordReset,
                )
            }
        }
    }
}
