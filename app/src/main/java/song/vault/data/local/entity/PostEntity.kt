package song.vault.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val userDisplayName: String?,
    val userProfileImageUrl: String?,
    val musicTitle: String,
    val musicArtist: String,
    val musicThumbnailUrl: String?,
    val musicExternalUrl: String,
    val musicSource: String,
    val caption: String?,
    val genre: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
