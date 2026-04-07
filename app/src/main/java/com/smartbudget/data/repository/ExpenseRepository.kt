package com.smartbudget.data.repository

import com.smartbudget.data.dao.BudgetDao
import com.smartbudget.data.dao.CategoryDao
import com.smartbudget.data.dao.ExpenseDao
import com.smartbudget.data.entity.Budget
import com.smartbudget.data.entity.Category
import com.smartbudget.data.entity.CategoryWithAmount
import com.smartbudget.data.entity.Expense
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao
) {
    // --- Categories ---
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    suspend fun getCategoryById(id: Long): Category? = categoryDao.getCategoryById(id)

    suspend fun insertCategory(category: Category): Result<Long> {
        if (category.name.isBlank()) return Result.failure(IllegalArgumentException("Le nom est obligatoire"))
        if (categoryDao.existsByName(category.name, category.id)) {
            return Result.failure(IllegalArgumentException("Une catégorie avec ce nom existe déjà"))
        }
        return Result.success(categoryDao.insert(category))
    }

    suspend fun updateCategory(category: Category): Result<Unit> {
        if (category.name.isBlank()) return Result.failure(IllegalArgumentException("Le nom est obligatoire"))
        if (categoryDao.existsByName(category.name, category.id)) {
            return Result.failure(IllegalArgumentException("Une catégorie avec ce nom existe déjà"))
        }
        categoryDao.update(category)
        return Result.success(Unit)
    }

    suspend fun deleteCategory(category: Category) = categoryDao.delete(category)

    // --- Expenses ---
    fun getExpensesByMonth(yearMonth: YearMonth): Flow<List<Expense>> {
        val (start, end) = monthRange(yearMonth)
        return expenseDao.getExpensesByMonth(start, end)
    }

    fun getTotalByMonth(yearMonth: YearMonth): Flow<Double> {
        val (start, end) = monthRange(yearMonth)
        return expenseDao.getTotalByMonth(start, end)
    }

    fun getTotalsByCategory(yearMonth: YearMonth): Flow<List<CategoryWithAmount>> {
        val (start, end) = monthRange(yearMonth)
        return expenseDao.getTotalsByCategory(start, end)
    }

    suspend fun getExpenseById(id: Long): Expense? = expenseDao.getExpenseById(id)

    suspend fun insertExpense(expense: Expense): Result<Long> {
        val validation = validateExpense(expense)
        if (validation != null) return Result.failure(IllegalArgumentException(validation))
        return Result.success(expenseDao.insert(expense))
    }

    suspend fun updateExpense(expense: Expense): Result<Unit> {
        val validation = validateExpense(expense)
        if (validation != null) return Result.failure(IllegalArgumentException(validation))
        expenseDao.update(expense)
        return Result.success(Unit)
    }

    suspend fun deleteExpense(expense: Expense) = expenseDao.delete(expense)

    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()

    // --- Budgets ---
    fun getBudgetsByMonth(yearMonth: String): Flow<List<Budget>> =
        budgetDao.getBudgetsByMonth(yearMonth)

    suspend fun upsertBudget(budget: Budget) = budgetDao.upsert(budget)

    suspend fun deleteBudget(budget: Budget) = budgetDao.delete(budget)

    fun getSpentByCategoryAndMonth(categoryId: Long, yearMonth: YearMonth): Flow<Double> {
        val (start, end) = monthRange(yearMonth)
        return expenseDao.getTotalByCategoryAndMonth(categoryId, start, end)
    }

    // --- Helpers ---
    private fun validateExpense(expense: Expense): String? {
        if (expense.amount <= 0) return "Le montant doit être strictement positif"
        if (expense.date == 0L) return "La date est obligatoire"
        if (expense.categoryId == 0L) return "La catégorie est obligatoire"
        return null
    }

    private fun monthRange(yearMonth: YearMonth): Pair<Long, Long> {
        val start = yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = yearMonth.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return start to end
    }
}
