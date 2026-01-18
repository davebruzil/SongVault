package song.vault.data.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.storage.FirebaseStorage
import song.vault.data.local.dao.UserDao
import song.vault.data.local.entity.UserEntity
import song.vault.data.remote.FirebaseAuthSource
import song.vault.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val userDao: UserDao,
    private val authSource: FirebaseAuthSource
) {
    val isLoggedIn: Boolean get() = authSource.isLoggedIn
    val currentFirebaseUser: FirebaseUser? get() = authSource.currentUser

    fun observeCurrentUser(): Flow<UserEntity?> = userDao.observeCurrentUser()
    suspend fun getCurrentUser(): UserEntity? = userDao.getCurrentUser()

    suspend fun login(email: String, password: String): Resource<UserEntity> {
        return when (val result = authSource.login(email, password)) {
            is Resource.Success -> {
                val user = result.data
                val entity = UserEntity(user.uid, email, user.displayName, user.photoUrl?.toString(), true)
                userDao.clearCurrentUser()
                userDao.insertUser(entity)
                Resource.Success(entity)
            }
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    suspend fun register(email: String, password: String, displayName: String): Resource<UserEntity> {
        return when (val result = authSource.register(email, password, displayName)) {
            is Resource.Success -> {
                val user = result.data
                val entity = UserEntity(user.uid, email, displayName, null, true)
                userDao.clearCurrentUser()
                userDao.insertUser(entity)
                Resource.Success(entity)
            }
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    suspend fun signInWithGoogle(idToken: String): Resource<UserEntity> {
        return when (val result = authSource.signInWithGoogle(idToken)) {
            is Resource.Success -> {
                val user = result.data
                val entity = UserEntity(
                    user.uid,
                    user.email ?: "",
                    user.displayName,
                    user.photoUrl?.toString(),
                    true
                )
                userDao.clearCurrentUser()
                userDao.insertUser(entity)
                Resource.Success(entity)
            }
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading
        }
    }

    suspend fun logout() {
        userDao.clearCurrentUser()
        authSource.logout()
    }

    suspend fun updateProfile(displayName: String?, profileImageUrl: String?): Resource<Unit> {
        val uid = currentFirebaseUser?.uid ?: return Resource.Error("Not logged in")
        userDao.updateProfile(uid, displayName, profileImageUrl)
        return Resource.Success(Unit)
    }

    suspend fun uploadProfileImage(imageUri: Uri): Resource<String> {
        val uid = currentFirebaseUser?.uid ?: return Resource.Error("Not logged in")

        return try {
            val storage = FirebaseStorage.getInstance()
            val imageRef = storage.reference.child("profile_images/$uid.jpg")

            // Upload the image
            imageRef.putFile(imageUri).await()

            // Get the download URL
            val downloadUrl = imageRef.downloadUrl.await()
            Resource.Success(downloadUrl.toString())
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to upload image")
        }
    }
}
