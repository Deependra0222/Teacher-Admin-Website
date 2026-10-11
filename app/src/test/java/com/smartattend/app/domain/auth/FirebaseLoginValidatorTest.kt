package com.smartattend.app.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FirebaseLoginValidatorTest {
    @Test
    fun validEmailAndPasswordAreAccepted() {
        assertNull(LoginValidator.validateFirebase("student@example.edu", "correct horse"))
    }

    @Test
    fun blankEmailIsRejected() {
        assertEquals(
            AuthError.INVALID_EMAIL,
            LoginValidator.validateFirebase("   ", "correct horse"),
        )
    }

    @Test
    fun malformedEmailIsRejected() {
        assertEquals(
            AuthError.INVALID_EMAIL,
            LoginValidator.validateFirebase("student.example.edu", "correct horse"),
        )
    }

    @Test
    fun blankPasswordIsRejected() {
        assertEquals(
            AuthError.PASSWORD_REQUIRED,
            LoginValidator.validateFirebase("student@example.edu", "   "),
        )
    }
}
