package com.project.zorvynone.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks every individual deposit into a savings vault.
 * Provides a full audit trail: manual deposits, round-ups, and sweeps.
 */
@Entity(tableName = "savings_deposits")
data class SavingsDeposit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goalId: Int,                            // FK → SavingsGoal.id
    val amount: Double,                         // ₹500
    val source: String,                         // "manual", "round_up", "sweep"
    val timestamp: Long = System.currentTimeMillis()
)
