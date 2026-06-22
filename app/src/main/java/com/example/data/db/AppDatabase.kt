package com.example.data.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- Room Entities ---

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelValue: String = "openai",
    val userEmail: String? = null
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String, // "USER" or "MODEL"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false
)

@Entity(tableName = "generated_images")
data class GeneratedImageEntity(
    @PrimaryKey val id: String,
    val prompt: String,
    val imageUrl: String,
    val style: String,
    val aspectRatio: String,
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String? = null,
    val model: String = "imagen3"
)

@Entity(tableName = "generated_videos")
data class GeneratedVideoEntity(
    @PrimaryKey val id: String,
    val prompt: String,
    val videoUrl: String,
    val style: String,
    val aspectRatio: String,
    val resolution: String,
    val duration: String,
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String? = null
)

@Entity(tableName = "generated_music")
data class GeneratedMusicEntity(
    @PrimaryKey val id: String,
    val prompt: String,
    val musicUrl: String,
    val genre: String,
    val duration: String,
    val tempo: String,
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String? = null
)

@Entity(tableName = "user_memories")
data class UserMemoryEntity(
    @PrimaryKey val id: String,
    val key: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String? = null
)

@Entity(tableName = "token_usages")
data class TokenUsageEntity(
    @PrimaryKey val id: String,
    val featureType: String, // "Text Generation", "Image Generation", "Video Generation", "Music Generation", etc.
    val detail: String,
    val tokenCost: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String? = null
)

// --- DAO (Data Access Object) ---

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM chat_sessions WHERE userEmail = :email ORDER BY timestamp DESC")
    fun getSessionsForUser(email: String): Flow<List<ChatSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET isBookmarked = :isBookmarked WHERE id = :messageId")
    suspend fun updateMessageBookmark(messageId: String, isBookmarked: Boolean)

    @Query("SELECT * FROM chat_messages WHERE isBookmarked = 1 ORDER BY timestamp DESC")
    fun getBookmarkedMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeneratedImage(image: GeneratedImageEntity)

    @Query("SELECT * FROM generated_images ORDER BY timestamp DESC")
    fun getAllGeneratedImages(): Flow<List<GeneratedImageEntity>>

    @Query("SELECT * FROM generated_images WHERE userEmail = :email ORDER BY timestamp DESC")
    fun getGeneratedImagesForUser(email: String): Flow<List<GeneratedImageEntity>>

    @Query("SELECT * FROM generated_images WHERE id = :imageId LIMIT 1")
    suspend fun getGeneratedImageById(imageId: String): GeneratedImageEntity?

    @Query("DELETE FROM generated_images WHERE id = :imageId")
    suspend fun deleteGeneratedImageById(imageId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeneratedVideo(video: GeneratedVideoEntity)

    @Query("SELECT * FROM generated_videos ORDER BY timestamp DESC")
    fun getAllGeneratedVideos(): Flow<List<GeneratedVideoEntity>>

    @Query("SELECT * FROM generated_videos WHERE userEmail = :email ORDER BY timestamp DESC")
    fun getGeneratedVideosForUser(email: String): Flow<List<GeneratedVideoEntity>>

    @Query("DELETE FROM generated_videos WHERE id = :videoId")
    suspend fun deleteGeneratedVideoById(videoId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeneratedMusic(music: GeneratedMusicEntity)

    @Query("SELECT * FROM generated_music ORDER BY timestamp DESC")
    fun getAllGeneratedMusic(): Flow<List<GeneratedMusicEntity>>

    @Query("SELECT * FROM generated_music WHERE userEmail = :email ORDER BY timestamp DESC")
    fun getGeneratedMusicForUser(email: String): Flow<List<GeneratedMusicEntity>>

    @Query("DELETE FROM generated_music WHERE id = :musicId")
    suspend fun deleteGeneratedMusicById(musicId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: UserMemoryEntity)

    @Query("SELECT * FROM user_memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<UserMemoryEntity>>

    @Query("SELECT * FROM user_memories WHERE userEmail = :email ORDER BY timestamp DESC")
    fun getMemoriesForUser(email: String): Flow<List<UserMemoryEntity>>

    @Query("DELETE FROM user_memories WHERE id = :id")
    suspend fun deleteMemoryById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTokenUsage(usage: TokenUsageEntity)

    @Query("SELECT * FROM token_usages ORDER BY timestamp DESC")
    fun getAllTokenUsages(): Flow<List<TokenUsageEntity>>

    @Query("SELECT * FROM token_usages WHERE userEmail = :email ORDER BY timestamp DESC")
    fun getTokenUsagesForUser(email: String): Flow<List<TokenUsageEntity>>

    @Query("DELETE FROM token_usages")
    suspend fun deleteAllTokenUsages()

    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAllSessions()
}

// --- AppDatabase class ---

@Database(
    entities = [
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        GeneratedImageEntity::class,
        GeneratedVideoEntity::class,
        GeneratedMusicEntity::class,
        UserMemoryEntity::class,
        TokenUsageEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            try {
                val field = android.database.CursorWindow::class.java.getDeclaredField("sCursorWindowSize")
                field.isAccessible = true
                field.set(null, 50 * 1024 * 1024) // Override CursorWindow size to 50MB to support larger Base64 blob fields safely
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Failed to override CursorWindow size", e)
            }
            return INSTANCE ?: synchronized(this) {
                val db = INSTANCE ?: try {
                    Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "ai_chat_database"
                    )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
                } catch (e: Throwable) {
                    android.util.Log.e("AppDatabase", "Error initializing database, recreating...", e)
                    try {
                        context.applicationContext.deleteDatabase("ai_chat_database")
                        Room.databaseBuilder(
                            context.applicationContext,
                            AppDatabase::class.java,
                            "ai_chat_database"
                        )
                        .allowMainThreadQueries()
                        .fallbackToDestructiveMigration()
                        .build()
                    } catch (ex: Throwable) {
                        android.util.Log.e("AppDatabase", "Fallback to in-memory database due to persistent error", ex)
                        try {
                            Room.inMemoryDatabaseBuilder(
                                context.applicationContext,
                                AppDatabase::class.java
                            )
                            .allowMainThreadQueries()
                            .fallbackToDestructiveMigration()
                            .build()
                        } catch (fatal: Throwable) {
                            android.util.Log.e("AppDatabase", "Fatal error building database", fatal)
                            throw fatal
                        }
                    }
                }
                INSTANCE = db
                db
            }
        }
    }
}
