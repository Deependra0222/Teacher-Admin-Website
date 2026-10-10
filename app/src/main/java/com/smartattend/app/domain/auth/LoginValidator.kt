package com.smartattend.app.domain.auth

object LoginValidator {
    fun validate(identifier: String, password: String): String? = when {
        identifier.isBlank() -> "Enter your student ID or email"
        password.length < 4 -> "Demo password must be at least 4 characters"
        else -> null
    }
}

