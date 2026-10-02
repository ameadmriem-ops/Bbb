package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.UserProfile

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey
    val uid: String,
    val email: String,
    val displayName: String,
    val isPremium: Boolean = false,
    val isAdmin: Boolean = false,
    val joinedAt: Long = System.currentTimeMillis(),
    val dailyMessagesUsed: Int = 0,
    val dailyImagesUsed: Int = 0,
    val lastActiveDate: String = ""
) {
    fun toDomain(): UserProfile = UserProfile(
        uid = uid,
        email = email,
        displayName = displayName,
        isPremium = isPremium,
        isAdmin = isAdmin,
        joinedAt = joinedAt,
        dailyMessagesUsed = dailyMessagesUsed,
        dailyImagesUsed = dailyImagesUsed,
        lastActiveDate = lastActiveDate
    )

    companion object {
        fun fromDomain(u: UserProfile): UserProfileEntity = UserProfileEntity(
            uid = u.uid,
            email = u.email,
            displayName = u.displayName,
            isPremium = u.isPremium,
            isAdmin = u.isAdmin,
            joinedAt = u.joinedAt,
            dailyMessagesUsed = u.dailyMessagesUsed,
            dailyImagesUsed = u.dailyImagesUsed,
            lastActiveDate = u.lastActiveDate
        )
    }
}
