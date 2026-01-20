package song.vault.ui.postdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import song.vault.data.local.dao.PostDao
import song.vault.data.repository.PostRepository

class PostDetailViewModelFactory(
    private val postDao: PostDao,
    private val postRepository: PostRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PostDetailViewModel::class.java)) {
            return PostDetailViewModel(postDao, postRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
