package song.vault.ui.postdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import song.vault.data.local.dao.PostDao
import song.vault.data.local.entity.PostEntity
import song.vault.data.repository.PostRepository
import song.vault.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PostDetailViewModel(
    private val postDao: PostDao,
    private val postRepository: PostRepository
) : ViewModel() {

    private val _post = MutableStateFlow<PostEntity?>(null)
    val post: StateFlow<PostEntity?> = _post

    private val _deleteStatus = MutableStateFlow<Resource<Unit>?>(null)
    val deleteStatus: StateFlow<Resource<Unit>?> = _deleteStatus

    fun loadPost(postId: String) {
        viewModelScope.launch {
            try {
                val post = postDao.getPostById(postId)
                _post.value = post
            } catch (e: Exception) {
                _deleteStatus.value = Resource.Error(e.message ?: "Failed to load post")
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            _deleteStatus.value = Resource.Loading()
            try {
                postRepository.deletePost(postId)
                _deleteStatus.value = Resource.Success(Unit)
            } catch (e: Exception) {
                _deleteStatus.value = Resource.Error(e.message ?: "Failed to delete post")
            }
        }
    }
}
