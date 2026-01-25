package song.vault.ui.post.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import song.vault.data.local.relation.PostWithUser
import song.vault.data.repository.PostRepository
import song.vault.data.repository.UserRepository
import song.vault.util.Resource
import kotlinx.coroutines.launch

class PostDetailViewModel(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _postWithUser = MutableLiveData<PostWithUser?>(null)
    val postWithUser: LiveData<PostWithUser?> = _postWithUser

    private val _isOwner = MutableLiveData<Boolean>(false)
    val isOwner: LiveData<Boolean> = _isOwner

    private val _deleteState = MutableLiveData<DeleteState>(DeleteState.Idle)
    val deleteState: LiveData<DeleteState> = _deleteState

    private val _updateState = MutableLiveData<UpdateState>(UpdateState.Idle)
    val updateState: LiveData<UpdateState> = _updateState

    fun loadPost(postId: String) {
        viewModelScope.launch {
            try {
                val post = postRepository.getAllPostsWithUsers()
                    .find { it.post.id == postId }

                _postWithUser.value = post

                // Check if current user is the owner
                val currentUser = userRepository.getCurrentUser()
                _isOwner.value = post?.post?.userId == currentUser?.uid
            } catch (e: Exception) {
                _postWithUser.value = null
            }
        }
    }

    fun updatePost(postId: String, caption: String?, genre: String?) {
        _updateState.value = UpdateState.Loading

        viewModelScope.launch {
            when (val result = postRepository.updatePost(postId, caption, genre)) {
                is Resource.Success -> {
                    _updateState.value = UpdateState.Success
                    loadPost(postId)  // Refresh
                }
                is Resource.Error -> _updateState.value = UpdateState.Error(result.message)
                is Resource.Loading -> _updateState.value = UpdateState.Loading
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

    sealed class DeleteState {
        object Idle : DeleteState()
        object Loading : DeleteState()
        object Success : DeleteState()
        data class Error(val message: String) : DeleteState()
    }

    sealed class UpdateState {
        object Idle : UpdateState()
        object Loading : UpdateState()
        object Success : UpdateState()
        data class Error(val message: String) : UpdateState()
    }

    class Factory(
        private val postRepository: PostRepository,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PostDetailViewModel(postRepository, userRepository) as T
        }
    }
}
