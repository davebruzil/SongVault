package song.vault.data.remote.youtube

import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeService {
    @GET("search")
    suspend fun searchSongs(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 20,
        @Query("key") apiKey: String,
        @Query("videoCategoryId") categoryId: String = "10"
    ): YouTubeSearchResponse
}



