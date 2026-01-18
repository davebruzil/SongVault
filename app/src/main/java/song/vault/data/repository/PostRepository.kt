package song.vault.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import song.vault.data.local.dao.PostDao
import song.vault.data.local.entity.PostEntity
import song.vault.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PostRepository(
    private val postDao: PostDao,
    private val userRepository: UserRepository
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val postsCollection = firestore.collection("posts")

    fun observeUserPosts(userId: String): Flow<List<PostEntity>> = postDao.observeUserPosts(userId)

    suspend fun getUserPostCount(userId: String): Int = postDao.getUserPostCount(userId)

    suspend fun createPost(
        musicTitle: String, musicArtist: String, musicThumbnailUrl: String?,
        musicExternalUrl: String, musicSource: String, caption: String?, genre: String?
    ): Resource<PostEntity> {
        return try {
            val user = userRepository.currentFirebaseUser ?: return Resource.Error("Not logged in")
            val cachedUser = userRepository.getCurrentUser()

            val post = PostEntity(
                id = UUID.randomUUID().toString(),
                userId = user.uid,
                userDisplayName = cachedUser?.displayName,
                userProfileImageUrl = cachedUser?.profileImageUrl,
                musicTitle = musicTitle,
                musicArtist = musicArtist,
                musicThumbnailUrl = musicThumbnailUrl,
                musicExternalUrl = musicExternalUrl,
                musicSource = musicSource,
                caption = caption,
                genre = genre
            )

            postsCollection.document(post.id).set(post).await()
            postDao.insertPost(post)
            Resource.Success(post)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create post")
        }
    }

    suspend fun updatePost(postId: String, caption: String?, genre: String?): Resource<Unit> {
        return try {
            val post = postDao.getPostById(postId) ?: return Resource.Error("Post not found")
            val updated = post.copy(caption = caption, genre = genre, updatedAt = System.currentTimeMillis())
            postsCollection.document(postId).set(updated).await()
            postDao.updatePost(updated)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update")
        }
    }

    suspend fun deletePost(postId: String): Resource<Unit> {
        return try {
            postsCollection.document(postId).delete().await()
            postDao.deletePostById(postId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete")
        }
    }

    suspend fun syncUserPosts(userId: String): Resource<List<PostEntity>> {
        return try {
            val snapshot = postsCollection.whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING).get().await()

            val posts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(PostEntity::class.java)
            }
            postDao.deleteUserPosts(userId)
            postDao.insertPosts(posts)
            Resource.Success(posts)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Sync failed")
        }
    }
}
