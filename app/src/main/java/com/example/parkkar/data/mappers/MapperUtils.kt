package com.example.parkkar.data.mappers

import java.util.Locale

/**
 * Creates a safe, URL-friendly ID from a string.
 * This is used to generate deterministic and unique IDs for parking spots.
 */
fun String.toSafeId(): String {
    return this.lowercase(Locale.getDefault())
        .replace(Regex("[^a-z0-9\\s]"), "") // Remove non-alphanumeric chars except space
        .trim()
        .replace(Regex("\\s+"), "_") // Replace multiple spaces/whitespace with a single underscore
}
