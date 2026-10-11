package com.smartattend.app.domain.auth

import com.smartattend.app.core.model.Student

data class AuthenticationState(
    val mode: AuthSessionMode,
    val student: Student? = null,
    val error: AuthError? = null,
)

class AuthenticationCoordinator(
    private val repository: AuthenticationRepository,
    private val sessionStore: AuthenticationSessionStore,
    private val demoStudent: Student,
) {
    suspend fun restore(): AuthenticationState = when (val result = repository.restoreSession()) {
        is AuthenticationResult.Authorized -> {
            sessionStore.setDemoModeEnabled(false)
            result.toState()
        }

        AuthenticationResult.NoSession -> {
            if (sessionStore.isDemoModeEnabled()) {
                AuthenticationState(AuthSessionMode.DEMO, demoStudent)
            } else {
                signedOut()
            }
        }

        is AuthenticationResult.Failure -> {
            sessionStore.setDemoModeEnabled(false)
            result.toState()
        }
    }

    suspend fun signIn(email: String, password: String): AuthenticationState {
        sessionStore.setDemoModeEnabled(false)
        LoginValidator.validateFirebase(email, password)?.let { return signedOut(it) }
        return repository.signIn(email.trim(), password).toState()
    }

    suspend fun enterDemoMode(): AuthenticationState {
        repository.signOut()
        sessionStore.setDemoModeEnabled(true)
        return AuthenticationState(AuthSessionMode.DEMO, demoStudent)
    }

    suspend fun logout(): AuthenticationState {
        repository.signOut()
        sessionStore.setDemoModeEnabled(false)
        return signedOut()
    }

    suspend fun sendPasswordReset(email: String): PasswordResetResult {
        LoginValidator.validateFirebase(email, "password-reset")?.let {
            return PasswordResetResult.Failure(AuthError.INVALID_EMAIL)
        }
        return repository.sendPasswordReset(email.trim())
    }

    private fun AuthenticationResult.toState(): AuthenticationState = when (this) {
        is AuthenticationResult.Authorized -> AuthenticationState(AuthSessionMode.FIREBASE, student)
        AuthenticationResult.NoSession -> signedOut()
        is AuthenticationResult.Failure -> signedOut(error)
    }

    private fun signedOut(error: AuthError? = null) = AuthenticationState(
        mode = AuthSessionMode.SIGNED_OUT,
        error = error,
    )
}
