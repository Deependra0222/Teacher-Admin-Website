package com.smartattend.app.data.firebase

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.smartattend.app.domain.auth.AuthError

object FirebaseErrorMapper {
    fun mapAuthCode(code: String?): AuthError = when (code) {
        "ERROR_INVALID_EMAIL" -> AuthError.INVALID_EMAIL
        "ERROR_INVALID_CREDENTIAL",
        "ERROR_WRONG_PASSWORD",
        "ERROR_USER_NOT_FOUND",
        "ERROR_EMAIL_ALREADY_IN_USE",
        -> AuthError.INVALID_CREDENTIALS
        "ERROR_USER_DISABLED" -> AuthError.ACCOUNT_DISABLED
        "ERROR_NETWORK_REQUEST_FAILED" -> AuthError.NETWORK
        "ERROR_TOO_MANY_REQUESTS" -> AuthError.TOO_MANY_REQUESTS
        else -> AuthError.UNKNOWN
    }

    fun mapFirestoreCode(code: String?): AuthError = when (code) {
        "PERMISSION_DENIED",
        "UNAUTHENTICATED",
        -> AuthError.PERMISSION_DENIED
        "UNAVAILABLE",
        "DEADLINE_EXCEEDED",
        -> AuthError.SERVICE_UNAVAILABLE
        else -> AuthError.UNKNOWN
    }

    fun mapThrowable(throwable: Throwable): AuthError {
        val causes = generateSequence(throwable as Throwable?) { it.cause }
        causes.filterIsInstance<FirebaseNetworkException>().firstOrNull()?.let {
            return AuthError.NETWORK
        }
        causes.filterIsInstance<FirebaseAuthException>().firstOrNull()?.let {
            return mapAuthCode(it.errorCode)
        }
        causes.filterIsInstance<FirebaseFirestoreException>().firstOrNull()?.let {
            return mapFirestoreCode(it.code.name)
        }
        return AuthError.UNKNOWN
    }
}
