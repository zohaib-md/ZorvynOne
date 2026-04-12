package com.project.zorvynone.model

import android.content.Context
import androidx.room.*

// We include all three entities: Transaction, SavingsGoal, and UserFinancialProfile
@Database(
    entities = [
        Transaction::class,
        SavingsGoal::class,
        UserFinancialProfile::class
    ],
    version = 4, // Bumped to 4 to accommodate the new user profile and goals
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zorvyn_database"
                )
                    // fallbackToDestructiveMigration allows us to change the database
                    // structure easily during development by clearing old data.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}