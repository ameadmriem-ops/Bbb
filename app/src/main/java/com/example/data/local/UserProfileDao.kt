package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profiles WHERE uid = :uid LIMIT 1")
    fun getUserProfile(uid: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE uid = :uid LIMIT 1")
    suspend fun getUserProfileSync(uid: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfileEntity)

    @Update
    suspend fun update(profile: UserProfileEntity)

    @Query("UPDATE user_profiles SET isPremium = :isPremium WHERE uid = :uid")
    suspend fun updatePremiumStatus(uid: String, isPremium: Boolean)

    @Query("SELECT COUNT(*) FROM user_profiles")
    suspend fun getTotalUserCount(): Int

    @Query("SELECT COUNT(*) FROM user_profiles WHERE isPremium = 1")
    suspend fun getPremiumUserCount(): Int
}
