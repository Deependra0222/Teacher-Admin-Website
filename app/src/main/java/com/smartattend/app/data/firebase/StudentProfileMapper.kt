package com.smartattend.app.data.firebase

import com.smartattend.app.core.model.Student
import com.smartattend.app.domain.auth.AuthError
import com.smartattend.app.domain.auth.AuthenticationResult

object StudentProfileMapper {
    fun map(uid: String, data: Map<String, Any?>?): AuthenticationResult {
        if (data == null) return AuthenticationResult.Failure(AuthError.PROFILE_MISSING)

        val role = data.requiredString("role")
            ?: return AuthenticationResult.Failure(AuthError.PROFILE_INCOMPLETE)
        if (role != "student") {
            return AuthenticationResult.Failure(AuthError.UNAUTHORIZED_ROLE)
        }

        val name = data.requiredString("name")
        val email = data.requiredString("email")
        val rollNumber = data.requiredString("rollNumber")
        val department = data.requiredString("department")
        val batch = data.requiredString("batch")
        if (listOf(name, email, rollNumber, department, batch).any { it == null }) {
            return AuthenticationResult.Failure(AuthError.PROFILE_INCOMPLETE)
        }

        return AuthenticationResult.Authorized(
            Student(
                id = uid,
                name = name!!,
                rollNumber = rollNumber!!,
                email = email!!,
                department = department!!,
                batch = batch!!,
            ),
        )
    }

    private fun Map<String, Any?>.requiredString(field: String): String? =
        (get(field) as? String)?.trim()?.takeIf { it.isNotEmpty() }
}
