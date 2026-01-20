package song.vault.data.repository

import song.vault.BuildConfig
import song.vault.data.remote.youtube.YouTubeClient
import song.vault.data.remote.youtube.YouTubeVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YouTubeRepository {

    private val apiService = YouTubeClient.apiService
    private val apiKey = BuildConfig.YOUTUBE_API_KEY

    suspend fun searchSongs(query: String): Result<List<YouTubeVideo>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.searchSongs(
                    query = "$query song",
                    apiKey = apiKey
                )
                val videos = response.items.map { YouTubeVideo.fromSearchItem(it) }
                Result.success(videos)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun searchVideos(
        query: String,
        maxResults: Int = 20,
        pageToken: String? = null
    ): Result<Pair<List<YouTubeVideo>, String?>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.searchVideos(
                    query = query,
                    maxResults = maxResults,
                    pageToken = pageToken,
                    apiKey = apiKey
                )
                val videos = response.items.map { YouTubeVideo.fromSearchItem(it) }
                Result.success(Pair(videos, response.nextPageToken))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
