package song.vault.data.repository

import song.vault.BuildConfig
import song.vault.data.local.dao.PostDao
import song.vault.data.local.entity.PostEntity
import song.vault.data.remote.youtube.YouTubeClient
import song.vault.data.remote.youtube.YouTubeVideo
import song.vault.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class YouTubeRepository(
    private val postDao: PostDao? = null
) {

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

    suspend fun getTrendingContent(): Resource<List<PostEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.searchVideos(
                    query = "trending music",
                    maxResults = 20,
                    pageToken = null,
                    apiKey = apiKey
                )
                val posts = response.items.mapIndexed { index, item ->
                    PostEntity(
                        id = UUID.randomUUID().toString(),
                        userId = "trending_${index}",
                        userDisplayName = "Trending",
                        userProfileImageUrl = null,
                        musicTitle = item.snippet.title.take(100),
                        musicArtist = item.snippet.channelTitle.take(100),
                        musicThumbnailUrl = item.snippet.thumbnails.high?.url ?: item.snippet.thumbnails.medium?.url,
                        musicExternalUrl = "https://www.youtube.com/watch?v=${item.id.videoId}",
                        musicSource = "YouTube",
                        caption = item.snippet.description.take(500),
                        genre = "Trending"
                    )
                }
                Resource.Success(posts)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Failed to fetch trending content")
            }
        }
    }

    suspend fun getTrendingByGenre(genre: String): Resource<List<PostEntity>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.searchVideos(
                    query = "trending $genre music",
                    maxResults = 20,
                    pageToken = null,
                    apiKey = apiKey
                )
                val posts = response.items.mapIndexed { index, item ->
                    PostEntity(
                        id = UUID.randomUUID().toString(),
                        userId = "trending_${index}",
                        userDisplayName = "Trending",
                        userProfileImageUrl = null,
                        musicTitle = item.snippet.title.take(100),
                        musicArtist = item.snippet.channelTitle.take(100),
                        musicThumbnailUrl = item.snippet.thumbnails.high?.url ?: item.snippet.thumbnails.medium?.url,
                        musicExternalUrl = "https://www.youtube.com/watch?v=${item.id.videoId}",
                        musicSource = "YouTube",
                        caption = item.snippet.description.take(500),
                        genre = genre
                    )
                }
                Resource.Success(posts)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Failed to fetch trending content by genre")
            }
        }
    }
}
