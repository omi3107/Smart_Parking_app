package com.example.parkkar.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.parkkar.data.UserPreferencesRepository
import com.example.parkkar.utils.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class TimeToLeaveWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val userPreferencesRepository = UserPreferencesRepository.getInstance(applicationContext)
        val notificationsEnabled = runBlocking { userPreferencesRepository.notificationsEnabled.first() }

        if (!notificationsEnabled) {
            return Result.success()
        }

        val parkingName = inputData.getString("parkingName") ?: return Result.failure()
        val expiryTime = inputData.getString("expiryTime") ?: return Result.failure()

        // In a real app, you would get the user's live location and calculate the travel time.
        // For this example, we'll just use a fixed travel time.
        val travelTime = 7

        NotificationHelper.showTimeToLeaveNotification(applicationContext, travelTime, parkingName, expiryTime)

        return Result.success()
    }
}
