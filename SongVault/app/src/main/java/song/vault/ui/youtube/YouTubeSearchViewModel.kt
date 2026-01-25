package song.vault.ui.youtube

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import retrofit2.HttpException
import song.vault.data.remote.youtube.N8nClient
import song.vault.data.remote.youtube.N8nSearchRequest
import song.vault.data.remote.youtube.N8nWebhookService
import song.vault.data.remote.youtube.YouTubeVideo
import song.vault.data.repository.YouTubeRepository

class YouTubeSearchViewModel(
    private val youTubeRepository: YouTubeRepository,
    private val n8nWebhookService: N8nWebhookService = N8nClient.webhookService
) : ViewModel() {

    private val _searchState = MutableLiveData<SearchState>(SearchState.Idle)
    val searchState: LiveData<SearchState> = _searchState

    private val _videos = MutableLiveData<List<YouTubeVideo>>(emptyList())
    val videos: LiveData<List<YouTubeVideo>> = _videos

    private val _selectedVideo = MutableLiveData<YouTubeVideo?>()
    val selectedVideo: LiveData<YouTubeVideo?> = _selectedVideo

    // LiveData for artists from n8n
    private val _artists = MutableLiveData<List<String>>(emptyList())
    val artists: LiveData<List<String>> = _artists

    // Can load more indicator
    private val _canLoadMore = MutableLiveData(false)
    val canLoadMore: LiveData<Boolean> = _canLoadMore

    // Loading more state
    private val _isLoadingMore = MutableLiveData(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    // Flag to use n8n or direct search
    private var useN8nWebhook = true

    // Track which artists we've already loaded
    private var allArtists: List<String> = emptyList()
    private var loadedArtistIndex = 0
    private val videosPerArtist = 2
    private val artistsPerLoad = 5

    fun searchSongs(query: String) {
        if (query.isBlank()) return

        // Reset state for new search
        loadedArtistIndex = 0
        allArtists = emptyList()
        _videos.value = emptyList()
        _canLoadMore.value = false
        _searchState.value = SearchState.Loading

        if (useN8nWebhook) {
            searchWithN8nWebhook(query)
        } else {
            searchDirectYouTube(query)
        }
    }

    // n8n webhook search - gets artists, then searches YouTube
    private fun searchWithN8nWebhook(query: String) {
        viewModelScope.launch {
            try {
                println("📤 Sending to n8n webhook: '$query'")
                val n8nResponse = n8nWebhookService.searchMusic(
                    N8nSearchRequest(query = query)
                )

                if (!n8nResponse.success || n8nResponse.artists.isNullOrEmpty()) {
                    println("❌ n8n returned no artists, falling back to YouTube")
                    val errorMsg = n8nResponse.error ?: "No artists found"
                    println("❌ Error: $errorMsg")
                    searchDirectYouTube(query)
                    return@launch
                }

                // Store ALL artists from n8n
                allArtists = n8nResponse.artists
                _artists.value = allArtists
                println("🎸 Found ${allArtists.size} artists from n8n: ${allArtists.joinToString()}")

                // Load first batch
                loadNextBatch()

            } catch (e: HttpException) {
                // HTTP error from n8n (403, 500, etc.)
                println("🔥 HTTP ${e.code()} error from n8n webhook: ${e.message()}")
                println("🔄 Falling back to direct YouTube search")
                searchDirectYouTube(query)
            } catch (e: Exception) {
                // Network error or other exception
                println("🔥 Error in n8n webhook search: ${e.message}")
                e.printStackTrace()
                println("🔄 Falling back to direct YouTube search")
                searchDirectYouTube(query)
            }
        }
    }

    // Load next batch of artists
    private suspend fun loadNextBatch() {
        val startIndex = loadedArtistIndex
        val endIndex = minOf(startIndex + artistsPerLoad, allArtists.size)

        if (startIndex >= allArtists.size) {
            _canLoadMore.value = false
            return
        }

        val artistsToSearch = allArtists.subList(startIndex, endIndex)
        println("🔍 Loading artists ${startIndex + 1} to $endIndex: ${artistsToSearch.joinToString()}")

        // Search YouTube for each artist in parallel
        // Use searchArtist() which adds "band official" for better results
        val searchResults = artistsToSearch.map { artist ->
            viewModelScope.async {
                println("🔍 Searching YouTube for artist: $artist")
                youTubeRepository.searchArtist(artist)
            }
        }.awaitAll()

        // Collect results (take videosPerArtist per artist)
        val newVideos = mutableListOf<YouTubeVideo>()
        searchResults.forEach { result ->
            result.onSuccess { videos ->
                newVideos.addAll(videos.take(videosPerArtist))
            }
        }

        // Update loaded index
        loadedArtistIndex = endIndex

        // Check if we can load more
        _canLoadMore.value = loadedArtistIndex < allArtists.size

        // Add to existing videos
        val currentVideos = _videos.value.orEmpty()
        _videos.value = currentVideos + newVideos

        if (_videos.value.isNullOrEmpty()) {
            _searchState.value = SearchState.Error("No videos found")
        } else {
            _searchState.value = SearchState.Success
        }

        println("🎉 Loaded ${newVideos.size} videos. Total: ${_videos.value?.size}. Can load more: ${_canLoadMore.value}")
    }

    // Public function to load more results
    fun loadMore() {
        if (_isLoadingMore.value == true || _canLoadMore.value == false) return

        _isLoadingMore.value = true
        viewModelScope.launch {
            try {
                loadNextBatch()
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    // Original direct YouTube search (fallback)
    private fun searchDirectYouTube(query: String) {
        viewModelScope.launch {
            youTubeRepository.searchSongs(query)
                .onSuccess { results ->
                    _videos.value = results
                    _artists.value = emptyList()
                    _canLoadMore.value = false
                    _searchState.value = SearchState.Success
                }
                .onFailure { error ->
                    _searchState.value = SearchState.Error(error.message ?: "Unknown error")
                }
        }
    }

    fun setUseN8nWebhook(useN8n: Boolean) {
        useN8nWebhook = useN8n
    }

    fun isUsingN8nWebhook(): Boolean = useN8nWebhook

    fun selectVideo(video: YouTubeVideo) {
        _selectedVideo.value = video
    }

    fun clearSelection() {
        _selectedVideo.value = null
    }

    class Factory(
        private val youTubeRepository: YouTubeRepository,
        private val n8nWebhookService: N8nWebhookService = N8nClient.webhookService
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(YouTubeSearchViewModel::class.java)) {
                return YouTubeSearchViewModel(youTubeRepository, n8nWebhookService) as T
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
