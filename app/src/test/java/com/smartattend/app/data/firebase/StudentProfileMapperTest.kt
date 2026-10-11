package com.smartattend.app.data.firebase

import com.smartattend.app.domain.auth.AuthError
import com.smartattend.app.domain.auth.AuthenticationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudentProfileMapperTest {
    private val validProfile = mapOf<String, Any?>(
        "id" to "client-controlled-id",
        "role" to "student",
        "name" to "Aarav Sharma",
        "email" to "aarav@example.edu",
        "rollNumber" to "CS-042",
        "department" to "Computer Science",
        "batch" to "2026-A",
    )

    @Test
    fun validStudentProfileUsesAuthenticatedUid() {
        val result = StudentProfileMapper.map("firebase-uid", validProfile)

        assertTrue(result is AuthenticationResult.Authorized)
        val student = (result as AuthenticationResult.Authorized).student
        assertEquals("firebase-uid", student.id)
        assertEquals("Aarav Sharma", student.name)
        assertEquals("aarav@example.edu", student.email)
        assertEquals("CS-042", student.rollNumber)
        assertEquals("Computer Science", student.department)
        assertEquals("2026-A", student.batch)
    }

    @Test
    fun missingDocumentIsAccountNotConfigured() {
        assertFailure(null, AuthError.PROFILE_MISSING)
    }

    @Test
    fun missingRequiredFieldIsIncomplete() {
        assertFailure(validProfile - "department", AuthError.PROFILE_INCOMPLETE)
    }

    @Test
    fun blankRequiredFieldIsIncomplete() {
        assertFailure(validProfile + ("rollNumber" to "   "), AuthError.PROFILE_INCOMPLETE)
    }

    @Test
    fun teacherRoleIsRejected() {
        assertFailure(validProfile + ("role" to "teacher"), AuthError.UNAUTHORIZED_ROLE)
    }

    @Test
    fun adminRoleIsRejected() {
        assertFailure(validProfile + ("role" to "admin"), AuthError.UNAUTHORIZED_ROLE)
    }

    @Test
    fun unknownRoleIsRejected() {
        assertFailure(validProfile + ("role" to "observer"), AuthError.UNAUTHORIZED_ROLE)
    }

    private fun assertFailure(profile: Map<String, Any?>?, expected: AuthError) {
        assertEquals(
            AuthenticationResult.Failure(expected),
            StudentProfileMapper.map("firebase-uid", profile),
        )
    }
}
