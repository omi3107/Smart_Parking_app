package com.example.parkkar.data.repository

import android.content.Context
import android.util.Log
import com.example.parkkar.data.DatabaseHelper
import com.example.parkkar.data.model.ParkingSpot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.toObjects
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * The repository is the single source of truth for all app data.
 * It follows the architecture plan by managing two data sources:
 *  - Remote: Firebase Firestore, the authoritative source.
 *  - Local: SQLite (DatabaseHelper), which acts as a cache and th      e Single Source of Truth for the UI.
 */
class ParkingDataRepository private constructor(context: Context) {

    // The local database (SQLite), which is the source of truth for the UI.
    private val localDataSource = DatabaseHelper(context)
    
    // The remote database (Firebase Firestore).
    private val remoteDataSource = Firebase.firestore

    init {
        // As soon as the repository is created, we start syncing the remote data
        // into our local database.
        startFirestoreSync()
    }

    /**
     * Sets up a real-time listener on the 'parking_spots' collection in Firestore.
     * When data changes on the server, it fetches the updates and saves them to the local SQLite database.
     */
    private fun startFirestoreSync() {
        remoteDataSource.collection("parking_spots")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("ParkingRepo", "Firestore listener failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    // Offload the database write to a background thread.
                    CoroutineScope(Dispatchers.IO).launch {
                        val parkingSpots = snapshot.toObjects<ParkingSpot>()
                        Log.d("ParkingRepo", "Syncing ${parkingSpots.size} spots from Firestore...")
                        
                        // Save the fresh data from Firebase into our local database.
                        localDataSource.insertOrUpdateParkingSpots(parkingSpots)
                        Log.d("ParkingRepo", "Sync complete. Data is now in the local SQLite database.")
                    }
                }
            }
    }

    /**
     * Gets all parking spots as a Flow from the local SQLite database.
     * The UI will collect this Flow, making it fast, reactive, and offline-first.
     */
    fun getAllParkingSpotsFlow(): Flow<List<ParkingSpot>> {
        Log.d("ParkingRepo", "Providing a FLOW of all spots from LOCAL database.")
        return localDataSource.getAllParkingSpotsFlow() // This function will be created in DatabaseHelper
    }

    /**
     * Gets all parking spots directly from the local SQLite database.
     */
    suspend fun getAllParkingSpots(): List<ParkingSpot> {
        Log.d("ParkingRepo", "Fetching all spots from LOCAL database.")
        return localDataSource.getAllParkingSpots()
    }

    /**
     * Gets a single parking spot by its ID from the local SQLite database.
     */
    suspend fun getParkingSpotById(spotId: String): ParkingSpot? {
        Log.d("ParkingRepo", "Fetching spot $spotId from LOCAL database.")
        return localDataSource.getParkingSpotById(spotId)
    }

    companion object {
        @Volatile
        private var INSTANCE: ParkingDataRepository? = null

        fun getInstance(context: Context): ParkingDataRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = ParkingDataRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
