package com.example.parkkar.utils

import android.content.Context
import android.widget.Toast
import java.security.MessageDigest

fun sha256(input: String): String {
    return try {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        bytes.joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        // Handle error appropriately in a real app, e.g., log it or throw a custom exception
        e.printStackTrace()
        ""
    }
}

fun isValidPassword(password: String): Boolean {
    if (password.length < 6) return false
    if (!password.any { it.isDigit() }) return false
    // Add more constraints if needed (e.g., uppercase, special character)
    return true
}

fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
