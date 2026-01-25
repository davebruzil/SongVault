package song.vault.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import song.vault.data.remote.youtube.N8nSearchRequest
import song.vault.data.remote.youtube.N8nWebhookService
import song.vault.data.remote.youtube.YouTubeVideo

/**
 * Processor that coordinates search between n8n workflow and YouTube API.
 * Flow: User query -> n8n webhook -> get artists -> search YouTube for each artist -> display in UI
 */
class MusicSearchProcessor(
    private val n8nWebhookService: N8nWebhookService,
    private val youTubeRepository: YouTubeRepository
) {

    /**
     * Process user search query through n8n workflow.
     * n8n returns a list of artists, then we search YouTube for each.
     */
    suspend fun processUserSearch(query: String): SearchResult = coroutineScope {
        try {
            // 1. Send query to n8n workflow
            val n8nResponse = n8nWebhookService.searchMusic(
                N8nSearchRequest(query = query)
            )

            // 2. Check if n8n returned artists
            if (!n8nResponse.success || n8nResponse.artists.isNullOrEmpty()) {
                val errorMsg = n8nResponse.error ?: "No artists found from n8n workflow"
                return@coroutineScope SearchResult.Error(errorMsg)
            }

            // 3. Get artists from n8n response
            val artists = n8nResponse.artists

            // 4. Search YouTube for each artist (limit to 5 artists, parallel)
            val artistsToSearch = artists.take(5)
            val allVideos = mutableListOf<YouTubeVideo>()

            val searchResults = artistsToSearch.map { artist ->
                async {
                    youTubeRepository.searchSongs(artist)
                }
            }.awaitAll()

            // Collect results (take top 4 per artist)
            searchResults.forEach { result ->
                result.onSuccess { videos ->
                    allVideos.addAll(videos.take(4))
                }
            }

            // 5. Return results
            if (allVideos.isNotEmpty()) {
                SearchResult.Success(
                    videos = allVideos,
                    sourceArtists = artists
                )
            } else {
                SearchResult.Error("No videos found for artists: ${artists.joinToString()}")
            }

        } catch (e: Exception) {
            SearchResult.Error("n8n search failed: ${e.message}")
        }
    }

    /**
     * Fallback to direct YouTube search if n8n fails.
     */
    suspend fun fallbackYouTubeSearch(query: String): SearchResult {
        return youTubeRepository.searchSongs(query)
            .fold(
                onSuccess = { videos ->
                    SearchResult.Success(
                        videos = videos,
                        sourceArtists = emptyList()
                    )
                },
                onFailure = { error ->
                    SearchResult.Error(error.message ?: "YouTube search failed")
                }
            )
    }

    sealed class SearchResult {
        data class Success(
            val videos: List<YouTubeVideo>,
            val sourceArtists: List<String>
        ) : SearchResult()

        data class Error(val message: String) : SearchResult()
    }
}
