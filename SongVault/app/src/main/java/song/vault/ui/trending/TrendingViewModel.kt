package song.vault.ui.trending

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import song.vault.data.remote.youtube.YouTubeVideo
import song.vault.data.repository.YouTubeRepository
import kotlinx.coroutines.launch

class TrendingViewModel(private val youTubeRepository: YouTubeRepository) : ViewModel() {

    private val _trendingVideos = MutableLiveData<List<YouTubeVideo>>(emptyList())
    val trendingVideos: LiveData<List<YouTubeVideo>> = _trendingVideos

    private val _loadState = MutableLiveData<LoadState>(LoadState.Idle)
    val loadState: LiveData<LoadState> = _loadState

    fun loadTrendingMusic() {
        _loadState.value = LoadState.Loading

        viewModelScope.launch {
            youTubeRepository.searchSongs("trending music today")
                .fold(
                    onSuccess = { videos ->
                        _trendingVideos.value = videos
                        _loadState.value = LoadState.Success
                    },
                    onFailure = { error ->
                        _loadState.value = LoadState.Error(error.message ?: "Failed to load trending")
                    }
                )
        }
    }

    sealed class LoadState {
        object Idle : LoadState()
        object Loading : LoadState()
        object Success : LoadState()
        data class Error(val message: String) : LoadState()
    }

    class Factory(private val youTubeRepository: YouTubeRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TrendingViewModel(youTubeRepository) as T
        }
    }
}
