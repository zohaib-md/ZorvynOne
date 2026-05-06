package com.project.zorvynone.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,                          // "iPhone 16 Pro"
    val targetAmount: Double,                   // ₹1,29,900
    val savedAmount: Double = 0.0,              // Running total of actual deposits
    val deadline: Long,                         // Target date (unix millis)
    val iconEmoji: String = "🎯",               // Visual identifier for the vault
    val roundUpRule: Int = 10,                  // Round-up to nearest ₹10/₹50/₹100; 0 = disabled
    val isDefaultRoundUpVault: Boolean = false, // User picks which vault receives round-ups
    val createdAt: Long = System.currentTimeMillis()
)