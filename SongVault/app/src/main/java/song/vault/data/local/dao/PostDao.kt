package song.vault.data.local.dao

import androidx.room.*
import song.vault.data.local.entity.PostEntity
import song.vault.data.local.relation.PostWithUser
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

    @Transaction
    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun observeAllPostsWithUsers(): Flow<List<PostWithUser>>

    @Transaction
    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    suspend fun getAllPostsWithUsers(): List<PostWithUser>

    @Transaction
    @Query("SELECT * FROM posts WHERE genre = :genre ORDER BY createdAt DESC")
    fun observePostsByGenre(genre: String): Flow<List<PostWithUser>>

    @Transaction
    @Query("SELECT * FROM posts WHERE genre = :genre ORDER BY createdAt DESC")
    suspend fun getPostsByGenre(genre: String): List<PostWithUser>

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

    @Query("DELETE FROM posts")
    suspend fun deleteAllPosts()
}
