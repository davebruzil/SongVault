package song.vault.util

import android.util.Patterns

object ValidationUtil {
    fun isValidEmail(email: String): Boolean =
        email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()

    fun isValidPassword(password: String): Boolean =
        password.length >= 6

    fun isValidDisplayName(name: String): Boolean =
        name.length in 2..30

    fun isValidUrl(url: String): Boolean =
        url.isNotBlank() && Patterns.WEB_URL.matcher(url).matches()
}
