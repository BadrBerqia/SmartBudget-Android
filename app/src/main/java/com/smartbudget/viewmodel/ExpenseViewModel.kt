package com.smartbudget.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartbudget.SmartBudgetApp
import com.smartbudget.data.entity.Category
import com.smartbudget.data.entity.CategoryWithAmount
import com.smartbudget.data.entity.Expense
import com.smartbudget.data.repository.ExpenseRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as SmartBudgetApp).database
    private val repository = ExpenseRepository(
        database.expenseDao(),
        database.categoryDao(),
        database.budgetDao()
    )

    // Current selected month
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    // Categories
    val categories: StateFlow<List<Category>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Expenses for current month
    val expenses: StateFlow<List<Expense>> = _currentMonth.flatMapLatest { month ->
        repository.getExpensesByMonth(month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total for current month
    val monthlyTotal: StateFlow<Double> = _currentMonth.flatMapLatest { month ->
        repository.getTotalByMonth(month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Totals by category for current month
    val categoryTotals: StateFlow<List<CategoryWithAmount>> = _currentMonth.flatMapLatest { month ->
        repository.getTotalsByCategory(month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI events
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    fun previousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun setMonth(yearMonth: YearMonth) {
        _currentMonth.value = yearMonth
    }

    fun addExpense(
        amount: Double,
        description: String,
        date: Long,
        categoryId: Long,
        paymentMethod: String = "Espèces",
        isRecurring: Boolean = false
    ) {
        viewModelScope.launch {
            val expense = Expense(
                amount = amount,
                description = description,
                date = date,
                categoryId = categoryId,
                paymentMethod = paymentMethod,
                isRecurring = isRecurring
            )
            repository.insertExpense(expense).fold(
                onSuccess = { _uiEvent.emit(UiEvent.Success("Dépense ajoutée")) },
                onFailure = { _uiEvent.emit(UiEvent.Error(it.message ?: "Erreur")) }
            )
        }
    }

    fun updateExpense(
        id: Long,
        amount: Double,
        description: String,
        date: Long,
        categoryId: Long,
        paymentMethod: String = "Espèces",
        isRecurring: Boolean = false
    ) {
        viewModelScope.launch {
            val existing = repository.getExpenseById(id)
            val expense = Expense(
                id = id,
                amount = amount,
                description = description,
                date = date,
                categoryId = categoryId,
                paymentMethod = paymentMethod,
                isRecurring = isRecurring,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.updateExpense(expense).fold(
                onSuccess = { _uiEvent.emit(UiEvent.Success("Dépense modifiée")) },
                onFailure = { _uiEvent.emit(UiEvent.Error(it.message ?: "Erreur")) }
            )
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _uiEvent.emit(UiEvent.Success("Dépense supprimée"))
        }
    }

    suspend fun getExpenseById(id: Long): Expense? = repository.getExpenseById(id)

    fun addCategory(name: String, icon: String = "category") {
        viewModelScope.launch {
            repository.insertCategory(Category(name = name, icon = icon)).fold(
                onSuccess = { _uiEvent.emit(UiEvent.Success("Catégorie ajoutée")) },
                onFailure = { _uiEvent.emit(UiEvent.Error(it.message ?: "Erreur")) }
            )
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category).fold(
                onSuccess = { _uiEvent.emit(UiEvent.Success("Catégorie modifiée")) },
                onFailure = { _uiEvent.emit(UiEvent.Error(it.message ?: "Erreur")) }
            )
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                repository.deleteCategory(category)
                _uiEvent.emit(UiEvent.Success("Catégorie supprimée"))
            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.Error("Impossible de supprimer : des dépenses utilisent cette catégorie"))
            }
        }
    }

    fun getAllExpensesForExport(): Flow<List<Expense>> = repository.getAllExpenses()

    sealed class UiEvent {
        data class Success(val message: String) : UiEvent()
        data class Error(val message: String) : UiEvent()
    }
}
