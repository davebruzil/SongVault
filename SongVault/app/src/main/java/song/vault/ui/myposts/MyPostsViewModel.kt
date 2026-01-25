package song.vault.ui.myposts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import song.vault.data.local.entity.PostEntity
import song.vault.data.repository.PostRepository
import song.vault.data.repository.UserRepository
import song.vault.util.Resource
import kotlinx.coroutines.launch

class MyPostsViewModel(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _userId = MutableLiveData<String>()

    val posts: LiveData<List<PostEntity>> = _userId.switchMap { userId ->
        postRepository.observeUserPosts(userId).asLiveData()
    }

    private val _syncState = MutableLiveData<SyncState>(SyncState.Idle)
    val syncState: LiveData<SyncState> = _syncState

    private val _deleteState = MutableLiveData<DeleteState>(DeleteState.Idle)
    val deleteState: LiveData<DeleteState> = _deleteState

    init {
        userRepository.currentFirebaseUser?.uid?.let { uid ->
            _userId.value = uid
            syncPosts(uid)
        }
    }

    fun syncPosts(userId: String? = null) {
        val uid = userId ?: _userId.value ?: return

        _syncState.value = SyncState.Loading

        viewModelScope.launch {
            when (val result = postRepository.syncUserPosts(uid)) {
                is Resource.Success -> _syncState.value = SyncState.Success
                is Resource.Error -> _syncState.value = SyncState.Error(result.message)
                is Resource.Loading -> _syncState.value = SyncState.Loading
            }
        }
    }

    fun deletePost(postId: String) {
        _deleteState.value = DeleteState.Loading

        viewModelScope.launch {
            when (val result = postRepository.deletePost(postId)) {
                is Resource.Success -> _deleteState.value = DeleteState.Success
                is Resource.Error -> _deleteState.value = DeleteState.Error(result.message)
                is Resource.Loading -> _deleteState.value = DeleteState.Loading
            }
        }
    }

    fun resetDeleteState() {
        _deleteState.value = DeleteState.Idle
    }

    class Factory(
        private val postRepository: PostRepository,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MyPostsViewModel::class.java)) {
                return MyPostsViewModel(postRepository, userRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

sealed class SyncState {
    data object Idle : SyncState()
    data object Loading : SyncState()
    data object Success : SyncState()
    data class Error(val message: String) : SyncState()
}

sealed class DeleteState {
    data object Idle : DeleteState()
    data object Loading : DeleteState()
    data object Success : DeleteState()
    data class Error(val message: String) : DeleteState()
}
