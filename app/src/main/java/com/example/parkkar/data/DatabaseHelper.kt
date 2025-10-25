package com.example.parkkar.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.parkkar.utils.sha256
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * This DatabaseHelper is SOLELY responsible for User and Log data, based on the
 * exact logic used in LoginActivity, SignUpActivity, and ForgotPasswordActivity.
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "ParkkarUser.db"

        // User Details Table as required by your Activities
        private const val TABLE_USER_DETAILS = "user_details"
        private const val KEY_ID = "id"
        private const val KEY_EMAIL_OR_PHONE = "email_or_phone"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_CREATED_AT_DATE = "created_at_date"
        private const val KEY_CREATED_AT_TIMESTAMP = "created_at_timestamp"

        // Log Details Table as required by your Activities
        private const val TABLE_LOG_DETAILS = "log_details"
        private const val COLUMN_LOG_ID = "log_id"
        private const val COLUMN_LOG_USER_ID = "user_id"
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
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USER_DETAILS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_LOG_DETAILS")
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
        // Not closing db here as it can cause issues if another operation is performed soon after.
        return id
    }

    fun checkUserExists(username: String, emailOrPhone: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT $KEY_ID FROM $TABLE_USER_DETAILS WHERE $KEY_USERNAME = ? OR $KEY_EMAIL_OR_PHONE = ?"
        val cursor = db.rawQuery(query, arrayOf(username, emailOrPhone))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun checkUserCredentials(usernameOrEmail: String, passwordAttempt: String): Boolean {
        val db = this.readableDatabase
        var userPasswordHash: String? = null
        val cursor = db.query(
            TABLE_USER_DETAILS,
            arrayOf(KEY_PASSWORD_HASH),
            "$KEY_USERNAME = ? OR $KEY_EMAIL_OR_PHONE = ?",
            arrayOf(usernameOrEmail, usernameOrEmail),
            null, null, null
        )
        cursor.use { 
            if (it.moveToFirst()) {
                userPasswordHash = it.getString(it.getColumnIndexOrThrow(KEY_PASSWORD_HASH))
            }
        }
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
        cursor.use { 
            if (it.moveToFirst()) {
                userId = it.getInt(it.getColumnIndexOrThrow(KEY_ID))
            }
        }
        return userId
    }

    fun addLogEntry(userId: Int?, activity: String) {
        val db = this.writableDatabase
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val values = ContentValues().apply {
            if (userId != null) {
                put(COLUMN_LOG_USER_ID, userId)
            } else {
                putNull(COLUMN_LOG_USER_ID)
            }
            put(COLUMN_LOG_ACTIVITY, activity)
            put(COLUMN_LOG_TIMESTAMP, timestamp)
        }
        db.insert(TABLE_LOG_DETAILS, null, values)
    }
}
