package com.project.zorvynone.model

import android.content.Context
import androidx.room.*

@Database(
    entities = [
        Transaction::class,
        SavingsGoal::class,
        SavingsDeposit::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Returns a database instance scoped to the given userId.
         * Each user gets their own SQLite file: "zorvyn_db_{userId}".
         * This ensures complete data isolation between accounts on the same device.
         */
        fun getDatabase(context: Context, userId: String): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zorvyn_db_$userId"   // ← Per-user database file
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Call this on sign-out so the next login creates a fresh DB
         * connection scoped to the new user's UID.
         */
        fun clearInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }
    }
}