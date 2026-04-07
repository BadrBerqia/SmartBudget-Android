package com.smartbudget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.smartbudget.ui.navigation.Screen
import com.smartbudget.ui.navigation.bottomNavItems
import com.smartbudget.ui.screens.*
import com.smartbudget.ui.theme.SmartBudgetTheme
import com.smartbudget.viewmodel.BudgetViewModel
import com.smartbudget.viewmodel.ExpenseViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartBudgetTheme {
                SmartBudgetMainScreen()
            }
        }
    }
}

@Composable
fun SmartBudgetMainScreen() {
    val navController = rememberNavController()
    val expenseViewModel: ExpenseViewModel = viewModel()
    val budgetViewModel: BudgetViewModel = viewModel()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = expenseViewModel,
                    onAddExpense = { navController.navigate(Screen.AddExpense.route) },
                    onEditExpense = { id -> navController.navigate(Screen.EditExpense.createRoute(id)) }
                )
            }
            composable(Screen.Summary.route) {
                SummaryScreen(viewModel = expenseViewModel)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    expenseViewModel = expenseViewModel,
                    budgetViewModel = budgetViewModel
                )
            }
            composable(Screen.AddExpense.route) {
                AddEditExpenseScreen(
                    viewModel = expenseViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.EditExpense.route,
                arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
            ) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getLong("expenseId")
                AddEditExpenseScreen(
                    viewModel = expenseViewModel,
                    expenseId = expenseId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
