package com.smartattend.app.domain.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoginValidatorTest {
    @Test
    fun blankIdentifierIsRejected() {
        assertEquals("Enter your student ID or email", LoginValidator.validate("   ", "demo123"))
    }

    @Test
    fun shortPasswordIsRejected() {
        assertEquals("Demo password must be at least 4 characters", LoginValidator.validate("CS-042", "123"))
    }

    @Test
    fun validDemoCredentialsAreAccepted() {
        assertNull(LoginValidator.validate("CS-042", "demo123"))
    }
}

