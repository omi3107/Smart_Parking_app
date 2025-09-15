package com.example.parkkar.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.parkkar.utils.sha256 // Assuming Crypto.kt is in this path

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "Parkkar.db"
        private const val TABLE_USER_DETAILS = "user_details"

        // User Details Table Columns
        private const val KEY_ID = "id"
        private const val KEY_EMAIL_OR_PHONE = "email_or_phone"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_CREATED_AT_DATE = "created_at_date"
        private const val KEY_CREATED_AT_TIMESTAMP = "created_at_timestamp"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createUserTable = ("CREATE TABLE " + TABLE_USER_DETAILS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_EMAIL_OR_PHONE + " TEXT UNIQUE,"
                + KEY_FULL_NAME + " TEXT,"
                + KEY_USERNAME + " TEXT UNIQUE,"
                + KEY_PASSWORD_HASH + " TEXT,"
                + KEY_CREATED_AT_DATE + " TEXT,"
                + KEY_CREATED_AT_TIMESTAMP + " TEXT" + ")")
        db?.execSQL(createUserTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USER_DETAILS")
        onCreate(db)
    }

    fun addUser(emailOrPhone: String, fullName: String, username: String, passwordHash: String, date: String, timestamp: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_EMAIL_OR_PHONE, emailOrPhone)
            put(KEY_FULL_NAME, fullName)
            put(KEY_USERNAME, username)
            put(KEY_PASSWORD_HASH, passwordHash)
            put(KEY_CREATED_AT_DATE, date)
            put(KEY_CREATED_AT_TIMESTAMP, timestamp)
        }
        val id = db.insert(TABLE_USER_DETAILS, null, values)
        db.close()
        return id // Returns -1 if error, otherwise the new row ID
    }

    fun checkUserExists(username: String, emailOrPhone: String): Boolean {
        val db = this.readableDatabase
        // Check if a user exists with EITHER the given username OR the given email/phone.
        // This is suitable for ForgotPasswordActivity to check if the account exists.
        // For SignUpActivity, it's also suitable to prevent duplicate username or email/phone.
        val query = "SELECT $KEY_ID FROM $TABLE_USER_DETAILS WHERE $KEY_USERNAME = ? OR $KEY_EMAIL_OR_PHONE = ?"
        val cursor = db.rawQuery(query, arrayOf(username, emailOrPhone))
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

    fun checkUserCredentials(usernameOrEmail: String, passwordAttempt: String): Boolean {
        val db = this.readableDatabase
        var userPasswordHash: String? = null

        // Try to get user by username first
        var cursor = db.query(
            TABLE_USER_DETAILS,
            arrayOf(KEY_PASSWORD_HASH),
            "$KEY_USERNAME = ?",
            arrayOf(usernameOrEmail),
            null, null, null
        )
        if (cursor.moveToFirst()) {
            userPasswordHash = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD_HASH))
        }
        cursor.close()

        // If not found by username, try by email/phone
        if (userPasswordHash == null) {
            cursor = db.query(
                TABLE_USER_DETAILS,
                arrayOf(KEY_PASSWORD_HASH),
                "$KEY_EMAIL_OR_PHONE = ?",
                arrayOf(usernameOrEmail),
                null, null, null
            )
            if (cursor.moveToFirst()) {
                userPasswordHash = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD_HASH))
            }
            cursor.close()
        }
        db.close()

        return if (userPasswordHash != null) {
            sha256(passwordAttempt) == userPasswordHash
        } else {
            false
        }
    }

    fun updatePasswordByUsernameOrEmail(usernameOrEmail: String, newHashedPassword: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues()
        values.put(KEY_PASSWORD_HASH, newHashedPassword)

        // Update password where username or email/phone matches
        val selection = "$KEY_USERNAME = ? OR $KEY_EMAIL_OR_PHONE = ?"
        val selectionArgs = arrayOf(usernameOrEmail, usernameOrEmail)

        val rowsAffected = db.update(TABLE_USER_DETAILS, values, selection, selectionArgs)
        db.close()
        return rowsAffected > 0 // Returns true if at least one row was updated
    }
}
