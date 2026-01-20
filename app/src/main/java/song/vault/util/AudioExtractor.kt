package song.vault.util

import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object AudioExtractor {

    private const val TAG = "AudioExtractor"
    private const val EXTRACTION_TIMEOUT_SECONDS = 60L

    data class AudioInfo(
        val audioUrl: String,
        val title: String,
        val duration: Int,
        val headers: Map<String, String> = emptyMap()
    )

    suspend fun extractAudioUrl(videoId: String): Resource<AudioInfo> = withContext(Dispatchers.IO) {
        val executor = Executors.newSingleThreadExecutor()
        val startTime = System.currentTimeMillis()

        try {
            val url = "https://www.youtube.com/watch?v=$videoId"
            Log.d(TAG, "=== STARTING EXTRACTION ===")
            Log.d(TAG, "Video ID: $videoId")

            val future = executor.submit(Callable {
                val request = YoutubeDLRequest(url)
                request.addOption("-f", "bestaudio/best")
                request.addOption("--no-playlist")
                request.addOption("--no-warnings")
                request.addOption("--no-check-certificate")
                // Use web client - has audio formats available
                request.addOption("--extractor-args", "youtube:player_client=web")

                Log.d(TAG, "Running yt-dlp getInfo...")
                val response = YoutubeDL.getInstance().getInfo(request)
                Log.d(TAG, "yt-dlp response received")
                response
            })

            val response = try {
                future.get(EXTRACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            } catch (e: TimeoutException) {
                future.cancel(true)
                Log.e(TAG, "Extraction timed out")
                return@withContext Resource.Error("Extraction timed out")
            }

            val audioUrl = response.url
            if (audioUrl.isNullOrEmpty()) {
                // Try fallback from formats
                val format = response.formats?.firstOrNull {
                    it.acodec != null && it.acodec != "none" && it.url != null
                }
                val fallbackUrl = format?.url
                if (fallbackUrl.isNullOrEmpty()) {
                    return@withContext Resource.Error("No audio URL found")
                }
                val headers = mapOf("User-Agent" to "Mozilla/5.0")
                return@withContext Resource.Success(AudioInfo(fallbackUrl, response.title ?: "Unknown", response.duration?.toInt() ?: 0, headers))
            }

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "Extraction completed in ${elapsed}ms")
            Log.d(TAG, "Audio URL: $audioUrl")

            // Get headers from yt-dlp response - use them EXACTLY as provided
            val headers = mutableMapOf<String, String>()

            // Copy HTTP headers from yt-dlp response without modification
            response.httpHeaders?.forEach { (key, value) ->
                headers[key] = value.toString()
                Log.d(TAG, "Header from yt-dlp: $key = $value")
            }

            // Only add User-Agent if yt-dlp didn't provide one
            if (!headers.containsKey("User-Agent")) {
                headers["User-Agent"] = "Mozilla/5.0"
            }

            Log.d(TAG, "Final headers: $headers")

            Resource.Success(AudioInfo(audioUrl, response.title ?: "Unknown", response.duration?.toInt() ?: 0, headers))

        } catch (e: Exception) {
            Log.e(TAG, "Extraction failed: ${e.message}", e)
            Resource.Error(e.message ?: "Failed to extract audio")
        } finally {
            executor.shutdownNow()
        }
    }
}
