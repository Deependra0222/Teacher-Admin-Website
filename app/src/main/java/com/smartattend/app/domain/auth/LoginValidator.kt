package com.smartattend.app.domain.auth

object LoginValidator {
    fun validate(identifier: String, password: String): String? = when {
        identifier.isBlank() -> "Enter your student ID or email"
        password.length < 4 -> "Demo password must be at least 4 characters"
        else -> null
    }

    fun validateFirebase(email: String, password: String): AuthError? = when {
        !email.trim().isPlausibleEmail() -> AuthError.INVALID_EMAIL
        password.isBlank() -> AuthError.PASSWORD_REQUIRED
        else -> null
    }

    private fun String.isPlausibleEmail(): Boolean {
        if (isEmpty() || any(Char::isWhitespace)) return false
        val at = lastIndexOf('@')
        val dot = lastIndexOf('.')
        return at > 0 && dot > at + 1 && dot < lastIndex
    }
}

