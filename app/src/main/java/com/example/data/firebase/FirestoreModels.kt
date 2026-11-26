package com.example.data.firebase

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class FirestoreUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val username: String = "",
    val photoUrl: String = "",
    val appSkinThemeId: String = "slate",
    val customSystemInstruction: String = "",
    val activeSubscriptionPlan: String = "Free Tier",
    val subscriptionExpiryDate: String = "Free Plan",
    val subscribed: Boolean = false,
    val userTokens: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreChatSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "Percakapan Baru",
    val modelValue: String = "gpt-5-2025-08-07",
    val clientTimestamp: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sessionId: String = "",
    val role: String = "USER",
    val text: String = "",
    val bookmarked: Boolean = false,
    val clientTimestamp: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreGeneratedImage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val prompt: String = "",
    val imageUrl: String = "",
    val style: String = "",
    val aspectRatio: String = "",
    val model: String = "imagen3",
    val clientTimestamp: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreGeneratedVideo(
    @DocumentId val id: String = "",
    val userId: String = "",
    val prompt: String = "",
    val videoUrl: String = "",
    val style: String = "",
    val aspectRatio: String = "",
    val resolution: String = "",
    val duration: String = "",
    val model: String = "Unknown",
    val clientTimestamp: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreGeneratedMusic(
    @DocumentId val id: String = "",
    val userId: String = "",
    val prompt: String = "",
    val musicUrl: String = "",
    val genre: String = "",
    val duration: String = "",
    val tempo: String = "",
    val clientTimestamp: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreUserMemory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val key: String = "",
    val content: String = "",
    val clientTimestamp: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FirestoreTokenUsage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val featureType: String = "",
    val detail: String = "",
    val tokenCost: Double = 0.0,
    val clientTimestamp: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)
