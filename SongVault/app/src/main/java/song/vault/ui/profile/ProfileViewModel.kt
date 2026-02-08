package song.vault.ui.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import song.vault.data.local.entity.UserEntity
import song.vault.data.repository.PostRepository
import song.vault.data.repository.UserRepository
import song.vault.util.Resource
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository
) : ViewModel() {

    val currentUser: LiveData<UserEntity?> = userRepository.observeCurrentUser().asLiveData()

    private val _postCount = MutableLiveData<Int>(0)
    val postCount: LiveData<Int> = _postCount

    private val _updateState = MutableLiveData<ProfileUpdateState>(ProfileUpdateState.Idle)
    val updateState: LiveData<ProfileUpdateState> = _updateState

    fun loadPostCount(userId: String) {
        viewModelScope.launch {
            _postCount.value = postRepository.getUserPostCount(userId)
        }
    }

    fun updateProfile(
        displayName: String?,
        profileImageUrl: String?,
        favoriteGenre: String?,
        favoriteSong: String?,
        bio: String?
    ) {
        _updateState.value = ProfileUpdateState.Loading

        viewModelScope.launch {
            when (val result = userRepository.updateProfile(
                displayName,
                profileImageUrl,
                favoriteGenre,
                favoriteSong,
                bio
            )) {
                is Resource.Success -> _updateState.value = ProfileUpdateState.Success
                is Resource.Error -> _updateState.value = ProfileUpdateState.Error(result.message)
                is Resource.Loading -> _updateState.value = ProfileUpdateState.Loading
            }
        }
    }

    fun updateProfileWithImage(
        displayName: String?,
        imageUri: Uri?,
        favoriteGenre: String?,
        favoriteSong: String?,
        bio: String?
    ) {
        _updateState.value = ProfileUpdateState.Loading

        viewModelScope.launch {
            // If there's an image to upload, upload it first
            if (imageUri != null) {
                when (val uploadResult = userRepository.uploadProfileImage(imageUri)) {
                    is Resource.Success -> {
                        // Image uploaded, now update profile with the URL
                        val imageUrl = uploadResult.data
                        when (val result = userRepository.updateProfile(
                            displayName,
                            imageUrl,
                            favoriteGenre,
                            favoriteSong,
                            bio
                        )) {
                            is Resource.Success -> _updateState.value = ProfileUpdateState.Success
                            is Resource.Error -> _updateState.value = ProfileUpdateState.Error(result.message)
                            is Resource.Loading -> _updateState.value = ProfileUpdateState.Loading
                        }
                    }
                    is Resource.Error -> _updateState.value = ProfileUpdateState.Error(uploadResult.message)
                    is Resource.Loading -> _updateState.value = ProfileUpdateState.Loading
                }
            } else {
                // No image, just update display name
                when (val result = userRepository.updateProfile(
                    displayName,
                    null,
                    favoriteGenre,
                    favoriteSong,
                    bio
                )) {
                    is Resource.Success -> _updateState.value = ProfileUpdateState.Success
                    is Resource.Error -> _updateState.value = ProfileUpdateState.Error(result.message)
                    is Resource.Loading -> _updateState.value = ProfileUpdateState.Loading
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
        }
    }

    fun resetUpdateState() {
        _updateState.value = ProfileUpdateState.Idle
    }

    class Factory(
        private val userRepository: UserRepository,
        private val postRepository: PostRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                return ProfileViewModel(userRepository, postRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

sealed class ProfileUpdateState {
    data object Idle : ProfileUpdateState()
    data object Loading : ProfileUpdateState()
    data object Success : ProfileUpdateState()
    data class Error(val message: String) : ProfileUpdateState()
}
