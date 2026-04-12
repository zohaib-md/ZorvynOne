package com.project.zorvynone.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserFinancialProfile(
    @PrimaryKey val id: Int = 0,
    val monthlySalary: Double = 0.0,
    val fixedExpenses: Double = 0.0, // New: Rent, Bills, etc.
    val goalName: String = "General Savings", // New: "iPhone 15 Pro"
    val targetAmount: Double = 0.0,
    val targetMonths: Int = 5
)