package com.example.parkkar.recommendation

import com.example.parkkar.model.ParkingLocation
import java.util.Calendar

class RecommendationEngine {

    fun generateRecommendations(
        userLat: Double,
        userLon: Double,
        allParkingLocations: List<ParkingLocation>
    ): List<ParkingLocation> {
        val scoredLocations = allParkingLocations.mapNotNull { location ->
            // Skip locations without valid coordinates or capacity
            if (location.latitude == null || location.longitude == null || location.totalCapacity <= 0) {
                return@mapNotNull null
            }

            val proximityScore = calculateProximityScore(userLat, userLon, location.latitude, location.longitude)
            val timeOfDayScore = calculateTimeOfDayScore(location)
            val capacityScore = calculateCapacityScore(location.totalCapacity)

            // Combine scores with weighting
            val totalScore = (proximityScore * 0.5) + (timeOfDayScore * 0.3) + (capacityScore * 0.2)

            Pair(location, totalScore)
        }

        // Sort by score in descending order and take the top 5
        return scoredLocations.sortedByDescending { it.second }.take(5).map { it.first }
    }

    private fun calculateProximityScore(userLat: Double, userLon: Double, destLat: Double, destLon: Double): Double {
        val distance = haversineDistance(userLat, userLon, destLat, destLon)
        // Inverse score: closer is better. Normalize to a 0-100 scale.
        // The score decreases as distance increases. A distance of 0 gives a score of 100.
        // A distance of 1km gives a score of roughly 50.
        return (1.0 / (1.0 + distance)).coerceIn(0.0, 1.0) * 100
    }

    private fun calculateTimeOfDayScore(location: ParkingLocation): Double {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        val isWeekday = dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
        val isMorning = hourOfDay in 8..11

        val locationName = location.name ?: ""

        // Boost score for business areas on weekday mornings
        if (isWeekday && isMorning) {
            if (locationName.contains("Thane Station Road", ignoreCase = true) || locationName.contains("Business Area", ignoreCase = true)) {
                return 100.0
            }
        }

        // Boost score for malls on evenings and weekends
        if (!isWeekday || hourOfDay >= 17) {
            if (locationName.contains("Korum Mall", ignoreCase = true) || locationName.contains("Viviana Mall", ignoreCase = true)) {
                return 100.0
            }
        }

        return 50.0 // Default score
    }

    private fun calculateCapacityScore(totalCapacity: Int): Double {
        // Larger capacity gets a higher score. Capped at 500 for normalization.
        return (totalCapacity.toDouble() / 500.0).coerceAtMost(1.0) * 100
    }

    // Haversine formula to calculate distance between two lat/lon points in kilometers
    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371 // Radius of Earth in kilometers
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)
        val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}
