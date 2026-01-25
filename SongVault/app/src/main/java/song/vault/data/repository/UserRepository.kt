package song.vault.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import song.vault.BuildConfig
import song.vault.data.local.dao.UserDao
import song.vault.data.local.entity.UserEntity
import song.vault.data.remote.FirebaseAuthSource
import song.vault.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

class UserRepository(
    private val userDao: UserDao,
    private val authSource: FirebaseAuthSource,
    private val context: Context
) {
    private val firestore = FirebaseFirestore.getInstance()

    init {
        initCloudinary()
    }

    private fun initCloudinary() {
        try {
            MediaManager.get()
        } catch (e: IllegalStateException) {
            val config = mapOf(
                "cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME,
                "api_key" to BuildConfig.CLOUDINARY_API_KEY,
                "api_secret" to BuildConfig.CLOUDINARY_API_SECRET
            )
            MediaManager.init(context, config)
        }
    }

    val isLoggedIn: Boolean get() = authSource.isLoggedIn
    val currentFirebaseUser: FirebaseUser? get() = authSource.currentUser

    fun observeCurrentUser(): Flow<UserEntity?> = userDao.observeCurrentUser()
    suspend fun getCurrentUser(): UserEntity? = userDao.getCurrentUser()

    suspend fun login(email: String, password: String): Resource<UserEntity> {
        return when (val result = authSource.login(email, password)) {
            is Resource.Success -> {
                val user = result.data
                Log.d("UserRepository", "Login successful for uid: ${user.uid}")

                // Fetch profile data from Firestore (including Cloudinary image URL)
                val firestoreData = try {
                    val doc = firestore.collection("users").document(user.uid).get().await()
                    Log.d("UserRepository", "Firestore doc exists: ${doc.exists()}, data: ${doc.data}")
                    doc
                } catch (e: Exception) {
                    Log.e("UserRepository", "Failed to fetch Firestore: ${e.message}")
                    null
                }

                val displayName = firestoreData?.getString("displayName") ?: user.displayName
                val profileImageUrl = firestoreData?.getString("profileImageUrl") ?: user.photoUrl?.toString()

                Log.d("UserRepository", "Login - using profileImageUrl: $profileImageUrl")

                val entity = UserEntity(user.uid, email, displayName, profileImageUrl, true)
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

                // Fetch profile data from Firestore (including custom Cloudinary image URL)
                val firestoreData = try {
                    firestore.collection("users").document(user.uid).get().await()
                } catch (e: Exception) {
                    Log.e("UserRepository", "Failed to fetch Firestore data: ${e.message}")
                    null
                }

                // Use Firestore data if available, otherwise use Google profile data
                val displayName = firestoreData?.getString("displayName") ?: user.displayName
                val profileImageUrl = firestoreData?.getString("profileImageUrl") ?: user.photoUrl?.toString()

                Log.d("UserRepository", "Google sign-in - profileImageUrl from Firestore: $profileImageUrl")

                val entity = UserEntity(
                    user.uid,
                    user.email ?: "",
                    displayName,
                    profileImageUrl,
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

        Log.d("UserRepository", "Updating profile - displayName: $displayName, imageUrl: $profileImageUrl")

        // Save to local Room DB
        userDao.updateProfile(uid, displayName, profileImageUrl)

        // Save to Firestore so it persists across logins
        try {
            val data = mutableMapOf<String, Any>("uid" to uid)
            displayName?.let { data["displayName"] = it }
            profileImageUrl?.let { data["profileImageUrl"] = it }

            // Use set with merge to create or update the document
            firestore.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .await()

            Log.d("UserRepository", "Profile saved to Firestore successfully")
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed to save profile to Firestore: ${e.message}")
            // Local is still updated, so don't fail completely
        }

        return Resource.Success(Unit)
    }

    suspend fun uploadProfileImage(imageUri: Uri): Resource<String> {
        val uid = currentFirebaseUser?.uid ?: return Resource.Error("Not logged in")

        return suspendCancellableCoroutine { continuation ->
            val requestId = MediaManager.get().upload(imageUri)
                .option("public_id", "profile_images/$uid")
                .option("overwrite", true)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        if (url != null) {
                            continuation.resume(Resource.Success(url))
                        } else {
                            continuation.resume(Resource.Error("Failed to get image URL"))
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        continuation.resume(Resource.Error(error.description ?: "Upload failed"))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        continuation.resume(Resource.Error("Upload rescheduled: ${error.description}"))
                    }
                })
                .dispatch()
        }
    }
}
