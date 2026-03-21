package com.example.expenser

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    // --- Expense (ചെലവ്) സംബന്ധമായ കാര്യങ്ങൾ ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    // എല്ലാ ചെലവുകളും പുതിയത് ആദ്യം എന്ന ക്രമത്തിൽ ലഭിക്കാൻ
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    // ഒരു പ്രത്യേക പ്രൊഫൈലിന്റെ മാത്രം ചെലവുകൾ ലഭിക്കാൻ
    @Query("SELECT * FROM expenses WHERE profile_id = :profileId ORDER BY date DESC")
    fun getExpensesByProfile(profileId: Long): Flow<List<Expense>>

    // ഒരു പ്രത്യേക കാറ്റഗറിയിലുള്ള ആകെ തുക കണക്കാക്കാൻ
    @Query("SELECT COALESCE(SUM(amount_cents), 0) FROM expenses WHERE category = :categoryName")
    suspend fun getTotalByCategory(categoryName: String): Long

    @Query("SELECT SUM(amount_cents) FROM expenses WHERE strftime('%m', date) = :month AND strftime('%Y', date) = :year")
    suspend fun getMonthlyTotal(month: String, year: String): Long?

    // --- Profile (പ്രൊഫൈൽ) സംബന്ധമായ കാര്യങ്ങൾ ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProfile(profile: Profile): Long

    @Query("SELECT * FROM profiles")
    fun getAllProfiles(): Flow<List<Profile>>

    @Delete
    suspend fun deleteProfile(profile: Profile)

    @Update
    suspend fun updateProfile(profile: Profile)
}