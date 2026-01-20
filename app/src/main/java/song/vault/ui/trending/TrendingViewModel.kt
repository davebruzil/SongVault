package song.vault.ui.trending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import song.vault.data.local.entity.PostEntity
import song.vault.data.repository.YouTubeRepository
import song.vault.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TrendingViewModel(
    private val youTubeRepository: YouTubeRepository
) : ViewModel() {

    private val _trendingPosts = MutableStateFlow<List<PostEntity>>(emptyList())
    val trendingPosts: StateFlow<List<PostEntity>> = _trendingPosts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadTrendingContent(genre: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = if (genre != null) {
                youTubeRepository.getTrendingByGenre(genre)
            } else {
                youTubeRepository.getTrendingContent()
            }

            when (result) {
                is Resource.Success -> {
                    _trendingPosts.value = result.data
                }
                is Resource.Error -> {
                    _error.value = result.message
                }
                is Resource.Loading -> {
                }
            }
            _isLoading.value = false
        }
    }

    fun addTrendingPostToFeed(post: PostEntity) {
        viewModelScope.launch {
        }
    }
}
