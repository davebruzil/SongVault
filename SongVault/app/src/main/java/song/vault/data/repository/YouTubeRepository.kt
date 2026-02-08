package song.vault.data.repository

import song.vault.BuildConfig
import song.vault.data.remote.youtube.YouTubeClient
import song.vault.data.remote.youtube.YouTubeVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import android.util.Log

class YouTubeRepository {

    private val apiService = YouTubeClient.apiService

    private val apiKey = BuildConfig.YOUTUBE_API_KEY

    init {
        if (apiKey.isEmpty()) {
            Log.e("YouTubeRepository", "⚠️ YOUTUBE_API_KEY is EMPTY. Search will fail with HTTP 403. Check local.properties.")
        }
    }

    /**
     * Search for songs - appends "song" for general searches
     */
    suspend fun searchSongs(query: String): Result<List<YouTubeVideo>> {
        if (apiKey.isEmpty()) return Result.failure(Exception("YouTube API Key is missing"))
        
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.searchSongs(
                    query = "$query song",
                    apiKey = apiKey
                )
                val videos = response.items.map { YouTubeVideo.fromSearchItem(it) }
                Result.success(videos)
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e(
                    "YouTubeRepository",
                    "searchSongs HTTP ${e.code()} error: ${e.message()} | body=${errorBody}",
                    e
                )
                Result.failure(e)
            } catch (e: Exception) {
                Log.e("YouTubeRepository", "searchSongs failed: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Search for artist/band - better for n8n results
     * Adds "band official" to get actual band music, not movie soundtracks
     */
    suspend fun searchArtist(artistName: String): Result<List<YouTubeVideo>> {
        if (apiKey.isEmpty()) return Result.failure(Exception("YouTube API Key is missing"))

        return withContext(Dispatchers.IO) {
            try {
                // Add "band official" to prioritize actual bands over movies/shows with same name
                val searchQuery = "$artistName band official"
                Log.d("YouTubeRepository", "Searching artist: $searchQuery")

                val response = apiService.searchSongs(
                    query = searchQuery,
                    apiKey = apiKey
                )
                val videos = response.items.map { YouTubeVideo.fromSearchItem(it) }
                Log.d("YouTubeRepository", "Found ${videos.size} videos for $artistName")
                Result.success(videos)
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e(
                    "YouTubeRepository",
                    "searchArtist HTTP ${e.code()} error: ${e.message()} | body=${errorBody}",
                    e
                )
                Result.failure(e)
            } catch (e: Exception) {
                Log.e("YouTubeRepository", "searchArtist failed for '$artistName': ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun searchVideos(
        query: String,
        maxResults: Int = 20,
        pageToken: String? = null
    ): Result<Pair<List<YouTubeVideo>, String?>> {
        if (apiKey.isEmpty()) return Result.failure(Exception("YouTube API Key is missing"))

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
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e(
                    "YouTubeRepository",
                    "searchVideos HTTP ${e.code()} error: ${e.message()} | body=${errorBody}",
                    e
                )
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
