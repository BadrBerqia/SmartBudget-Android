package com.smartbudget.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartbudget.ui.components.MonthSelector
import com.smartbudget.ui.theme.CategoryColors
import com.smartbudget.ui.util.formatCurrency
import com.smartbudget.viewmodel.ExpenseViewModel

@Composable
fun SummaryScreen(viewModel: ExpenseViewModel) {
    val currentMonth by viewModel.currentMonth.collectAsState()
    val monthlyTotal by viewModel.monthlyTotal.collectAsState()
    val categoryTotals by viewModel.categoryTotals.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Month selector
        MonthSelector(
            currentMonth = currentMonth,
            onPrevious = { viewModel.previousMonth() },
            onNext = { viewModel.nextMonth() },
            modifier = Modifier.padding(8.dp)
        )

        // Total
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total du mois",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = formatCurrency(monthlyTotal),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (categoryTotals.isNotEmpty()) {
            // Pie chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    var startAngle = -90f
                    categoryTotals.forEachIndexed { index, cat ->
                        val sweep = (cat.totalAmount / monthlyTotal * 360).toFloat()
                        val color = CategoryColors[index % CategoryColors.size]
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = true,
                            topLeft = Offset.Zero,
                            size = Size(size.width, size.height)
                        )
                        startAngle += sweep
                    }
                }
            }

            // Category breakdown
            Text(
                text = "Répartition par catégorie",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                itemsIndexed(categoryTotals) { index, catAmount ->
                    val percentage = if (monthlyTotal > 0) {
                        (catAmount.totalAmount / monthlyTotal * 100)
                    } else 0.0
                    val color = CategoryColors[index % CategoryColors.size]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Canvas(modifier = Modifier.size(12.dp)) {
                                drawCircle(color = color)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = catAmount.categoryName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                LinearProgressIndicator(
                                    progress = { (percentage / 100).toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    color = color,
                                    trackColor = color.copy(alpha = 0.2f)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatCurrency(catAmount.totalAmount),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format("%.1f%%", percentage),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucune dépense ce mois-ci",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
