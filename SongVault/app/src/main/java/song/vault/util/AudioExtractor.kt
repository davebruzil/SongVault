package song.vault.util

import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object AudioExtractor {

    private const val TAG = "AudioExtractor"
    private const val EXTRACTION_TIMEOUT_SECONDS = 30L  // Reduced from 60s

    data class AudioInfo(
        val audioUrl: String,
        val title: String,
        val duration: Int,
        val headers: Map<String, String> = emptyMap()
    )

    // Cache for recently extracted URLs (valid for ~6 hours typically)
    private data class CachedAudio(
        val info: AudioInfo,
        val timestamp: Long
    )
    private val urlCache = ConcurrentHashMap<String, CachedAudio>()
    private const val CACHE_DURATION_MS = 2 * 60 * 60 * 1000L  // 2 hours

    suspend fun extractAudioUrl(videoId: String): Resource<AudioInfo> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // Check cache first
        val cached = urlCache[videoId]
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) < CACHE_DURATION_MS) {
            Log.d(TAG, "Cache HIT for $videoId - returning instantly")
            return@withContext Resource.Success(cached.info)
        }

        val executor = Executors.newSingleThreadExecutor()

        try {
            val url = "https://www.youtube.com/watch?v=$videoId"
            Log.d(TAG, "=== STARTING EXTRACTION ===")
            Log.d(TAG, "Video ID: $videoId")

            val future = executor.submit(Callable {
                val request = YoutubeDLRequest(url)

                // Optimized options for faster extraction
                request.addOption("-f", "bestaudio[ext=m4a]/bestaudio/best")  // Prefer m4a (faster)
                request.addOption("--no-playlist")
                request.addOption("--no-warnings")
                request.addOption("--no-check-certificate")
                request.addOption("--no-cache-dir")  // Skip disk cache operations
                request.addOption("--socket-timeout", "10")  // Faster timeout
                request.addOption("--retries", "1")  // Less retries
                request.addOption("--fragment-retries", "1")
                // Use android client - often faster
                request.addOption("--extractor-args", "youtube:player_client=android")

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
                // Try fallback from formats - prefer audio-only formats
                val format = response.formats?.firstOrNull {
                    it.acodec != null && it.acodec != "none" &&
                    (it.vcodec == null || it.vcodec == "none") &&  // Audio only
                    it.url != null
                } ?: response.formats?.firstOrNull {
                    it.acodec != null && it.acodec != "none" && it.url != null
                }

                val fallbackUrl = format?.url
                if (fallbackUrl.isNullOrEmpty()) {
                    return@withContext Resource.Error("No audio URL found")
                }
                val headers = mapOf("User-Agent" to "Mozilla/5.0")
                val info = AudioInfo(fallbackUrl, response.title ?: "Unknown", response.duration?.toInt() ?: 0, headers)

                // Cache the result
                urlCache[videoId] = CachedAudio(info, System.currentTimeMillis())

                return@withContext Resource.Success(info)
            }

            val elapsed = System.currentTimeMillis() - startTime
            Log.d(TAG, "Extraction completed in ${elapsed}ms")
            Log.d(TAG, "Audio URL: ${audioUrl.take(80)}...")

            // Get headers from yt-dlp response
            val headers = mutableMapOf<String, String>()
            response.httpHeaders?.forEach { (key, value) ->
                headers[key] = value.toString()
            }
            if (!headers.containsKey("User-Agent")) {
                headers["User-Agent"] = "Mozilla/5.0"
            }

            val info = AudioInfo(audioUrl, response.title ?: "Unknown", response.duration?.toInt() ?: 0, headers)

            // Cache the result
            urlCache[videoId] = CachedAudio(info, System.currentTimeMillis())
            Log.d(TAG, "Cached URL for $videoId")

            Resource.Success(info)

        } catch (e: Exception) {
            Log.e(TAG, "Extraction failed: ${e.message}", e)
            Resource.Error(e.message ?: "Failed to extract audio")
        } finally {
            executor.shutdownNow()
        }
    }

    // Pre-extract audio for a video (call this when user is likely to play)
    suspend fun preExtract(videoId: String) = withContext(Dispatchers.IO) {
        if (!urlCache.containsKey(videoId)) {
            Log.d(TAG, "Pre-extracting $videoId")
            extractAudioUrl(videoId)
        }
    }

    // Clear old cache entries
    fun cleanCache() {
        val now = System.currentTimeMillis()
        urlCache.entries.removeIf { (_, cached) ->
            (now - cached.timestamp) > CACHE_DURATION_MS
        }
    }
}
