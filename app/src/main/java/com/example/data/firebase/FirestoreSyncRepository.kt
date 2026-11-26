package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        }
        return uid
    }

    private fun isValidDocId(id: String): Boolean =
        id.length in 1..128 && id.matches(Regex("^[a-zA-Z0-9_\\-]+$"))

    private fun <T> observeSubcollection(
        userId: String,
        subcollection: String,
        clazz: Class<T>
    ): Flow<List<T>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch for $subcollection." }
        val path = "users/$uid/$subcollection"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection(subcollection)
                .orderBy("clientTimestamp", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeUserProfile(userId: String): Flow<FirestoreUserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch for profile." }
        val path = "users/$uid"
        emitAll(
            db.collection("users")
                .document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            FirestoreUserProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeSessions(userId: String): Flow<List<FirestoreChatSession>> =
        observeSubcollection(userId, "sessions", FirestoreChatSession::class.java)

    fun observeMessages(userId: String): Flow<List<FirestoreChatMessage>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Signed-in user mismatch for messages." }
        val path = "users/$uid/messages"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection("messages")
                .orderBy("clientTimestamp", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        FirestoreChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeGeneratedImages(userId: String): Flow<List<FirestoreGeneratedImage>> =
        observeSubcollection(userId, "generated_images", FirestoreGeneratedImage::class.java)

    fun observeGeneratedVideos(userId: String): Flow<List<FirestoreGeneratedVideo>> =
        observeSubcollection(userId, "generated_videos", FirestoreGeneratedVideo::class.java)

    fun observeGeneratedMusic(userId: String): Flow<List<FirestoreGeneratedMusic>> =
        observeSubcollection(userId, "generated_music", FirestoreGeneratedMusic::class.java)

    fun observeMemories(userId: String): Flow<List<FirestoreUserMemory>> =
        observeSubcollection(userId, "memories", FirestoreUserMemory::class.java)

    fun observeTokenUsages(userId: String): Flow<List<FirestoreTokenUsage>> =
        observeSubcollection(userId, "token_usages", FirestoreTokenUsage::class.java)

    suspend fun ensureUserProfile(
        username: String,
        email: String?,
        photoUrl: String?,
        defaultThemeId: String,
        defaultInstruction: String,
        defaultPlan: String,
        defaultExpiry: String,
        defaultSubscribed: Boolean,
        defaultTokens: Double
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            require(isValidDocId(uid)) { "Invalid UID format" }
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val snapshot = docRef.get().await()
            val safeName = username.trim().ifBlank { "User" }.take(100)
            val safeEmail = email?.trim()?.take(254)
            val safePhoto = photoUrl?.trim()?.take(1000)

            if (!snapshot.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "username" to safeName,
                    "email" to safeEmail,
                    "photoUrl" to safePhoto,
                    "appSkinThemeId" to defaultThemeId.take(40),
                    "customSystemInstruction" to defaultInstruction.take(4000),
                    "activeSubscriptionPlan" to defaultPlan.take(80),
                    "subscriptionExpiryDate" to defaultExpiry.take(80),
                    "subscribed" to defaultSubscribed,
                    "userTokens" to defaultTokens.coerceAtLeast(0.0),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "username" to safeName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!safeEmail.isNullOrBlank()) updates["email"] = safeEmail
                if (!safePhoto.isNullOrBlank()) updates["photoUrl"] = safePhoto
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserPreferences(
        appSkinThemeId: String? = null,
        customSystemInstruction: String? = null,
        activeSubscriptionPlan: String? = null,
        subscriptionExpiryDate: String? = null,
        subscribed: Boolean? = null,
        userTokens: Double? = null
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            appSkinThemeId?.let { updates["appSkinThemeId"] = it.take(40) }
            customSystemInstruction?.let { updates["customSystemInstruction"] = it.take(4000) }
            activeSubscriptionPlan?.let { updates["activeSubscriptionPlan"] = it.take(80) }
            subscriptionExpiryDate?.let { updates["subscriptionExpiryDate"] = it.take(80) }
            subscribed?.let { updates["subscribed"] = it }
            userTokens?.let { updates["userTokens"] = it.coerceAtLeast(0.0) }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertSession(
        sessionId: String,
        title: String,
        modelValue: String,
        clientTimestamp: Long
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/sessions/$sessionId"
        return try {
            val uid = requireUserId()
            require(isValidDocId(sessionId)) { "Invalid sessionId" }
            val docRef = db.collection("users").document(uid).collection("sessions").document(sessionId)
            docPath = docRef.path
            val safeTitle = title.trim().ifBlank { "Percakapan Baru" }.take(200)
            val safeModel = modelValue.trim().ifBlank { "gpt-5-2025-08-07" }.take(120)
            val safeTs = clientTimestamp.coerceAtLeast(0L)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "title" to safeTitle,
                        "modelValue" to safeModel,
                        "clientTimestamp" to safeTs,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "title" to safeTitle,
                        "modelValue" to safeModel,
                        "clientTimestamp" to safeTs,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSessionAndMessages(sessionId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/sessions/$sessionId"
        return try {
            val uid = requireUserId()
            require(isValidDocId(sessionId)) { "Invalid sessionId" }
            val userDoc = db.collection("users").document(uid)
            val msgSnapshots = userDoc.collection("messages")
                .whereEqualTo("sessionId", sessionId)
                .get()
                .await()
            val batch = db.batch()
            msgSnapshots.documents.forEach { batch.delete(it.reference) }
            val sessionRef = userDoc.collection("sessions").document(sessionId)
            docPath = sessionRef.path
            batch.delete(sessionRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertMessage(
        messageId: String,
        sessionId: String,
        role: String,
        text: String,
        bookmarked: Boolean,
        clientTimestamp: Long
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/messages/$messageId"
        return try {
            val uid = requireUserId()
            require(isValidDocId(messageId) && isValidDocId(sessionId)) { "Invalid ID" }
            val docRef = db.collection("users").document(uid).collection("messages").document(messageId)
            docPath = docRef.path
            val normalizedRole = if (role == "USER") "USER" else "MODEL"
            val safeText = text.take(50000)
            val safeTs = clientTimestamp.coerceAtLeast(0L)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "text" to safeText,
                        "bookmarked" to bookmarked,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "sessionId" to sessionId,
                        "role" to normalizedRole,
                        "text" to safeText,
                        "bookmarked" to bookmarked,
                        "clientTimestamp" to safeTs,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateMessageBookmark(messageId: String, bookmarked: Boolean): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/messages/$messageId"
        return try {
            val uid = requireUserId()
            require(isValidDocId(messageId)) { "Invalid messageId" }
            val docRef = db.collection("users").document(uid).collection("messages").document(messageId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "bookmarked" to bookmarked,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveGeneratedImage(
        id: String,
        prompt: String,
        imageUrl: String,
        style: String,
        aspectRatio: String,
        model: String,
        clientTimestamp: Long
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/generated_images/$id"
        return try {
            val uid = requireUserId()
            require(isValidDocId(id)) { "Invalid imageId" }
            if (imageUrl.length > 780_000) {
                return Result.failure(IllegalArgumentException("Image payload exceeds Firestore document limit; kept locally in Room."))
            }
            val docRef = db.collection("users").document(uid).collection("generated_images").document(id)
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "prompt" to prompt.take(4000),
                    "imageUrl" to imageUrl,
                    "style" to style.take(120),
                    "aspectRatio" to aspectRatio.take(40),
                    "model" to model.take(120),
                    "clientTimestamp" to clientTimestamp.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteGeneratedImage(id: String): Result<Unit> =
        deleteDocument("generated_images", id)

    suspend fun saveGeneratedVideo(
        id: String,
        prompt: String,
        videoUrl: String,
        style: String,
        aspectRatio: String,
        resolution: String,
        duration: String,
        model: String,
        clientTimestamp: Long
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/generated_videos/$id"
        return try {
            val uid = requireUserId()
            require(isValidDocId(id)) { "Invalid videoId" }
            val docRef = db.collection("users").document(uid).collection("generated_videos").document(id)
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "prompt" to prompt.take(4000),
                    "videoUrl" to videoUrl.take(4000),
                    "style" to style.take(120),
                    "aspectRatio" to aspectRatio.take(40),
                    "resolution" to resolution.take(40),
                    "duration" to duration.take(40),
                    "model" to model.take(120),
                    "clientTimestamp" to clientTimestamp.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteGeneratedVideo(id: String): Result<Unit> =
        deleteDocument("generated_videos", id)

    suspend fun saveGeneratedMusic(
        id: String,
        prompt: String,
        musicUrl: String,
        genre: String,
        duration: String,
        tempo: String,
        clientTimestamp: Long
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/generated_music/$id"
        return try {
            val uid = requireUserId()
            require(isValidDocId(id)) { "Invalid musicId" }
            val docRef = db.collection("users").document(uid).collection("generated_music").document(id)
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "prompt" to prompt.take(4000),
                    "musicUrl" to musicUrl.take(4000),
                    "genre" to genre.take(120),
                    "duration" to duration.take(40),
                    "tempo" to tempo.take(40),
                    "clientTimestamp" to clientTimestamp.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteGeneratedMusic(id: String): Result<Unit> =
        deleteDocument("generated_music", id)

    suspend fun saveMemory(
        id: String,
        key: String,
        content: String,
        clientTimestamp: Long
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/memories/$id"
        return try {
            val uid = requireUserId()
            require(isValidDocId(id)) { "Invalid memoryId" }
            val safeKey = key.trim().ifBlank { "NOTE" }.take(120)
            val safeContent = content.trim().ifBlank { "-" }.take(4000)
            val docRef = db.collection("users").document(uid).collection("memories").document(id)
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "key" to safeKey,
                    "content" to safeContent,
                    "clientTimestamp" to clientTimestamp.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteMemory(id: String): Result<Unit> =
        deleteDocument("memories", id)

    suspend fun saveTokenUsage(
        id: String,
        featureType: String,
        detail: String,
        tokenCost: Double,
        clientTimestamp: Long
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/token_usages/$id"
        return try {
            val uid = requireUserId()
            require(isValidDocId(id)) { "Invalid usageId" }
            val docRef = db.collection("users").document(uid).collection("token_usages").document(id)
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "featureType" to featureType.trim().ifBlank { "General" }.take(120),
                    "detail" to detail.take(2000),
                    "tokenCost" to tokenCost.coerceAtLeast(0.0),
                    "clientTimestamp" to clientTimestamp.coerceAtLeast(0L),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun clearTokenUsages(): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/token_usages"
        return try {
            val uid = requireUserId()
            val colRef = db.collection("users").document(uid).collection("token_usages")
            docPath = colRef.path
            val snap = colRef.get().await()
            val batch = db.batch()
            snap.documents.forEach { batch.delete(it.reference) }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private suspend fun deleteDocument(subcollection: String, id: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/$subcollection/$id"
        return try {
            val uid = requireUserId()
            require(isValidDocId(id)) { "Invalid document id" }
            val docRef = db.collection("users").document(uid).collection(subcollection).document(id)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
