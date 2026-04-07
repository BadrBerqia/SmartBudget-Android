package com.smartbudget.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Dépenses", Icons.Default.Home)
    data object Summary : Screen("summary", "Statistiques", Icons.Default.Analytics)
    data object Settings : Screen("settings", "Paramètres", Icons.Default.Settings)
    data object AddExpense : Screen("add_expense", "Ajouter", Icons.Default.Home)
    data object EditExpense : Screen("edit_expense/{expenseId}", "Modifier", Icons.Default.Home) {
        fun createRoute(expenseId: Long) = "edit_expense/$expenseId"
    }
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Summary,
    Screen.Settings
)
