package song.vault.data.local.dao

import androidx.room.*
import song.vault.data.local.entity.PostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeUserPosts(userId: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getUserPosts(userId: String): List<PostEntity>

    @Query("SELECT * FROM posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: String): PostEntity?

    @Query("SELECT COUNT(*) FROM posts WHERE userId = :userId")
    suspend fun getUserPostCount(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Update
    suspend fun updatePost(post: PostEntity)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: String)

    @Query("DELETE FROM posts WHERE userId = :userId")
    suspend fun deleteUserPosts(userId: String)

    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun observeAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE genre = :genre ORDER BY createdAt DESC")
    fun observePostsByGenre(genre: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE musicTitle LIKE :query OR musicArtist LIKE :query OR caption LIKE :query ORDER BY createdAt DESC")
    fun searchPosts(query: String): Flow<List<PostEntity>>

    @Query("SELECT DISTINCT genre FROM posts WHERE genre IS NOT NULL")
    suspend fun getAvailableGenres(): List<String>
}
