package com.example.parkkar.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.parkkar.model.Booking
import com.example.parkkar.model.FavoriteSpot
import com.example.parkkar.model.UserDetails
import com.example.parkkar.utils.sha256
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_VERSION = 4 // Incremented version
        private const val DATABASE_NAME = "Parkkar.db"

        // User Details Table
        private const val TABLE_USER_DETAILS = "user_details"
        private const val KEY_ID = "id"
        private const val KEY_EMAIL = "email"
        private const val KEY_PHONE = "phone"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_CREATED_AT_DATE = "created_at_date"
        private const val KEY_CREATED_AT_TIMESTAMP = "created_at_timestamp"

        // Log Details Table
        private const val TABLE_LOG_DETAILS = "log_details"
        private const val COLUMN_LOG_ID = "log_id"
        private const val COLUMN_LOG_USER_ID = "user_id"
        private const val COLUMN_LOG_ACTIVITY = "activity"
        private const val COLUMN_LOG_TIMESTAMP = "timestamp"

        // Booking History Table
        private const val TABLE_BOOKING_HISTORY = "booking_history"
        private const val KEY_BOOKING_ID = "booking_id"
        private const val KEY_BOOKING_USER_ID = "user_id"
        private const val KEY_PARKING_NAME = "parking_name"
        private const val KEY_ADDRESS = "address"
        private const val KEY_BOOKING_DATE = "booking_date"
        private const val KEY_BOOKING_TIME = "booking_time"
        private const val KEY_DURATION = "duration"
        private const val KEY_TOTAL_COST = "total_cost"
        private const val KEY_STATUS = "status"

        // Favorite Spots Table
        private const val TABLE_FAVORITE_SPOTS = "favorite_spots"
        private const val KEY_FAVORITE_ID = "favorite_id"
        private const val KEY_FAVORITE_USER_ID = "user_id"
        private const val KEY_FAVORITE_PARKING_ID = "parking_id"
        private const val KEY_FAVORITE_PARKING_NAME = "parking_name"
        private const val KEY_FAVORITE_ADDRESS = "address"
        private const val KEY_LATITUDE = "latitude"
        private const val KEY_LONGITUDE = "longitude"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createUserTable = ("CREATE TABLE " + TABLE_USER_DETAILS + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_EMAIL + " TEXT UNIQUE,"
                + KEY_PHONE + " TEXT,"
                + KEY_FULL_NAME + " TEXT,"
                + KEY_USERNAME + " TEXT UNIQUE,"
                + KEY_PASSWORD_HASH + " TEXT,"
                + KEY_CREATED_AT_DATE + " TEXT,"
                + KEY_CREATED_AT_TIMESTAMP + " TEXT" + ")")
        db?.execSQL(createUserTable)

    // ... (rest of onCreate is the same)
    // The other tables (log, booking, favorite) are created here as well
        val createLogTable = ("CREATE TABLE " + TABLE_LOG_DETAILS + "("
                + COLUMN_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_LOG_USER_ID + " INTEGER,"
                + COLUMN_LOG_ACTIVITY + " TEXT,"
                + COLUMN_LOG_TIMESTAMP + " TEXT" + ")")
        db?.execSQL(createLogTable)

        val createBookingHistoryTable = ("CREATE TABLE " + TABLE_BOOKING_HISTORY + "("
                + KEY_BOOKING_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_BOOKING_USER_ID + " INTEGER,"
                + KEY_PARKING_NAME + " TEXT,"
                + KEY_ADDRESS + " TEXT,"
                + KEY_BOOKING_DATE + " TEXT,"
                + KEY_BOOKING_TIME + " TEXT,"
                + KEY_DURATION + " INTEGER,"
                + KEY_TOTAL_COST + " REAL,"
                + KEY_STATUS + " TEXT" + ")")
        db?.execSQL(createBookingHistoryTable)

        val createFavoriteSpotsTable = ("CREATE TABLE " + TABLE_FAVORITE_SPOTS + "("
                + KEY_FAVORITE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_FAVORITE_USER_ID + " INTEGER,"
                + KEY_FAVORITE_PARKING_ID + " TEXT,"
                + KEY_FAVORITE_PARKING_NAME + " TEXT,"
                + KEY_FAVORITE_ADDRESS + " TEXT,"
                + KEY_LATITUDE + " REAL,"
                + KEY_LONGITUDE + " REAL" + ")")
        db?.execSQL(createFavoriteSpotsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USER_DETAILS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_LOG_DETAILS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKING_HISTORY")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_FAVORITE_SPOTS")
        onCreate(db)
    }

    fun addUser(email: String, phone: String, fullName: String, username: String, passwordHash: String, date: String, timestamp: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_EMAIL, email)
            put(KEY_PHONE, phone)
            put(KEY_FULL_NAME, fullName)
            put(KEY_USERNAME, username)
            put(KEY_PASSWORD_HASH, passwordHash)
            put(KEY_CREATED_AT_DATE, date)
            put(KEY_CREATED_AT_TIMESTAMP, timestamp)
        }
        val id = db.insert(TABLE_USER_DETAILS, null, values)
        return id
    }

    fun checkUserExists(username: String, email: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT $KEY_ID FROM $TABLE_USER_DETAILS WHERE $KEY_USERNAME = ? OR $KEY_EMAIL = ?"
        val cursor = db.rawQuery(query, arrayOf(username, email))
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
            "$KEY_USERNAME = ? OR $KEY_EMAIL = ?",
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

    fun getUserDetails(usernameOrEmail: String): UserDetails? {
        val db = this.readableDatabase
        var userDetails: UserDetails? = null
        val cursor = db.query(
            TABLE_USER_DETAILS,
            arrayOf(KEY_FULL_NAME, KEY_USERNAME, KEY_EMAIL, KEY_PHONE),
            "$KEY_USERNAME = ? OR $KEY_EMAIL = ?",
            arrayOf(usernameOrEmail, usernameOrEmail),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                userDetails = UserDetails(
                    name = it.getString(it.getColumnIndexOrThrow(KEY_FULL_NAME)),
                    username = it.getString(it.getColumnIndexOrThrow(KEY_USERNAME)),
                    email = it.getString(it.getColumnIndexOrThrow(KEY_EMAIL)),
                    phone = it.getString(it.getColumnIndexOrThrow(KEY_PHONE)) ?: ""
                )
            }
        }
        return userDetails
    }

    fun updateUserDetails(userId: Int, name: String, username: String, email: String, phone: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_FULL_NAME, name)
            put(KEY_USERNAME, username)
            put(KEY_EMAIL, email)
            put(KEY_PHONE, phone)
        }
        val rowsAffected = db.update(TABLE_USER_DETAILS, values, "$KEY_ID = ?", arrayOf(userId.toString()))
        return rowsAffected > 0
    }

    fun getUserIdByUsername(usernameOrEmail: String): Int? {
        val db = this.readableDatabase
        var userId: Int? = null
        val cursor = db.query(
            TABLE_USER_DETAILS,
            arrayOf(KEY_ID),
            "$KEY_USERNAME = ? OR $KEY_EMAIL = ?",
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

    // ... (rest of the file is the same)
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

    fun addBooking(booking: Booking): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_BOOKING_USER_ID, booking.userId)
            put(KEY_PARKING_NAME, booking.parkingName)
            put(KEY_ADDRESS, booking.address)
            put(KEY_BOOKING_DATE, booking.bookingDate)
            put(KEY_BOOKING_TIME, booking.bookingTime)
            put(KEY_DURATION, booking.duration)
            put(KEY_TOTAL_COST, booking.totalCost)
            put(KEY_STATUS, booking.status)
        }
        return db.insert(TABLE_BOOKING_HISTORY, null, values)
    }

    fun getBookings(userId: Int): List<Booking> {
        val bookings = mutableListOf<Booking>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_BOOKING_HISTORY,
            null,
            "$KEY_BOOKING_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null, "$KEY_BOOKING_DATE DESC, $KEY_BOOKING_TIME DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                bookings.add(
                    Booking(
                        id = it.getInt(it.getColumnIndexOrThrow(KEY_BOOKING_ID)),
                        userId = it.getInt(it.getColumnIndexOrThrow(KEY_BOOKING_USER_ID)),
                        parkingName = it.getString(it.getColumnIndexOrThrow(KEY_PARKING_NAME)),
                        address = it.getString(it.getColumnIndexOrThrow(KEY_ADDRESS)),
                        bookingDate = it.getString(it.getColumnIndexOrThrow(KEY_BOOKING_DATE)),
                        bookingTime = it.getString(it.getColumnIndexOrThrow(KEY_BOOKING_TIME)),
                        duration = it.getInt(it.getColumnIndexOrThrow(KEY_DURATION)),
                        totalCost = it.getDouble(it.getColumnIndexOrThrow(KEY_TOTAL_COST)),
                        status = it.getString(it.getColumnIndexOrThrow(KEY_STATUS))
                    )
                )
            }
        }
        return bookings
    }

    fun addFavoriteSpot(favoriteSpot: FavoriteSpot): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_FAVORITE_USER_ID, favoriteSpot.userId)
            put(KEY_FAVORITE_PARKING_ID, favoriteSpot.parkingId)
            put(KEY_FAVORITE_PARKING_NAME, favoriteSpot.parkingName)
            put(KEY_FAVORITE_ADDRESS, favoriteSpot.address)
            put(KEY_LATITUDE, favoriteSpot.latitude)
            put(KEY_LONGITUDE, favoriteSpot.longitude)
        }
        return db.insert(TABLE_FAVORITE_SPOTS, null, values)
    }

    fun getFavoriteSpots(userId: Int): List<FavoriteSpot> {
        val favoriteSpots = mutableListOf<FavoriteSpot>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_FAVORITE_SPOTS,
            null,
            "$KEY_FAVORITE_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null, null
        )
        cursor.use {
            while (it.moveToNext()) {
                favoriteSpots.add(
                    FavoriteSpot(
                        id = it.getInt(it.getColumnIndexOrThrow(KEY_FAVORITE_ID)),
                        userId = it.getInt(it.getColumnIndexOrThrow(KEY_FAVORITE_USER_ID)),
                        parkingId = it.getString(it.getColumnIndexOrThrow(KEY_FAVORITE_PARKING_ID)),
                        parkingName = it.getString(it.getColumnIndexOrThrow(KEY_FAVORITE_PARKING_NAME)),
                        address = it.getString(it.getColumnIndexOrThrow(KEY_FAVORITE_ADDRESS)),
                        latitude = it.getDouble(it.getColumnIndexOrThrow(KEY_LATITUDE)),
                        longitude = it.getDouble(it.getColumnIndexOrThrow(KEY_LONGITUDE))
                    )
                )
            }
        }
        return favoriteSpots
    }

    fun updatePasswordByUsernameOrEmail(usernameOrEmail: String, newPasswordHash: String): Boolean {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_PASSWORD_HASH, newPasswordHash)
        }
        val rowsAffected = db.update(
            TABLE_USER_DETAILS,
            values,
            "$KEY_USERNAME = ? OR $KEY_EMAIL = ?",
            arrayOf(usernameOrEmail, usernameOrEmail)
        )
        return rowsAffected > 0
    }
}
