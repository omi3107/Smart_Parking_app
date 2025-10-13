package com.example.parkkar.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.parkkar.data.model.OpeningTimeInfo
import com.example.parkkar.data.model.ParkingSpot
import com.example.parkkar.data.model.PriceInfo
import com.example.parkkar.utils.sha256 
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val gson = Gson()

    companion object {
        private const val DATABASE_VERSION = 3 
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
        private const val COLUMN_LOG_USER_ID = "user_id"
        private const val COLUMN_LOG_ACTIVITY = "activity"
        private const val COLUMN_LOG_TIMESTAMP = "timestamp"

        // Parking Spots Table
        private const val TABLE_PARKING_SPOTS = "parking_spots"
        private const val COLUMN_SPOT_ID = "spot_id"
        private const val COLUMN_CITY_NAME = "city_name"
        private const val COLUMN_PARKING_NAME = "parking_name"
        private const val COLUMN_ADDRESS = "address"
        private const val COLUMN_LATITUDE = "latitude"
        private const val COLUMN_LONGITUDE = "longitude"
        private const val COLUMN_FOUR_WHEELER_SPOTS = "four_wheeler_spots"
        private const val COLUMN_TWO_WHEELER_SPOTS = "two_wheeler_spots"
        private const val COLUMN_ZONE_NAME = "zone_name"
        private const val COLUMN_WARD_NAME = "ward_name"
        private const val COLUMN_PRICES = "prices_json"
        private const val COLUMN_OPENING_TIMES = "opening_times_json"
        private const val COLUMN_COVERAGE_TYPE = "coverage_type"
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

        val createParkingSpotsTable = ("CREATE TABLE " + TABLE_PARKING_SPOTS + "("
                + COLUMN_SPOT_ID + " TEXT PRIMARY KEY,"
                + COLUMN_CITY_NAME + " TEXT NOT NULL,"
                + COLUMN_PARKING_NAME + " TEXT,"
                + COLUMN_ADDRESS + " TEXT,"
                + COLUMN_LATITUDE + " REAL,"
                + COLUMN_LONGITUDE + " REAL,"
                + COLUMN_FOUR_WHEELER_SPOTS + " INTEGER NOT NULL,"
                + COLUMN_TWO_WHEELER_SPOTS + " INTEGER NOT NULL,"
                + COLUMN_ZONE_NAME + " TEXT,"
                + COLUMN_WARD_NAME + " TEXT,"
                + COLUMN_PRICES + " TEXT,"
                + COLUMN_OPENING_TIMES + " TEXT,"
                + COLUMN_COVERAGE_TYPE + " TEXT" + ")")
        db?.execSQL(createParkingSpotsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            val createLogTable = ("CREATE TABLE " + TABLE_LOG_DETAILS + "("
                    + COLUMN_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_LOG_USER_ID + " INTEGER,"
                    + COLUMN_LOG_ACTIVITY + " TEXT,"
                    + COLUMN_LOG_TIMESTAMP + " TEXT" + ")")
            db?.execSQL(createLogTable)
        }
        if (oldVersion < 3) {
            val createParkingSpotsTable = ("CREATE TABLE " + TABLE_PARKING_SPOTS + "("
                + COLUMN_SPOT_ID + " TEXT PRIMARY KEY,"
                + COLUMN_CITY_NAME + " TEXT NOT NULL,"
                + COLUMN_PARKING_NAME + " TEXT,"
                + COLUMN_ADDRESS + " TEXT,"
                + COLUMN_LATITUDE + " REAL,"
                + COLUMN_LONGITUDE + " REAL,"
                + COLUMN_FOUR_WHEELER_SPOTS + " INTEGER NOT NULL,"
                + COLUMN_TWO_WHEELER_SPOTS + " INTEGER NOT NULL,"
                + COLUMN_ZONE_NAME + " TEXT,"
                + COLUMN_WARD_NAME + " TEXT,"
                + COLUMN_PRICES + " TEXT,"
                + COLUMN_OPENING_TIMES + " TEXT,"
                + COLUMN_COVERAGE_TYPE + " TEXT" + ")")
             db?.execSQL(createParkingSpotsTable)
        }
    }

    fun insertOrUpdateParkingSpots(parkingSpots: List<ParkingSpot>) {
        val db = this.writableDatabase
        db.beginTransaction()
        try {
            parkingSpots.forEach { spot ->
                val values = ContentValues().apply {
                    put(COLUMN_SPOT_ID, spot.id)
                    put(COLUMN_CITY_NAME, spot.cityName)
                    put(COLUMN_PARKING_NAME, spot.parkingName)
                    put(COLUMN_ADDRESS, spot.address)
                    put(COLUMN_LATITUDE, spot.latitude)
                    put(COLUMN_LONGITUDE, spot.longitude)
                    put(COLUMN_FOUR_WHEELER_SPOTS, spot.fourWheelerSpots)
                    put(COLUMN_TWO_WHEELER_SPOTS, spot.twoWheelerSpots)
                    put(COLUMN_ZONE_NAME, spot.zoneName)
                    put(COLUMN_WARD_NAME, spot.wardName)
                    put(COLUMN_PRICES, gson.toJson(spot.prices))
                    put(COLUMN_OPENING_TIMES, gson.toJson(spot.openingTimes))
                    put(COLUMN_COVERAGE_TYPE, spot.coverageType)
                }
                db.insertWithOnConflict(TABLE_PARKING_SPOTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getAllParkingSpotsFlow(): Flow<List<ParkingSpot>> = flow {
        emit(getAllParkingSpots()) // Emit the list from the existing function
    }.flowOn(Dispatchers.IO) // Ensure the DB query runs on the IO thread

    fun getAllParkingSpots(): List<ParkingSpot> {
        val spots = mutableListOf<ParkingSpot>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_PARKING_SPOTS", null)

        if (cursor.moveToFirst()) {
            do {
                val priceListType = object : TypeToken<List<PriceInfo>>() {}.type
                val openingTimeListType = object : TypeToken<List<OpeningTimeInfo>>() {}.type

                val pricesJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRICES))
                val openingTimesJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OPENING_TIMES))

                spots.add(ParkingSpot(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPOT_ID)),
                    cityName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CITY_NAME)),
                    parkingName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PARKING_NAME)),
                    address = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)),
                    latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)),
                    longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)),
                    fourWheelerSpots = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOUR_WHEELER_SPOTS)),
                    twoWheelerSpots = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TWO_WHEELER_SPOTS)),
                    zoneName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ZONE_NAME)),
                    wardName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WARD_NAME)),
                    prices = gson.fromJson(pricesJson, priceListType),
                    openingTimes = gson.fromJson(openingTimesJson, openingTimeListType),
                    coverageType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COVERAGE_TYPE))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return spots
    }

    fun getParkingSpotById(spotId: String): ParkingSpot? {
        val db = this.readableDatabase
        var spot: ParkingSpot? = null
        val cursor = db.query(TABLE_PARKING_SPOTS, null, "$COLUMN_SPOT_ID = ?", arrayOf(spotId), null, null, null, "1")

        if (cursor.moveToFirst()) {
            val priceListType = object : TypeToken<List<PriceInfo>>() {}.type
            val openingTimeListType = object : TypeToken<List<OpeningTimeInfo>>() {}.type

            val pricesJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRICES))
            val openingTimesJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OPENING_TIMES))
            
            spot = ParkingSpot(
                id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPOT_ID)),
                cityName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CITY_NAME)),
                parkingName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PARKING_NAME)),
                address = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADDRESS)),
                latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LATITUDE)),
                longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LONGITUDE)),
                fourWheelerSpots = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_FOUR_WHEELER_SPOTS)),
                twoWheelerSpots = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TWO_WHEELER_SPOTS)),
                zoneName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ZONE_NAME)),
                wardName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_WARD_NAME)),
                prices = gson.fromJson(pricesJson, priceListType),
                openingTimes = gson.fromJson(openingTimesJson, openingTimeListType),
                coverageType = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COVERAGE_TYPE))
            )
        }
        cursor.close()
        return spot
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

        var cursor = db.query(
            TABLE_USER_DETAILS,
            arrayOf(KEY_PASSWORD_HASH),
            "$KEY_USERNAME = ? OR $KEY_EMAIL_OR_PHONE = ?", 
            arrayOf(usernameOrEmail, usernameOrEmail),
            null, null, null
        )
        if (cursor.moveToFirst()) {
            userPasswordHash = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD_HASH))
        }
        cursor.close()

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
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID))
        }
        cursor.close()
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
