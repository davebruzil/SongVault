package song.vault.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import song.vault.data.repository.UserRepository
import song.vault.util.Resource
import song.vault.util.ValidationUtil
import kotlinx.coroutines.launch

class AuthViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    private val _emailError = MutableLiveData<String?>()
    val emailError: LiveData<String?> = _emailError

    private val _passwordError = MutableLiveData<String?>()
    val passwordError: LiveData<String?> = _passwordError

    private val _displayNameError = MutableLiveData<String?>()
    val displayNameError: LiveData<String?> = _displayNameError

    private val _confirmPasswordError = MutableLiveData<String?>()
    val confirmPasswordError: LiveData<String?> = _confirmPasswordError

    val isLoggedIn: Boolean get() = userRepository.isLoggedIn

    fun login(email: String, password: String) {
        clearErrors()

        var hasError = false

        if (!ValidationUtil.isValidEmail(email)) {
            _emailError.value = "Please enter a valid email address"
            hasError = true
        }

        if (!ValidationUtil.isValidPassword(password)) {
            _passwordError.value = "Password must be at least 6 characters"
            hasError = true
        }

        if (hasError) return

        _authState.value = AuthState.Loading

        viewModelScope.launch {
            when (val result = userRepository.login(email, password)) {
                is Resource.Success -> _authState.value = AuthState.Success(result.data)
                is Resource.Error -> _authState.value = AuthState.Error(result.message)
                is Resource.Loading -> _authState.value = AuthState.Loading
            }
        }
    }

    fun register(displayName: String, email: String, password: String, confirmPassword: String) {
        clearErrors()

        var hasError = false

        if (!ValidationUtil.isValidDisplayName(displayName)) {
            _displayNameError.value = "Display name must be 2-30 characters"
            hasError = true
        }

        if (!ValidationUtil.isValidEmail(email)) {
            _emailError.value = "Please enter a valid email address"
            hasError = true
        }

        if (!ValidationUtil.isValidPassword(password)) {
            _passwordError.value = "Password must be at least 6 characters"
            hasError = true
        }

        if (password != confirmPassword) {
            _confirmPasswordError.value = "Passwords do not match"
            hasError = true
        }

        if (hasError) return

        _authState.value = AuthState.Loading

        viewModelScope.launch {
            when (val result = userRepository.register(email, password, displayName)) {
                is Resource.Success -> _authState.value = AuthState.Success(result.data)
                is Resource.Error -> _authState.value = AuthState.Error(result.message)
                is Resource.Loading -> _authState.value = AuthState.Loading
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _authState.value = AuthState.Loading

        viewModelScope.launch {
            when (val result = userRepository.signInWithGoogle(idToken)) {
                is Resource.Success -> _authState.value = AuthState.Success(result.data)
                is Resource.Error -> _authState.value = AuthState.Error(result.message)
                is Resource.Loading -> _authState.value = AuthState.Loading
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            _authState.value = AuthState.Idle
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
        clearErrors()
    }

    private fun clearErrors() {
        _emailError.value = null
        _passwordError.value = null
        _displayNameError.value = null
        _confirmPasswordError.value = null
    }

    class Factory(private val userRepository: UserRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                return AuthViewModel(userRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
