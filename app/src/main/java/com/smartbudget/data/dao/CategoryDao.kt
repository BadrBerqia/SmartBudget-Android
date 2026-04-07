package com.smartbudget.data.dao

import androidx.room.*
import com.smartbudget.data.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE name = :name AND id != :excludeId)")
    suspend fun existsByName(name: String, excludeId: Long = 0): Boolean

    @Insert
    suspend fun insert(category: Category): Long

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)
}
