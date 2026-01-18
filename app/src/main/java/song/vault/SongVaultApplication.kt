package song.vault

import android.app.Application
import song.vault.data.local.AppDatabase
import song.vault.data.remote.FirebaseAuthSource
import song.vault.data.repository.PostRepository
import song.vault.data.repository.UserRepository
import song.vault.data.repository.YouTubeRepository

class SongVaultApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val authSource: FirebaseAuthSource by lazy { FirebaseAuthSource() }
    val userRepository: UserRepository by lazy {
        UserRepository(database.userDao(), authSource)
    }
    val postRepository: PostRepository by lazy {
        PostRepository(database.postDao(), userRepository)
    }
    val youTubeRepository: YouTubeRepository by lazy { YouTubeRepository() }
}
