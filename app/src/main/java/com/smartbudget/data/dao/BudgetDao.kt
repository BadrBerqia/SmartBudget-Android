package com.smartbudget.data.dao

import androidx.room.*
import com.smartbudget.data.entity.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE yearMonth = :yearMonth")
    fun getBudgetsByMonth(yearMonth: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND yearMonth = :yearMonth")
    suspend fun getBudget(categoryId: Long, yearMonth: String): Budget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)
}
