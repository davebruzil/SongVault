package song.vault.ui.post.create

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import song.vault.data.local.entity.PostEntity
import song.vault.data.repository.MusicSearchProcessor
import song.vault.data.repository.PostRepository
import song.vault.data.repository.YouTubeRepository
import song.vault.data.remote.youtube.N8nWebhookService
import song.vault.data.remote.youtube.YouTubeVideo
import song.vault.util.Resource
import song.vault.util.ValidationUtil
import kotlinx.coroutines.launch

class PostCreateViewModel(
    private val postRepository: PostRepository,
    private val youTubeRepository: YouTubeRepository,
    private val n8nWebhookService: N8nWebhookService
) : ViewModel() {

    private val musicSearchProcessor = MusicSearchProcessor(n8nWebhookService, youTubeRepository)

    private val _createState = MutableLiveData<CreateState>(CreateState.Idle)
    val createState: LiveData<CreateState> = _createState

    private val _musicMetadata = MutableLiveData<YouTubeVideo?>(null)
    val musicMetadata: LiveData<YouTubeVideo?> = _musicMetadata

    private val _autoFetchState = MutableLiveData<AutoFetchState>(AutoFetchState.Idle)
    val autoFetchState: LiveData<AutoFetchState> = _autoFetchState

    private val _validationErrors = MutableLiveData<Map<String, String>>(emptyMap())
    val validationErrors: LiveData<Map<String, String>> = _validationErrors

    fun autoFetchMetadata(musicLink: String) {
        _autoFetchState.value = AutoFetchState.Loading

        viewModelScope.launch {
            try {
                // Try to extract video ID and search for metadata
                val videoId = extractYouTubeVideoId(musicLink)
                
                if (videoId != null) {
                    // Search for the video to get metadata
                    youTubeRepository.searchVideos(videoId).fold(
                        onSuccess = { (videos, _) ->
                            if (videos.isNotEmpty()) {
                                _musicMetadata.value = videos.first()
                                _autoFetchState.value = AutoFetchState.Success
                            } else {
                                _autoFetchState.value = AutoFetchState.Error("Video not found")
                            }
                        },
                        onFailure = { error ->
                            _autoFetchState.value = AutoFetchState.Error(error.message ?: "Failed to fetch metadata")
                        }
                    )
                } else {
                    _autoFetchState.value = AutoFetchState.Error("Invalid YouTube link")
                }
            } catch (e: Exception) {
                _autoFetchState.value = AutoFetchState.Error(e.message ?: "Failed to fetch metadata")
            }
        }
    }

    fun createPost(
        musicTitle: String,
        musicArtist: String,
        musicThumbnailUrl: String?,
        musicExternalUrl: String,
        musicSource: String,
        caption: String?,
        genre: String?
    ) {
        // Validate
        val errors = mutableMapOf<String, String>()

        if (!ValidationUtil.isValidDisplayName(musicTitle)) {
            errors["title"] = "Title must be 2-100 characters"
        }

        if (!ValidationUtil.isValidDisplayName(musicArtist)) {
            errors["artist"] = "Artist must be 2-100 characters"
        }

        if (!ValidationUtil.isValidUrl(musicExternalUrl)) {
            errors["link"] = "Invalid music link"
        }

        if (errors.isNotEmpty()) {
            _validationErrors.value = errors
            return
        }

        _createState.value = CreateState.Loading

        viewModelScope.launch {
            when (val result = postRepository.createPost(
                musicTitle = musicTitle,
                musicArtist = musicArtist,
                musicThumbnailUrl = musicThumbnailUrl,
                musicExternalUrl = musicExternalUrl,
                musicSource = musicSource,
                caption = caption,
                genre = genre
            )) {
                is Resource.Success -> _createState.value = CreateState.Success(result.data)
                is Resource.Error -> _createState.value = CreateState.Error(result.message)
                is Resource.Loading -> _createState.value = CreateState.Loading
            }
        }
    }

    private fun extractYouTubeVideoId(url: String): String? {
        return when {
            url.contains("youtube.com/watch") -> {
                url.substringAfter("v=").substringBefore("&")
            }
            url.contains("youtu.be/") -> {
                url.substringAfter("youtu.be/").substringBefore("?")
            }
            else -> null
        }
    }

    sealed class CreateState {
        object Idle : CreateState()
        object Loading : CreateState()
        data class Success(val post: PostEntity) : CreateState()
        data class Error(val message: String) : CreateState()
    }

    sealed class AutoFetchState {
        object Idle : AutoFetchState()
        object Loading : AutoFetchState()
        object Success : AutoFetchState()
        data class Error(val message: String) : AutoFetchState()
    }

    class Factory(
        private val postRepository: PostRepository,
        private val youTubeRepository: YouTubeRepository,
        private val n8nWebhookService: N8nWebhookService
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PostCreateViewModel(postRepository, youTubeRepository, n8nWebhookService) as T
        }
    }
}
