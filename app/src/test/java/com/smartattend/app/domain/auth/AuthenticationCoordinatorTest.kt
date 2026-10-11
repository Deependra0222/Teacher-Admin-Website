package com.smartattend.app.domain.auth

import com.smartattend.app.core.model.Student
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthenticationCoordinatorTest {
    private val demoStudent = student("demo-id", "Demo Student")
    private val firebaseStudent = student("firebase-uid", "Firebase Student")

    @Test
    fun demoModeRequiresExplicitEntry() = runTest {
        val repository = FakeAuthenticationRepository(restoreResult = AuthenticationResult.NoSession)
        val store = FakeSessionStore(false)
        val coordinator = AuthenticationCoordinator(repository, store, demoStudent)

        assertEquals(AuthSessionMode.SIGNED_OUT, coordinator.restore().mode)

        val entered = coordinator.enterDemoMode()
        assertEquals(AuthSessionMode.DEMO, entered.mode)
        assertEquals(demoStudent, entered.student)
        assertTrue(store.demoEnabled)
        assertEquals(1, repository.signOutCalls)
    }

    @Test
    fun failedFirebaseLoginClearsDemoAndNeverFallsBack() = runTest {
        val repository = FakeAuthenticationRepository(
            signInResult = AuthenticationResult.Failure(AuthError.INVALID_CREDENTIALS),
        )
        val store = FakeSessionStore(true)
        val coordinator = AuthenticationCoordinator(repository, store, demoStudent)

        val state = coordinator.signIn("student@example.edu", "wrong password")

        assertEquals(AuthSessionMode.SIGNED_OUT, state.mode)
        assertEquals(AuthError.INVALID_CREDENTIALS, state.error)
        assertNull(state.student)
        assertFalse(store.demoEnabled)
    }

    @Test
    fun successfulFirebaseLoginClearsStaleDemoMarker() = runTest {
        val repository = FakeAuthenticationRepository(
            signInResult = AuthenticationResult.Authorized(firebaseStudent),
        )
        val store = FakeSessionStore(true)
        val coordinator = AuthenticationCoordinator(repository, store, demoStudent)

        val state = coordinator.signIn("student@example.edu", "valid password")

        assertEquals(AuthSessionMode.FIREBASE, state.mode)
        assertEquals(firebaseStudent, state.student)
        assertFalse(store.demoEnabled)
    }

    @Test
    fun restoredFirebaseUserWinsOverStaleDemoMarker() = runTest {
        val repository = FakeAuthenticationRepository(
            restoreResult = AuthenticationResult.Authorized(firebaseStudent),
        )
        val store = FakeSessionStore(true)
        val coordinator = AuthenticationCoordinator(repository, store, demoStudent)

        val state = coordinator.restore()

        assertEquals(AuthSessionMode.FIREBASE, state.mode)
        assertEquals(firebaseStudent, state.student)
        assertFalse(store.demoEnabled)
    }

    @Test
    fun demoRestoresOnlyWhenFirebaseHasNoSessionAndMarkerIsSet() = runTest {
        val repository = FakeAuthenticationRepository(restoreResult = AuthenticationResult.NoSession)
        val store = FakeSessionStore(true)
        val coordinator = AuthenticationCoordinator(repository, store, demoStudent)

        val state = coordinator.restore()

        assertEquals(AuthSessionMode.DEMO, state.mode)
        assertEquals(demoStudent, state.student)
    }

    @Test
    fun restorationFailuresRemainSignedOut() = runTest {
        listOf(
            AuthError.PROFILE_MISSING,
            AuthError.UNAUTHORIZED_ROLE,
            AuthError.NETWORK,
        ).forEach { error ->
            val repository = FakeAuthenticationRepository(
                restoreResult = AuthenticationResult.Failure(error),
            )
            val store = FakeSessionStore(true)
            val state = AuthenticationCoordinator(repository, store, demoStudent).restore()

            assertEquals(AuthSessionMode.SIGNED_OUT, state.mode)
            assertEquals(error, state.error)
            assertNull(state.student)
            assertFalse(store.demoEnabled)
        }
    }

    @Test
    fun logoutSignsOutFirebaseClearsDemoAndDropsProfile() = runTest {
        val repository = FakeAuthenticationRepository(
            signInResult = AuthenticationResult.Authorized(firebaseStudent),
        )
        val store = FakeSessionStore(true)
        val coordinator = AuthenticationCoordinator(repository, store, demoStudent)
        assertEquals(AuthSessionMode.FIREBASE, coordinator.signIn("student@example.edu", "valid").mode)

        val state = coordinator.logout()

        assertEquals(AuthSessionMode.SIGNED_OUT, state.mode)
        assertNull(state.student)
        assertFalse(store.demoEnabled)
        assertEquals(1, repository.signOutCalls)
    }

    @Test
    fun passwordResetValidatesEmailBeforeCallingRepository() = runTest {
        val repository = FakeAuthenticationRepository()
        val coordinator = AuthenticationCoordinator(repository, FakeSessionStore(false), demoStudent)

        assertEquals(
            PasswordResetResult.Failure(AuthError.INVALID_EMAIL),
            coordinator.sendPasswordReset("not-an-email"),
        )
        assertNull(repository.lastResetEmail)
    }

    @Test
    fun passwordResetReturnsRepositorySuccessAndFailure() = runTest {
        val repository = FakeAuthenticationRepository(resetResult = PasswordResetResult.Sent)
        val coordinator = AuthenticationCoordinator(repository, FakeSessionStore(false), demoStudent)

        assertEquals(PasswordResetResult.Sent, coordinator.sendPasswordReset("student@example.edu"))
        assertEquals("student@example.edu", repository.lastResetEmail)

        repository.resetResult = PasswordResetResult.Failure(AuthError.NETWORK)
        assertEquals(
            PasswordResetResult.Failure(AuthError.NETWORK),
            coordinator.sendPasswordReset("student@example.edu"),
        )
    }

    @Test
    fun invalidLocalLoginNeverCallsFirebase() = runTest {
        val repository = FakeAuthenticationRepository()
        val store = FakeSessionStore(true)
        val coordinator = AuthenticationCoordinator(repository, store, demoStudent)

        val state = coordinator.signIn("bad-email", "password")

        assertEquals(AuthError.INVALID_EMAIL, state.error)
        assertEquals(0, repository.signInCalls)
        assertFalse(store.demoEnabled)
    }

    private fun student(id: String, name: String) = Student(
        id = id,
        name = name,
        rollNumber = "CS-042",
        email = "student@example.edu",
        department = "Computer Science",
        batch = "2026-A",
    )

    private class FakeSessionStore(initialValue: Boolean) : AuthenticationSessionStore {
        var demoEnabled = initialValue

        override suspend fun isDemoModeEnabled(): Boolean = demoEnabled

        override suspend fun setDemoModeEnabled(enabled: Boolean) {
            demoEnabled = enabled
        }
    }

    private class FakeAuthenticationRepository(
        var signInResult: AuthenticationResult = AuthenticationResult.NoSession,
        var restoreResult: AuthenticationResult = AuthenticationResult.NoSession,
        var resetResult: PasswordResetResult = PasswordResetResult.Sent,
    ) : AuthenticationRepository {
        var signInCalls = 0
        var signOutCalls = 0
        var lastResetEmail: String? = null

        override suspend fun signIn(email: String, password: String): AuthenticationResult {
            signInCalls += 1
            return signInResult
        }

        override suspend fun restoreSession(): AuthenticationResult = restoreResult

        override suspend fun signOut() {
            signOutCalls += 1
        }

        override suspend fun sendPasswordReset(email: String): PasswordResetResult {
            lastResetEmail = email
            return resetResult
        }
    }
}
