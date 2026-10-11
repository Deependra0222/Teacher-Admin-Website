package com.smartattend.app.data.firebase

import com.smartattend.app.domain.auth.AuthError
import com.smartattend.app.domain.auth.PasswordResetResult
import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseErrorMapperTest {
    @Test
    fun invalidEmailCodeMapsToInvalidEmail() {
        assertEquals(
            AuthError.INVALID_EMAIL,
            FirebaseErrorMapper.mapAuthCode("ERROR_INVALID_EMAIL"),
        )
    }

    @Test
    fun credentialCodesMapToOneNonEnumeratingError() {
        listOf(
            "ERROR_INVALID_CREDENTIAL",
            "ERROR_WRONG_PASSWORD",
            "ERROR_USER_NOT_FOUND",
        ).forEach { code ->
            assertEquals(AuthError.INVALID_CREDENTIALS, FirebaseErrorMapper.mapAuthCode(code))
        }
    }

    @Test
    fun disabledNetworkAndRateLimitCodesMapPrecisely() {
        assertEquals(
            AuthError.ACCOUNT_DISABLED,
            FirebaseErrorMapper.mapAuthCode("ERROR_USER_DISABLED"),
        )
        assertEquals(
            AuthError.NETWORK,
            FirebaseErrorMapper.mapAuthCode("ERROR_NETWORK_REQUEST_FAILED"),
        )
        assertEquals(
            AuthError.TOO_MANY_REQUESTS,
            FirebaseErrorMapper.mapAuthCode("ERROR_TOO_MANY_REQUESTS"),
        )
    }

    @Test
    fun firestorePermissionErrorsDoNotExposeSdkDetails() {
        assertEquals(
            AuthError.PERMISSION_DENIED,
            FirebaseErrorMapper.mapFirestoreCode("PERMISSION_DENIED"),
        )
        assertEquals(
            AuthError.PERMISSION_DENIED,
            FirebaseErrorMapper.mapFirestoreCode("UNAUTHENTICATED"),
        )
    }

    @Test
    fun firestoreUnavailableMapsToServiceUnavailable() {
        assertEquals(
            AuthError.SERVICE_UNAVAILABLE,
            FirebaseErrorMapper.mapFirestoreCode("UNAVAILABLE"),
        )
    }

    @Test
    fun unknownCodesMapToGenericError() {
        assertEquals(AuthError.UNKNOWN, FirebaseErrorMapper.mapAuthCode("SECRET_BACKEND_DETAIL"))
        assertEquals(
            AuthError.UNKNOWN,
            FirebaseErrorMapper.mapFirestoreCode("INTERNAL"),
        )
    }

    @Test
    fun passwordResetDoesNotRevealWhetherAccountExists() {
        assertEquals(
            PasswordResetResult.Sent,
            FirebaseErrorMapper.mapPasswordResetAuthCode("ERROR_USER_NOT_FOUND"),
        )
        assertEquals(
            PasswordResetResult.Sent,
            FirebaseErrorMapper.mapPasswordResetAuthCode("ERROR_INVALID_CREDENTIAL"),
        )
        assertEquals(
            PasswordResetResult.Sent,
            FirebaseErrorMapper.mapPasswordResetAuthCode("ERROR_USER_DISABLED"),
        )
    }

    @Test
    fun passwordResetStillReportsActionableFailures() {
        assertEquals(
            PasswordResetResult.Failure(AuthError.TOO_MANY_REQUESTS),
            FirebaseErrorMapper.mapPasswordResetAuthCode("ERROR_TOO_MANY_REQUESTS"),
        )
        assertEquals(
            PasswordResetResult.Failure(AuthError.NETWORK),
            FirebaseErrorMapper.mapPasswordResetAuthCode("ERROR_NETWORK_REQUEST_FAILED"),
        )
    }
}
