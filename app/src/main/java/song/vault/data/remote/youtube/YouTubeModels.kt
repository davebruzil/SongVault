package song.vault.data.remote.youtube

import com.google.gson.annotations.SerializedName

data class YouTubeSearchResponse(
    @SerializedName("items") val items: List<YouTubeSearchItem>,
    @SerializedName("nextPageToken") val nextPageToken: String?
)

data class YouTubeSearchItem(
    @SerializedName("id") val id: VideoId,
    @SerializedName("snippet") val snippet: Snippet
)

data class VideoId(
    @SerializedName("videoId") val videoId: String
)

data class Snippet(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("thumbnails") val thumbnails: Thumbnails,
    @SerializedName("channelTitle") val channelTitle: String
)

data class Thumbnails(
    @SerializedName("default") val default: Thumbnail?,
    @SerializedName("medium") val medium: Thumbnail?,
    @SerializedName("high") val high: Thumbnail?
)

data class Thumbnail(
    @SerializedName("url") val url: String,
    @SerializedName("width") val width: Int,
    @SerializedName("height") val height: Int
)

// Simplified model for app usage
data class YouTubeVideo(
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val channelName: String,
    val description: String
) {
    companion object {
        fun fromSearchItem(item: YouTubeSearchItem): YouTubeVideo {
            return YouTubeVideo(
                videoId = item.id.videoId,
                title = item.snippet.title,
                thumbnailUrl = item.snippet.thumbnails.high?.url
                    ?: item.snippet.thumbnails.medium?.url
                    ?: item.snippet.thumbnails.default?.url
                    ?: "",
                channelName = item.snippet.channelTitle,
                description = item.snippet.description
            )
        }
    }
}
