package com.example.parkkar.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.parkkar.utils.sha256 // Assuming Crypto.kt is in this path
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 2 // Incremented for schema change
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

        // Log Details Table
        private const val TABLE_LOG_DETAILS = "log_details"
        private const val COLUMN_LOG_ID = "log_id"
        private const val COLUMN_LOG_USER_ID = "user_id" // Nullable, as failed login might not have user_id
        private const val COLUMN_LOG_ACTIVITY = "activity"
        private const val COLUMN_LOG_TIMESTAMP = "timestamp"
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

        val createLogTable = ("CREATE TABLE " + TABLE_LOG_DETAILS + "("
                + COLUMN_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_LOG_USER_ID + " INTEGER,"
                + COLUMN_LOG_ACTIVITY + " TEXT,"
                + COLUMN_LOG_TIMESTAMP + " TEXT" + ")")
        db?.execSQL(createLogTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) { // Check if upgrading from a version before log_details table
            val createLogTable = ("CREATE TABLE " + TABLE_LOG_DETAILS + "("
                    + COLUMN_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_LOG_USER_ID + " INTEGER,"
                    + COLUMN_LOG_ACTIVITY + " TEXT,"
                    + COLUMN_LOG_TIMESTAMP + " TEXT" + ")")
            db?.execSQL(createLogTable)
        }
        // Simple upgrade: drop older tables and recreate. 
        // For production, a migration strategy would be better.
        // db?.execSQL("DROP TABLE IF EXISTS $TABLE_USER_DETAILS") // Only drop if structure changes
        // db?.execSQL("DROP TABLE IF EXISTS $TABLE_LOG_DETAILS") // Only drop if structure changes
        // onCreate(db) // Then recreate. For this change, only adding a table is fine.
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
        // db.close() // Avoid closing db here if used frequently by other methods
        return id
    }

    fun checkUserExists(username: String, emailOrPhone: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT $KEY_ID FROM $TABLE_USER_DETAILS WHERE $KEY_USERNAME = ? OR $KEY_EMAIL_OR_PHONE = ?"
        val cursor = db.rawQuery(query, arrayOf(username, emailOrPhone))
        val exists = cursor.count > 0
        cursor.close()
        // db.close()
        return exists
    }

    fun checkUserCredentials(usernameOrEmail: String, passwordAttempt: String): Boolean {
        val db = this.readableDatabase
        var userPasswordHash: String? = null

        var cursor = db.query(
            TABLE_USER_DETAILS,
            arrayOf(KEY_PASSWORD_HASH),
            "$KEY_USERNAME = ? OR $KEY_EMAIL_OR_PHONE = ?", // Check against both username and email/phone
            arrayOf(usernameOrEmail, usernameOrEmail),
            null, null, null
        )
        if (cursor.moveToFirst()) {
            userPasswordHash = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD_HASH))
        }
        cursor.close()
        // db.close()

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
        val selection = "$KEY_USERNAME = ? OR $KEY_EMAIL_OR_PHONE = ?"
        val selectionArgs = arrayOf(usernameOrEmail, usernameOrEmail)
        val rowsAffected = db.update(TABLE_USER_DETAILS, values, selection, selectionArgs)
        // db.close()
        return rowsAffected > 0
    }

    fun getUserIdByUsername(usernameOrEmail: String): Int? {
        val db = this.readableDatabase
        var userId: Int? = null
        val cursor = db.query(
            TABLE_USER_DETAILS,
            arrayOf(KEY_ID),
            "$KEY_USERNAME = ? OR $KEY_EMAIL_OR_PHONE = ?",
            arrayOf(usernameOrEmail, usernameOrEmail),
            null, null, null
        )
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID))
        }
        cursor.close()
        // db.close()
        return userId
    }

    fun addLogEntry(userId: Int?, activity: String) {
        val db = this.writableDatabase
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val values = ContentValues().apply {
            if (userId != null) {
                put(COLUMN_LOG_USER_ID, userId)
            } else {
                putNull(COLUMN_LOG_USER_ID) // Explicitly put null if userId is null
            }
            put(COLUMN_LOG_ACTIVITY, activity)
            put(COLUMN_LOG_TIMESTAMP, timestamp)
        }
        db.insert(TABLE_LOG_DETAILS, null, values)
        // db.close() // Keep db open if multiple operations are frequent
    }
}
