package song.vault.ui.createpost

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import song.vault.data.repository.PostRepository
import song.vault.data.repository.YouTubeRepository
import song.vault.util.Genre
import song.vault.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CreatePostViewModel(
    private val postRepository: PostRepository,
    private val youTubeRepository: YouTubeRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    private val _artist = MutableStateFlow("")
    val artist: StateFlow<String> = _artist

    private val _musicLink = MutableStateFlow("")
    val musicLink: StateFlow<String> = _musicLink

    private val _caption = MutableStateFlow("")
    val caption: StateFlow<String> = _caption

    private val _selectedGenre = MutableStateFlow<Genre?>(null)
    val selectedGenre: StateFlow<Genre?> = _selectedGenre

    private val _thumbnailUrl = MutableStateFlow("")
    val thumbnailUrl: StateFlow<String> = _thumbnailUrl

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _createStatus = MutableStateFlow<Resource<String>?>(null)
    val createStatus: StateFlow<Resource<String>?> = _createStatus

    val postId: String? = savedStateHandle.get<String>("postId")

    fun setTitle(title: String) {
        _title.value = title
    }

    fun setArtist(artist: String) {
        _artist.value = artist
    }

    fun setMusicLink(link: String) {
        _musicLink.value = link
        autoFetchMetadata(link)
    }

    fun setCaption(caption: String) {
        _caption.value = caption
    }

    fun setGenre(genre: Genre) {
        _selectedGenre.value = genre
    }

    fun autoFetchMetadata(link: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when {
                    link.contains("youtube.com") || link.contains("youtu.be") -> {
                        val videoId = extractYouTubeId(link)
                        if (videoId != null) {
                            _thumbnailUrl.value = "https://img.youtube.com/vi/$videoId/0.jpg"
                        }
                    }
                    link.contains("spotify.com") -> {
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createPost() {
        viewModelScope.launch {
            _createStatus.value = Resource.Loading(null)
            
            val result = if (postId != null) {
                postRepository.updatePost(postId!!, _caption.value, _selectedGenre.value?.name)
            } else {
                postRepository.createPost(
                    musicTitle = _title.value,
                    musicArtist = _artist.value,
                    musicThumbnailUrl = _thumbnailUrl.value,
                    musicExternalUrl = _musicLink.value,
                    musicSource = determineMusicSource(_musicLink.value),
                    caption = _caption.value,
                    genre = _selectedGenre.value?.name
                )
            }
            
            _createStatus.value = result.mapSuccess { }
        }
    }

    private fun extractYouTubeId(link: String): String? {
        return when {
            link.contains("youtu.be") -> link.substringAfterLast("/")
            link.contains("youtube.com") -> {
                val index = link.indexOf("v=")
                if (index != -1) link.substring(index + 2).takeWhile { it != '&' } else null
            }
            else -> null
        }
    }

    private fun determineMusicSource(link: String): String {
        return when {
            link.contains("youtube") || link.contains("youtu.be") -> "YouTube"
            link.contains("spotify") -> "Spotify"
            else -> "Unknown"
        }
    }

    inline fun <T, R> Resource<T>.mapSuccess(transform: (T) -> R): Resource<R> {
        return when (this) {
            is Resource.Success -> Resource.Success(transform(data))
            is Resource.Error -> Resource.Error(message)
            is Resource.Loading -> Resource.Loading()
        }
    }
}
