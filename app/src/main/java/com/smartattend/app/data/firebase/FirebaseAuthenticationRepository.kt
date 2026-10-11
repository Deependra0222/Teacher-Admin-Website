package com.smartattend.app.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.smartattend.app.domain.auth.AuthError
import com.smartattend.app.domain.auth.AuthenticationRepository
import com.smartattend.app.domain.auth.AuthenticationResult
import com.smartattend.app.domain.auth.PasswordResetResult
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

class FirebaseAuthenticationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : AuthenticationRepository {
    override suspend fun signIn(email: String, password: String): AuthenticationResult = try {
        val user = auth.signInWithEmailAndPassword(email, password).await().user
            ?: auth.currentUser
            ?: return AuthenticationResult.Failure(AuthError.UNKNOWN)
        authorize(user)
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        auth.signOut()
        AuthenticationResult.Failure(FirebaseErrorMapper.mapThrowable(error))
    }

    override suspend fun restoreSession(): AuthenticationResult {
        val user = awaitFirstAuthState() ?: return AuthenticationResult.NoSession
        return authorize(user)
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun sendPasswordReset(email: String): PasswordResetResult = try {
        auth.sendPasswordResetEmail(email).await()
        PasswordResetResult.Sent
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        PasswordResetResult.Failure(FirebaseErrorMapper.mapThrowable(error))
    }

    private suspend fun authorize(user: FirebaseUser): AuthenticationResult = try {
        val snapshot = firestore.collection(USERS_COLLECTION).document(user.uid).get().await()
        val result = StudentProfileMapper.map(
            uid = user.uid,
            data = snapshot.takeIf { it.exists() }?.data,
        )
        if (result !is AuthenticationResult.Authorized) auth.signOut()
        result
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        auth.signOut()
        AuthenticationResult.Failure(FirebaseErrorMapper.mapThrowable(error))
    }

    private suspend fun awaitFirstAuthState(): FirebaseUser? = suspendCancellableCoroutine { continuation ->
        lateinit var listener: FirebaseAuth.AuthStateListener
        listener = FirebaseAuth.AuthStateListener { resolvedAuth ->
            resolvedAuth.removeAuthStateListener(listener)
            if (continuation.isActive) continuation.resume(resolvedAuth.currentUser)
        }
        continuation.invokeOnCancellation { auth.removeAuthStateListener(listener) }
        auth.addAuthStateListener(listener)
    }

    private companion object {
        const val USERS_COLLECTION = "users"
    }
}
