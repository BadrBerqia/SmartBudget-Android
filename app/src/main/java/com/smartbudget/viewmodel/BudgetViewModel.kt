package com.smartbudget.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartbudget.SmartBudgetApp
import com.smartbudget.data.entity.Budget
import com.smartbudget.data.repository.ExpenseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as SmartBudgetApp).database
    private val repository = ExpenseRepository(
        database.expenseDao(),
        database.categoryDao(),
        database.budgetDao()
    )

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    val budgets: StateFlow<List<Budget>> = _currentMonth.flatMapLatest { month ->
        repository.getBudgetsByMonth(month.toString())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setMonth(yearMonth: YearMonth) {
        _currentMonth.value = yearMonth
    }

    fun setBudget(categoryId: Long, amount: Double) {
        viewModelScope.launch {
            val budget = Budget(
                categoryId = categoryId,
                yearMonth = _currentMonth.value.toString(),
                limitAmount = amount
            )
            repository.upsertBudget(budget)
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    fun getSpentForCategory(categoryId: Long): Flow<Double> {
        return _currentMonth.flatMapLatest { month ->
            repository.getSpentByCategoryAndMonth(categoryId, month)
        }
    }
}
