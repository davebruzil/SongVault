package song.vault.util

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    data class Loading<out T>(val data: T? = null) : Resource<T>()
}
