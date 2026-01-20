package song.vault.ui.youtube

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import song.vault.data.remote.youtube.YouTubeVideo
import song.vault.data.repository.YouTubeRepository
import kotlinx.coroutines.launch

class YouTubeSearchViewModel(
    private val youTubeRepository: YouTubeRepository
) : ViewModel() {

    private val _searchState = MutableLiveData<SearchState>(SearchState.Idle)
    val searchState: LiveData<SearchState> = _searchState

    private val _videos = MutableLiveData<List<YouTubeVideo>>(emptyList())
    val videos: LiveData<List<YouTubeVideo>> = _videos

    private val _selectedVideo = MutableLiveData<YouTubeVideo?>()
    val selectedVideo: LiveData<YouTubeVideo?> = _selectedVideo

    fun searchSongs(query: String) {
        if (query.isBlank()) return

        _searchState.value = SearchState.Loading
        viewModelScope.launch {
            youTubeRepository.searchSongs(query)
                .onSuccess { results ->
                    _videos.value = results
                    _searchState.value = SearchState.Success
                }
                .onFailure { error ->
                    _searchState.value = SearchState.Error(error.message ?: "Unknown error")
                }
        }
    }

    fun selectVideo(video: YouTubeVideo) {
        _selectedVideo.value = video
    }

    fun clearSelection() {
        _selectedVideo.value = null
    }

    class Factory(
        private val youTubeRepository: YouTubeRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(YouTubeSearchViewModel::class.java)) {
                return YouTubeSearchViewModel(youTubeRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

sealed class SearchState {
    data object Idle : SearchState()
    data object Loading : SearchState()
    data object Success : SearchState()
    data class Error(val message: String) : SearchState()
}
