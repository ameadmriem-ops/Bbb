package com.example.model

data class UserProfile(
    val uid: String,
    val email: String,
    val displayName: String,
    val isPremium: Boolean = false,
    val isAdmin: Boolean = false,
    val joinedAt: Long = System.currentTimeMillis(),
    val dailyMessagesUsed: Int = 0,
    val dailyImagesUsed: Int = 0,
    val lastActiveDate: String = ""
)
