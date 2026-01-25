package song.vault.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import song.vault.data.local.relation.PostWithUser
import song.vault.data.repository.PostRepository
import song.vault.util.Resource
import kotlinx.coroutines.launch

class FeedViewModel(private val postRepository: PostRepository) : ViewModel() {

    private val _syncState = MutableLiveData<SyncState>(SyncState.Idle)
    val syncState: LiveData<SyncState> = _syncState

    private val _selectedGenre = MutableLiveData<String?>(null)  // null = "All"
    val selectedGenre: LiveData<String?> = _selectedGenre

    // Reactive feed that updates based on selected genre
    val allPosts: LiveData<List<PostWithUser>> = postRepository.observeAllPostsWithUsers().asLiveData()
    
    // Individual genre observables (we'll use the selectedGenre to filter in the fragment)
    private val _filteredPosts = MutableLiveData<List<PostWithUser>>(emptyList())
    val filteredPosts: LiveData<List<PostWithUser>> = _filteredPosts

    fun syncFeed() {
        _syncState.value = SyncState.Loading

        viewModelScope.launch {
            when (val result = postRepository.syncAllPosts()) {
                is Resource.Success -> _syncState.value = SyncState.Success
                is Resource.Error -> _syncState.value = SyncState.Error(result.message)
                is Resource.Loading -> _syncState.value = SyncState.Loading
            }
        }
    }

    fun filterByGenre(genre: String?) {
        _selectedGenre.value = genre

        viewModelScope.launch {
            if (genre == null) {
                // Show all posts
                _filteredPosts.value = postRepository.getAllPostsWithUsers()
            } else {
                // Filter by genre
                _filteredPosts.value = postRepository.getPostsByGenre(genre)
            }
        }
    }

    sealed class SyncState {
        object Idle : SyncState()
        object Loading : SyncState()
        object Success : SyncState()
        data class Error(val message: String) : SyncState()
    }

    class Factory(private val postRepository: PostRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FeedViewModel(postRepository) as T
        }
    }
}
