package song.vault

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDL.UpdateChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import song.vault.data.local.AppDatabase
import song.vault.data.remote.FirebaseAuthSource
import song.vault.data.repository.PostRepository
import song.vault.data.repository.UserRepository
import song.vault.data.repository.YouTubeRepository

class SongVaultApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val authSource: FirebaseAuthSource by lazy { FirebaseAuthSource() }
    val userRepository: UserRepository by lazy {
        UserRepository(database.userDao(), authSource, this)
    }
    val postRepository: PostRepository by lazy {
        PostRepository(database.postDao(), userRepository)
    }
    val youtubeRepository: YouTubeRepository by lazy { YouTubeRepository(database.postDao()) }

    var isYtDlpReady = false
        private set

    override fun onCreate() {
        super.onCreate()
        initYtDlp()
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun initYtDlp() {
        Log.d("SongVaultApp", "=== STARTING YT-DLP INIT ===")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("SongVaultApp", "Calling YoutubeDL.init()...")
                YoutubeDL.getInstance().init(this@SongVaultApplication)
                Log.d("SongVaultApp", "yt-dlp initialized successfully")

                // Update to latest nightly for 403 fixes
                Log.d("SongVaultApp", "Updating yt-dlp to nightly...")
                try {
                    val status = YoutubeDL.getInstance().updateYoutubeDL(
                        this@SongVaultApplication,
                        UpdateChannel.NIGHTLY
                    )
                    Log.d("SongVaultApp", "yt-dlp update status: $status")
                } catch (e: Exception) {
                    Log.w("SongVaultApp", "yt-dlp update failed (using bundled): ${e.message}")
                }

                isYtDlpReady = true
                showToast("yt-dlp ready!")
                Log.d("SongVaultApp", "=== YT-DLP READY ===")

            } catch (e: Exception) {
                Log.e("SongVaultApp", "=== YT-DLP INIT FAILED ===")

                // Get full stack trace as string
                val sw = java.io.StringWriter()
                e.printStackTrace(java.io.PrintWriter(sw))
                val stackTrace = sw.toString()

                Log.e("SongVaultApp", "Full error: $stackTrace")

                // Show first part of stack trace in toast
                val shortError = stackTrace.take(200)
                showToast(shortError)
            }
        }
    }
}
