package song.vault.data.local.dao

import androidx.room.*
import song.vault.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM users WHERE isCurrentUser = 1 LIMIT 1")
    fun observeCurrentUser(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET isCurrentUser = 0")
    suspend fun clearCurrentUser()

    @Query(
        "UPDATE users SET displayName = :displayName, profileImageUrl = :profileImageUrl, favoriteGenre = :favoriteGenre, favoriteSong = :favoriteSong, bio = :bio, updatedAt = :updatedAt WHERE uid = :uid"
    )
    suspend fun updateProfile(
        uid: String,
        displayName: String?,
        profileImageUrl: String?,
        favoriteGenre: String?,
        favoriteSong: String?,
        bio: String?,
        updatedAt: Long = System.currentTimeMillis()
    )
}
