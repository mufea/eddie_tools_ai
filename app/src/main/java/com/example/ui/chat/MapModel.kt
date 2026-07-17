package com.example.ui.chat

data class MapPlace(
    val id: String,
    val name: String,
    val category: String, // e.g. "Cafe", "Restaurant", "Park", "Museum", "Landmark"
    val latOffset: Float, // relative offset on our scrollable canvas (-1.0 to 1.0)
    val lngOffset: Float, // relative offset on our scrollable canvas (-1.0 to 1.0)
    val latitude: Double, // Real lat coordinate
    val longitude: Double, // Real lng coordinate
    val rating: Float,
    val reviewsCount: Int,
    val address: String,
    val description: String,
    val highlights: String, // e.g. "Signature Lavender Latte", "Panoramic view"
    val openHours: String,
    val funFact: String
)

data class MapChatMessage(
    val sender: String, // "user" or "assistant"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
