package song.vault.data.remote

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import song.vault.util.Resource
import kotlinx.coroutines.tasks.await

class FirebaseAuthSource {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val currentUser: FirebaseUser? get() = auth.currentUser
    val isLoggedIn: Boolean get() = currentUser != null

    suspend fun login(email: String, password: String): Resource<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { Resource.Success(it) } ?: Resource.Error("Login failed")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Login failed")
        }
    }

    suspend fun signInWithGoogle(idToken: String): Resource<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: return Resource.Error("Google sign-in failed")

            // Check if this is a new user and create Firestore document
            if (result.additionalUserInfo?.isNewUser == true) {
                firestore.collection("users").document(user.uid).set(
                    mapOf(
                        "uid" to user.uid,
                        "email" to (user.email ?: ""),
                        "displayName" to (user.displayName ?: ""),
                        "profileImageUrl" to (user.photoUrl?.toString() ?: ""),
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()
            }

            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Google sign-in failed")
        }
    }

    suspend fun register(email: String, password: String, displayName: String): Resource<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Resource.Error("Registration failed")

            user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(displayName).build()).await()

            firestore.collection("users").document(user.uid).set(
                mapOf("uid" to user.uid, "email" to email, "displayName" to displayName, "createdAt" to System.currentTimeMillis())
            ).await()

            Resource.Success(user)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Registration failed")
        }
    }

    fun logout() = auth.signOut()
}
