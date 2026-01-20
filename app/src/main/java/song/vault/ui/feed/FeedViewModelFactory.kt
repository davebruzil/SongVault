package song.vault.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import song.vault.data.local.dao.PostDao

class FeedViewModelFactory(
    private val postDao: PostDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FeedViewModel::class.java)) {
            return FeedViewModel(postDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
