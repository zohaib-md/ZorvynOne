package com.project.zorvynone.model

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    // --- Core Transaction Queries ---
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

    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 0 AND date >= :startTime")
    fun getExpensesSince(startTime: Long): Flow<Int?>

    @Query("SELECT SUM(amount) FROM transactions WHERE isIncome = 1 AND date >= :startTime")
    fun getIncomeSince(startTime: Long): Flow<Int?>

    // --- Smart Vaults Queries ---
    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    fun getAllGoals(): Flow<List<SavingsGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoal)

    @Delete
    suspend fun deleteGoal(goal: SavingsGoal)

    @Query("UPDATE savings_goals SET savedAmount = savedAmount + :amount WHERE id = :goalId")
    suspend fun addToVault(goalId: Int, amount: Double)

    @Query("UPDATE savings_goals SET isDefaultRoundUpVault = 0")
    suspend fun clearDefaultRoundUpVault()

    @Query("UPDATE savings_goals SET isDefaultRoundUpVault = 1 WHERE id = :goalId")
    suspend fun setDefaultRoundUpVault(goalId: Int)

    // --- Savings Deposits (Audit Trail) ---
    @Insert
    suspend fun insertDeposit(deposit: SavingsDeposit)

    @Query("SELECT * FROM savings_deposits WHERE goalId = :goalId ORDER BY timestamp DESC")
    fun getDepositsForGoal(goalId: Int): Flow<List<SavingsDeposit>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM savings_deposits WHERE source = 'round_up'")
    fun getTotalRoundUpSavings(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM savings_deposits")
    fun getTotalAllSavings(): Flow<Double>
}