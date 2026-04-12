package com.project.zorvynone.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 1")
    fun getTotalIncome(): Flow<Int?>

    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 0")
    fun getTotalExpenses(): Flow<Int?>

    // --- NEW: Savings Engine Queries ---
    @Query("SELECT * FROM user_profile WHERE id = 0")
    fun getUserProfile(): Flow<UserFinancialProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserFinancialProfile)

    @Query("SELECT * FROM savings_goals")
    fun getAllGoals(): Flow<List<SavingsGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoal)
    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 0 AND date >= :startTime")
    fun getExpensesSince(startTime: Long): Flow<Int?>

    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 1 AND date >= :startTime")
    fun getIncomeSince(startTime: Long): Flow<Int?>
}