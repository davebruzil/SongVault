package song.vault.data.remote.youtube

import com.google.gson.annotations.SerializedName

/**
 * Request model for n8n webhook
 */
data class N8nSearchRequest(
    @SerializedName("query") val query: String,
    @SerializedName("maxResults") val maxResults: Int = 15
)

/**
 * Response model from n8n webhook - Returns artists list
 * The app will then search YouTube for each artist
 */
data class N8nSearchResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("artists") val artists: List<String>?,
    @SerializedName("query") val query: String?,
    @SerializedName("count") val count: Int?,
    @SerializedName("timestamp") val timestamp: Long?,
    @SerializedName("error") val error: String?
)
