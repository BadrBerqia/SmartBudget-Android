package com.smartbudget.data.dao

import androidx.room.*
import com.smartbudget.data.entity.CategoryWithAmount
import com.smartbudget.data.entity.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("""
        SELECT * FROM expenses
        WHERE date >= :startOfMonth AND date < :endOfMonth
        ORDER BY date DESC
    """)
    fun getExpensesByMonth(startOfMonth: Long, endOfMonth: Long): Flow<List<Expense>>

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM expenses
        WHERE date >= :startOfMonth AND date < :endOfMonth
    """)
    fun getTotalByMonth(startOfMonth: Long, endOfMonth: Long): Flow<Double>

    @Query("""
        SELECT e.categoryId as categoryId, c.name as categoryName, COALESCE(SUM(e.amount), 0) as totalAmount
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        WHERE e.date >= :startOfMonth AND e.date < :endOfMonth
        GROUP BY e.categoryId
        ORDER BY totalAmount DESC
    """)
    fun getTotalsByCategory(startOfMonth: Long, endOfMonth: Long): Flow<List<CategoryWithAmount>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM expenses
        WHERE categoryId = :categoryId AND date >= :startOfMonth AND date < :endOfMonth
    """)
    fun getTotalByCategoryAndMonth(categoryId: Long, startOfMonth: Long, endOfMonth: Long): Flow<Double>

    @Insert
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>
}
