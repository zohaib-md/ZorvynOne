package com.project.zorvynone.model

import android.content.Context
import androidx.room.*

@Database(
    entities = [
        Transaction::class,
        SavingsGoal::class,
        SavingsDeposit::class
    ],
    version = 5, // Bumped: replaced UserFinancialProfile with SavingsDeposit, revamped SavingsGoal
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