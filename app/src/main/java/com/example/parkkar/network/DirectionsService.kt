package com.example.parkkar.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object DirectionsService {

    // ✅ Using OkHttp engine instead of CIO to fix TLS handshake issues
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun getDirections(coordinates: String): RouteResponse? {
        return try {
            val url = "https://router.project-osrm.org/route/v1/driving/$coordinates?overview=full&geometries=geojson"
            client.get(url).body<RouteResponse>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

@Serializable
data class RouteResponse(
    val routes: List<Route>
)

@Serializable
data class Route(
    val geometry: Geometry
)

@Serializable
data class Geometry(
    val coordinates: List<List<Double>>
)
