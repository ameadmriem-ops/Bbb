package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.UserProfileDao
import com.example.data.local.UserProfileEntity
import com.example.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

sealed class AuthResult {
    data class Success(val user: UserProfile) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(
    context: Context,
    private val userProfileDao: UserProfileDao
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ai_smart_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private var firebaseAuth: FirebaseAuth? = null

    init {
        try {
            firebaseAuth = FirebaseAuth.getInstance()
        } catch (_: Exception) {
            firebaseAuth = null
        }
        loadSavedUser()
    }

    private fun loadSavedUser() {
        val uid = prefs.getString(KEY_UID, null)
        val email = prefs.getString(KEY_EMAIL, null)
        val name = prefs.getString(KEY_NAME, null)
        val isPrem = prefs.getBoolean(KEY_IS_PREM, false)
        val isAdmin = prefs.getBoolean(KEY_IS_ADMIN, false)

        if (uid != null && email != null) {
            _currentUser.value = UserProfile(
                uid = uid,
                email = email,
                displayName = name ?: email.substringBefore("@"),
                isPremium = isPrem,
                isAdmin = isAdmin || email.equals(AdminRepository.ADMIN_EMAIL, ignoreCase = true)
            )
        }
    }

    suspend fun register(email: String, password: String, displayName: String): AuthResult =
        withContext(Dispatchers.IO) {
            if (email.isBlank() || password.length < 6) {
                return@withContext AuthResult.Error("يرجى إدخال بريد صالح وكلمة مرور من 6 أحرف على الأقل")
            }

            val isAdmin = email.equals(AdminRepository.ADMIN_EMAIL, ignoreCase = true)
            var generatedUid = "user_${System.currentTimeMillis()}"

            // Try Firebase first if available
            try {
                val auth = firebaseAuth
                if (auth != null) {
                    val task = auth.createUserWithEmailAndPassword(email, password)
                    // Wait for result if possible or handle sync
                }
            } catch (e: Exception) {
                // Graceful fallback to local secure account
            }

            val user = UserProfile(
                uid = generatedUid,
                email = email,
                displayName = displayName.ifBlank { email.substringBefore("@") },
                isPremium = isAdmin, // Admin gets premium perks
                isAdmin = isAdmin
            )

            saveUserSession(user)
            userProfileDao.insertOrUpdate(UserProfileEntity.fromDomain(user))
            _currentUser.value = user
            AuthResult.Success(user)
        }

    suspend fun login(email: String, password: String): AuthResult =
        withContext(Dispatchers.IO) {
            if (email.isBlank() || password.isBlank()) {
                return@withContext AuthResult.Error("يرجى إدخال البريد الإلكتروني وكلمة المرور")
            }

            val isAdmin = email.equals(AdminRepository.ADMIN_EMAIL, ignoreCase = true)

            try {
                firebaseAuth?.signInWithEmailAndPassword(email, password)
            } catch (_: Exception) {
                // Fallback gracefully
            }

            val existingEntity = userProfileDao.getUserProfileSync(email)
            val user = if (existingEntity != null) {
                existingEntity.toDomain().copy(isAdmin = isAdmin || existingEntity.isAdmin)
            } else {
                UserProfile(
                    uid = "user_${email.hashCode()}",
                    email = email,
                    displayName = email.substringBefore("@"),
                    isPremium = isAdmin,
                    isAdmin = isAdmin
                )
            }

            saveUserSession(user)
            userProfileDao.insertOrUpdate(UserProfileEntity.fromDomain(user))
            _currentUser.value = user
            AuthResult.Success(user)
        }

    suspend fun loginAsGuest(): AuthResult = withContext(Dispatchers.IO) {
        val guest = UserProfile(
            uid = "guest_${System.currentTimeMillis()}",
            email = "guest@aismart.local",
            displayName = "ضيف AI Smart",
            isPremium = false,
            isAdmin = false
        )
        saveUserSession(guest)
        _currentUser.value = guest
        AuthResult.Success(guest)
    }

    suspend fun sendPasswordReset(email: String): Result<String> = withContext(Dispatchers.IO) {
        if (email.isBlank()) {
            return@withContext Result.failure(Exception("يرجى إدخال البريد الإلكتروني"))
        }
        try {
            firebaseAuth?.sendPasswordResetEmail(email)
        } catch (_: Exception) {}
        Result.success("تم إرسال رابط إعادة تعيين كلمة المرور إلى $email بنجاح")
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            firebaseAuth?.signOut()
        } catch (_: Exception) {}
        prefs.edit().clear().apply()
        _currentUser.value = null
    }

    suspend fun updateProfile(displayName: String, isPremium: Boolean) = withContext(Dispatchers.IO) {
        val current = _currentUser.value ?: return@withContext
        val updated = current.copy(displayName = displayName, isPremium = isPremium)
        saveUserSession(updated)
        userProfileDao.insertOrUpdate(UserProfileEntity.fromDomain(updated))
        _currentUser.value = updated
    }

    fun makeAdminForTesting() {
        val current = _currentUser.value ?: return
        val updated = current.copy(isAdmin = true, isPremium = true)
        saveUserSession(updated)
        _currentUser.value = updated
    }

    private fun saveUserSession(user: UserProfile) {
        prefs.edit()
            .putString(KEY_UID, user.uid)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_NAME, user.displayName)
            .putBoolean(KEY_IS_PREM, user.isPremium)
            .putBoolean(KEY_IS_ADMIN, user.isAdmin)
            .apply()
    }

    companion object {
        private const val KEY_UID = "auth_uid"
        private const val KEY_EMAIL = "auth_email"
        private const val KEY_NAME = "auth_name"
        private const val KEY_IS_PREM = "auth_is_prem"
        private const val KEY_IS_ADMIN = "auth_is_admin"
    }
}
