package com.smartbudget.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.smartbudget.data.entity.Budget
import com.smartbudget.data.entity.Category
import com.smartbudget.data.entity.Expense
import com.smartbudget.ui.components.MonthSelector
import com.smartbudget.ui.util.formatCurrency
import com.smartbudget.viewmodel.BudgetViewModel
import com.smartbudget.viewmodel.ExpenseViewModel
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    expenseViewModel: ExpenseViewModel,
    budgetViewModel: BudgetViewModel
) {
    val context = LocalContext.current
    val categories by expenseViewModel.categories.collectAsState()
    val allExpenses by expenseViewModel.getAllExpensesForExport().collectAsState(initial = emptyList())
    val currentMonth by expenseViewModel.currentMonth.collectAsState()
    val budgets by budgetViewModel.budgets.collectAsState()

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editCategory by remember { mutableStateOf<Category?>(null) }
    var deleteCategory by remember { mutableStateOf<Category?>(null) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var importResult by remember { mutableStateOf<String?>(null) }

    // Sync month
    LaunchedEffect(currentMonth) {
        budgetViewModel.setMonth(currentMonth)
    }

    // CSV Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val result = importCsv(context, it, categories, expenseViewModel)
            importResult = result
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Section: Catégories ---
        item {
            Text(
                text = "Paramètres",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Catégories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showAddCategoryDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Ajouter une catégorie")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            if (category.isActive) {
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Text(
                                    text = "Inactive",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { editCategory = category },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Modifier", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { deleteCategory = category },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Supprimer",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Section: Budgets mensuels ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Budgets mensuels",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showAddBudgetDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Définir un budget")
                        }
                    }

                    MonthSelector(
                        currentMonth = currentMonth,
                        onPrevious = { expenseViewModel.previousMonth() },
                        onNext = { expenseViewModel.nextMonth() },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    if (budgets.isEmpty()) {
                        Text(
                            text = "Aucun budget défini pour ce mois",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        budgets.forEach { budget ->
                            val category = categories.find { it.id == budget.categoryId }
                            val spent by budgetViewModel.getSpentForCategory(budget.categoryId)
                                .collectAsState(initial = 0.0)
                            val progress = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat().coerceIn(0f, 1f) else 0f
                            val isOverBudget = spent > budget.limitAmount

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = category?.name ?: "Inconnue",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                        color = if (isOverBudget) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = "${formatCurrency(spent)} / ${formatCurrency(budget.limitAmount)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isOverBudget) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { budgetViewModel.deleteBudget(budget) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Supprimer",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Section: Devise ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Devise",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Devise actuelle : MAD (DH)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // --- Section: Export / Import ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ImportExport, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Export / Import CSV",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "${allExpenses.size} dépense(s) enregistrée(s)",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { exportCsv(context, allExpenses, categories) },
                            modifier = Modifier.weight(1f),
                            enabled = allExpenses.isNotEmpty()
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exporter")
                        }

                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Importer")
                        }
                    }

                    importResult?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (it.startsWith("Erreur")) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // --- Section: À propos ---
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "À propos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("SmartBudget v1.0", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Application de gestion de budget personnel offline-first",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    // Add category dialog
    if (showAddCategoryDialog) {
        CategoryDialog(
            title = "Nouvelle catégorie",
            initialName = "",
            onDismiss = { showAddCategoryDialog = false },
            onConfirm = { name ->
                expenseViewModel.addCategory(name)
                showAddCategoryDialog = false
            }
        )
    }

    // Edit category dialog
    editCategory?.let { category ->
        CategoryDialog(
            title = "Modifier la catégorie",
            initialName = category.name,
            onDismiss = { editCategory = null },
            onConfirm = { name ->
                expenseViewModel.updateCategory(category.copy(name = name))
                editCategory = null
            }
        )
    }

    // Delete category confirmation
    deleteCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { deleteCategory = null },
            title = { Text("Supprimer « ${category.name} » ?") },
            text = { Text("Si des dépenses utilisent cette catégorie, la suppression échouera.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        expenseViewModel.deleteCategory(category)
                        deleteCategory = null
                    }
                ) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCategory = null }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Add budget dialog
    if (showAddBudgetDialog) {
        AddBudgetDialog(
            categories = categories,
            existingBudgetCategoryIds = budgets.map { it.categoryId }.toSet(),
            onDismiss = { showAddBudgetDialog = false },
            onConfirm = { categoryId, amount ->
                budgetViewModel.setBudget(categoryId, amount)
                showAddBudgetDialog = false
            }
        )
    }
}

@Composable
private fun CategoryDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom de la catégorie") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Valider")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBudgetDialog(
    categories: List<Category>,
    existingBudgetCategoryIds: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Double) -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    val availableCategories = categories.filter { it.id !in existingBudgetCategoryIds }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Définir un budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = availableCategories.find { it.id == selectedCategoryId }?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Catégorie") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        availableCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Montant limite (DH)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (selectedCategoryId != null && amountValue != null && amountValue > 0) {
                        onConfirm(selectedCategoryId!!, amountValue)
                    }
                },
                enabled = selectedCategoryId != null && (amount.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Valider")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

private fun exportCsv(
    context: Context,
    expenses: List<Expense>,
    categories: List<Category>
) {
    try {
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val categoryMap = categories.associateBy { it.id }

        val csv = buildString {
            appendLine("Date,Montant,Catégorie,Description,Méthode de paiement,Récurrente")
            expenses.forEach { expense ->
                val date = Instant.ofEpochMilli(expense.date)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(dateFormatter)
                val category = categoryMap[expense.categoryId]?.name ?: "Inconnue"
                val desc = expense.description.replace("\"", "\"\"")
                val recurring = if (expense.isRecurring) "Oui" else "Non"
                appendLine("$date,${expense.amount},\"$category\",\"$desc\",\"${expense.paymentMethod}\",\"$recurring\"")
            }
        }

        val file = File(context.cacheDir, "smartbudget_export.csv")
        file.writeText(csv, Charsets.UTF_8)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "SmartBudget - Export")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Partager l'export"))
    } catch (e: Exception) {
        Toast.makeText(context, "Erreur lors de l'export: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun importCsv(
    context: Context,
    uri: Uri,
    categories: List<Category>,
    viewModel: ExpenseViewModel
): String {
    return try {
        val categoryMap = categories.associateBy { it.name.lowercase() }
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        var imported = 0
        var skipped = 0

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
            val header = reader.readLine() // Skip header

            reader.forEachLine { line ->
                try {
                    val parts = parseCsvLine(line)
                    if (parts.size >= 4) {
                        val date = LocalDate.parse(parts[0].trim(), dateFormatter)
                        val amount = parts[1].trim().toDouble()
                        val categoryName = parts[2].trim()
                        val description = parts[3].trim()
                        val paymentMethod = if (parts.size >= 5) parts[4].trim() else "Espèces"
                        val isRecurring = if (parts.size >= 6) parts[5].trim().equals("Oui", ignoreCase = true) else false

                        val category = categoryMap[categoryName.lowercase()]
                        if (category != null && amount > 0) {
                            val dateMillis = date.atStartOfDay(ZoneId.systemDefault())
                                .toInstant().toEpochMilli()
                            viewModel.addExpense(amount, description, dateMillis, category.id, paymentMethod, isRecurring)
                            imported++
                        } else {
                            skipped++
                        }
                    } else {
                        skipped++
                    }
                } catch (e: Exception) {
                    skipped++
                }
            }
        }

        "$imported dépense(s) importée(s)" + if (skipped > 0) ", $skipped ignorée(s)" else ""
    } catch (e: Exception) {
        "Erreur lors de l'import: ${e.message}"
    }
}

private fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    var current = StringBuilder()
    var inQuotes = false

    for (char in line) {
        when {
            char == '"' -> inQuotes = !inQuotes
            char == ',' && !inQuotes -> {
                result.add(current.toString())
                current = StringBuilder()
            }
            else -> current.append(char)
        }
    }
    result.add(current.toString())
    return result
}
