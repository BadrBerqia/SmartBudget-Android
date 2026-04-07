package com.smartbudget.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartbudget.data.dao.BudgetDao
import com.smartbudget.data.dao.CategoryDao
import com.smartbudget.data.dao.ExpenseDao
import com.smartbudget.data.entity.Budget
import com.smartbudget.data.entity.Category
import com.smartbudget.data.entity.Expense
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

@Database(
    entities = [Expense::class, Category::class, Budget::class],
    version = 2,
    exportSchema = false
)
abstract class SmartBudgetDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: SmartBudgetDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'Espèces'")
                db.execSQL("ALTER TABLE expenses ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE expenses ADD COLUMN createdAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
                db.execSQL("ALTER TABLE expenses ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
                db.execSQL("ALTER TABLE categories ADD COLUMN color TEXT NOT NULL DEFAULT '#4CAF50'")
                db.execSQL("ALTER TABLE categories ADD COLUMN isActive INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getDatabase(context: Context): SmartBudgetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartBudgetDatabase::class.java,
                    "smartbudget_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.let { database ->
                                    populateDefaultCategories(database.categoryDao())
                                    populateTestExpenses(database.expenseDao(), database.categoryDao())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateDefaultCategories(categoryDao: CategoryDao) {
            val defaults = listOf(
                Category(name = "Alimentation", icon = "restaurant", color = "#4CAF50"),
                Category(name = "Transport", icon = "directions_car", color = "#2196F3"),
                Category(name = "Logement", icon = "home", color = "#FF9800"),
                Category(name = "Loisirs", icon = "sports_esports", color = "#E91E63"),
                Category(name = "Santé", icon = "local_hospital", color = "#9C27B0"),
                Category(name = "Shopping", icon = "shopping_bag", color = "#00BCD4"),
                Category(name = "Factures", icon = "receipt_long", color = "#FF5722"),
                Category(name = "Autre", icon = "more_horiz", color = "#607D8B")
            )
            defaults.forEach { categoryDao.insert(it) }
        }

        private suspend fun populateTestExpenses(expenseDao: ExpenseDao, categoryDao: CategoryDao) {
            val zone = ZoneId.systemDefault()

            fun dateMillis(year: Int, month: Int, day: Int): Long =
                LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()

            // Mars 2026 - 18 dépenses
            val marchExpenses = listOf(
                Expense(amount = 85.0, description = "Courses Marjane", date = dateMillis(2026, 3, 2), categoryId = 1, paymentMethod = "Carte"),
                Expense(amount = 45.50, description = "Restaurant famille", date = dateMillis(2026, 3, 3), categoryId = 1, paymentMethod = "Espèces"),
                Expense(amount = 25.0, description = "Fruits et légumes", date = dateMillis(2026, 3, 7), categoryId = 1, paymentMethod = "Espèces"),
                Expense(amount = 120.0, description = "Plein essence", date = dateMillis(2026, 3, 5), categoryId = 2, paymentMethod = "Carte"),
                Expense(amount = 30.0, description = "Ticket tramway", date = dateMillis(2026, 3, 10), categoryId = 2, paymentMethod = "Espèces"),
                Expense(amount = 15.0, description = "Taxi", date = dateMillis(2026, 3, 18), categoryId = 2, paymentMethod = "Espèces"),
                Expense(amount = 3500.0, description = "Loyer mars", date = dateMillis(2026, 3, 1), categoryId = 3, paymentMethod = "Virement", isRecurring = true),
                Expense(amount = 250.0, description = "Facture électricité", date = dateMillis(2026, 3, 12), categoryId = 7, paymentMethod = "Virement", isRecurring = true),
                Expense(amount = 99.0, description = "Abonnement internet", date = dateMillis(2026, 3, 8), categoryId = 7, paymentMethod = "Virement", isRecurring = true),
                Expense(amount = 60.0, description = "Cinéma + popcorn", date = dateMillis(2026, 3, 14), categoryId = 4, paymentMethod = "Carte"),
                Expense(amount = 200.0, description = "Sortie week-end", date = dateMillis(2026, 3, 22), categoryId = 4, paymentMethod = "Espèces"),
                Expense(amount = 150.0, description = "Consultation médecin", date = dateMillis(2026, 3, 6), categoryId = 5, paymentMethod = "Espèces"),
                Expense(amount = 85.0, description = "Pharmacie", date = dateMillis(2026, 3, 6), categoryId = 5, paymentMethod = "Carte"),
                Expense(amount = 350.0, description = "Chaussures Nike", date = dateMillis(2026, 3, 15), categoryId = 6, paymentMethod = "Carte"),
                Expense(amount = 120.0, description = "Vêtements Zara", date = dateMillis(2026, 3, 20), categoryId = 6, paymentMethod = "Carte"),
                Expense(amount = 40.0, description = "Boulangerie", date = dateMillis(2026, 3, 25), categoryId = 1, paymentMethod = "Espèces"),
                Expense(amount = 75.0, description = "Livres universitaires", date = dateMillis(2026, 3, 11), categoryId = 8, paymentMethod = "Carte"),
                Expense(amount = 50.0, description = "Coiffeur", date = dateMillis(2026, 3, 28), categoryId = 8, paymentMethod = "Espèces"),
            )

            // Avril 2026 - 17 dépenses
            val aprilExpenses = listOf(
                Expense(amount = 95.0, description = "Courses Carrefour", date = dateMillis(2026, 4, 1), categoryId = 1, paymentMethod = "Carte"),
                Expense(amount = 55.0, description = "Restaurant avec amis", date = dateMillis(2026, 4, 4), categoryId = 1, paymentMethod = "Espèces"),
                Expense(amount = 30.0, description = "Marché fruits", date = dateMillis(2026, 4, 8), categoryId = 1, paymentMethod = "Espèces"),
                Expense(amount = 110.0, description = "Plein essence", date = dateMillis(2026, 4, 3), categoryId = 2, paymentMethod = "Carte"),
                Expense(amount = 20.0, description = "Bus universitaire", date = dateMillis(2026, 4, 6), categoryId = 2, paymentMethod = "Espèces"),
                Expense(amount = 3500.0, description = "Loyer avril", date = dateMillis(2026, 4, 1), categoryId = 3, paymentMethod = "Virement", isRecurring = true),
                Expense(amount = 200.0, description = "Facture eau + élec", date = dateMillis(2026, 4, 10), categoryId = 7, paymentMethod = "Virement", isRecurring = true),
                Expense(amount = 99.0, description = "Abonnement internet", date = dateMillis(2026, 4, 8), categoryId = 7, paymentMethod = "Virement", isRecurring = true),
                Expense(amount = 80.0, description = "Bowling + dîner", date = dateMillis(2026, 4, 5), categoryId = 4, paymentMethod = "Carte"),
                Expense(amount = 150.0, description = "Parc aquatique", date = dateMillis(2026, 4, 12), categoryId = 4, paymentMethod = "Espèces"),
                Expense(amount = 100.0, description = "Dentiste", date = dateMillis(2026, 4, 7), categoryId = 5, paymentMethod = "Carte"),
                Expense(amount = 45.0, description = "Médicaments", date = dateMillis(2026, 4, 7), categoryId = 5, paymentMethod = "Espèces"),
                Expense(amount = 280.0, description = "T-shirts H&M", date = dateMillis(2026, 4, 9), categoryId = 6, paymentMethod = "Carte"),
                Expense(amount = 180.0, description = "Sac à dos", date = dateMillis(2026, 4, 14), categoryId = 6, paymentMethod = "Carte"),
                Expense(amount = 35.0, description = "Boulangerie pâtisserie", date = dateMillis(2026, 4, 6), categoryId = 1, paymentMethod = "Espèces"),
                Expense(amount = 60.0, description = "Fournitures scolaires", date = dateMillis(2026, 4, 2), categoryId = 8, paymentMethod = "Carte"),
                Expense(amount = 25.0, description = "Café et goûter", date = dateMillis(2026, 4, 11), categoryId = 1, paymentMethod = "Espèces"),
            )

            (marchExpenses + aprilExpenses).forEach { expenseDao.insert(it) }
        }
    }
}
