package com.smartbudget.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.smartbudget.data.entity.Category
import com.smartbudget.data.entity.Expense
import com.smartbudget.viewmodel.ExpenseViewModel
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ExportScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val allExpenses by viewModel.getAllExpensesForExport().collectAsState(initial = emptyList())
    val categories by viewModel.categories.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Exporter les données",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Export CSV",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Exporte toutes vos dépenses au format CSV, compatible avec Excel et Google Sheets.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${allExpenses.size} dépense(s) à exporter",
                    style = MaterialTheme.typography.bodySmall
                )

                Button(
                    onClick = {
                        exportCsv(context, allExpenses, categories)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = allExpenses.isNotEmpty()
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exporter et partager")
                }
            }
        }

        if (allExpenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucune dépense à exporter",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
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
            appendLine("Date,Montant,Catégorie,Description")
            expenses.forEach { expense ->
                val date = Instant.ofEpochMilli(expense.date)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(dateFormatter)
                val category = categoryMap[expense.categoryId]?.name ?: "Inconnue"
                val desc = expense.description.replace("\"", "\"\"")
                appendLine("$date,${expense.amount},\"$category\",\"$desc\"")
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
