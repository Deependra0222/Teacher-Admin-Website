package com.smartattend.app.domain.auth

import com.smartattend.app.core.model.Student

enum class AuthSessionMode {
    RESTORING,
    SIGNED_OUT,
    DEMO,
    FIREBASE,
}

enum class AuthError {
    INVALID_EMAIL,
    PASSWORD_REQUIRED,
    INVALID_CREDENTIALS,
    ACCOUNT_DISABLED,
    NETWORK,
    TOO_MANY_REQUESTS,
    PROFILE_MISSING,
    PROFILE_INCOMPLETE,
    UNAUTHORIZED_ROLE,
    PERMISSION_DENIED,
    SERVICE_UNAVAILABLE,
    UNKNOWN,
}

sealed interface AuthenticationResult {
    data class Authorized(val student: Student) : AuthenticationResult

    data object NoSession : AuthenticationResult

    data class Failure(val error: AuthError) : AuthenticationResult
}

sealed interface PasswordResetResult {
    data object Sent : PasswordResetResult

    data class Failure(val error: AuthError) : PasswordResetResult
}

interface AuthenticationRepository {
    suspend fun signIn(email: String, password: String): AuthenticationResult

    suspend fun restoreSession(): AuthenticationResult

    suspend fun signOut()

    suspend fun sendPasswordReset(email: String): PasswordResetResult
}

interface AuthenticationSessionStore {
    suspend fun isDemoModeEnabled(): Boolean

    suspend fun setDemoModeEnabled(enabled: Boolean)
}

fun AuthError.userMessage(): String = when (this) {
    AuthError.INVALID_EMAIL -> "Enter a valid email address."
    AuthError.PASSWORD_REQUIRED -> "Enter your password."
    AuthError.INVALID_CREDENTIALS -> "Incorrect email or password."
    AuthError.ACCOUNT_DISABLED -> "This account has been disabled. Contact your administrator."
    AuthError.NETWORK -> "No internet connection. Check your connection and try again."
    AuthError.TOO_MANY_REQUESTS -> "Too many attempts. Wait a while and try again."
    AuthError.PROFILE_MISSING ->
        "Account Not Configured. Ask your administrator to create your student profile."
    AuthError.PROFILE_INCOMPLETE ->
        "Account Not Configured. The student profile is incomplete."
    AuthError.UNAUTHORIZED_ROLE -> "This account is not authorized for the student app."
    AuthError.PERMISSION_DENIED -> "Your student profile could not be authorized."
    AuthError.SERVICE_UNAVAILABLE -> "The authentication service is temporarily unavailable. Try again."
    AuthError.UNKNOWN -> "Something went wrong. Please try again."
}
