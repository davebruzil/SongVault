package song.vault.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import song.vault.data.local.dao.PostDao
import song.vault.data.local.entity.PostEntity
import song.vault.util.Genre
import song.vault.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class FeedViewModel(
    private val postDao: PostDao
) : ViewModel() {

    private val _selectedGenre = MutableStateFlow<Genre?>(null)
    val selectedGenre: StateFlow<Genre?> = _selectedGenre

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _deleteStatus = MutableStateFlow<Resource<Unit>?>(null)
    val deleteStatus: StateFlow<Resource<Unit>?> = _deleteStatus

    val allPosts: Flow<List<PostEntity>> = postDao.observeAllPosts()

    val filteredPosts: Flow<List<PostEntity>> = combine(
        allPosts,
        _selectedGenre,
        _searchQuery
    ) { posts, genre, query ->
        posts.filter { post ->
            val genreMatch = genre == null || post.genre == genre.name
            val queryMatch = query.isBlank() ||
                    post.musicTitle.contains(query, ignoreCase = true) ||
                    post.musicArtist.contains(query, ignoreCase = true) ||
                    (post.caption?.contains(query, ignoreCase = true) ?: false)
            genreMatch && queryMatch
        }
    }

    fun setSelectedGenre(genre: Genre?) {
        _selectedGenre.value = genre
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearFilters() {
        _selectedGenre.value = null
        _searchQuery.value = ""
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                postDao.deletePost(postId)
                _deleteStatus.value = Resource.Success(Unit)
            } catch (e: Exception) {
                _deleteStatus.value = Resource.Error(e.message ?: "Unknown error")
            }
        }
    }
}
