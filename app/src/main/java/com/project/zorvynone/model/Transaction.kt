package com.project.zorvynone.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val subtitle: String,
    val amount: Int,
    val isIncome: Boolean,
    val category: String,  // NEW
    val date: Long,        // NEW: Unix timestamp for filtering later
    val note: String?,     // NEW: Optional notes
    val iconType: IconType
)

// Matches the exact icons needed for your Figma design
enum class IconType { WALLET, FOOD, CAFE, SHOPPING, HOUSING, TRANSPORT, SALARY, DEFAULT }