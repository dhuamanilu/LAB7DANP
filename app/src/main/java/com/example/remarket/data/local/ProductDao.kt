package com.example.remarket.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products") fun getAll(): Flow<List<ProductEntity>>
    @Query("SELECT * FROM products WHERE syncStatus = :status")
    suspend fun getByStatus(status: SyncStatus): List<ProductEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(product: ProductEntity)
    @Update
    suspend fun update(product: ProductEntity)
    // NUEVOS MÉTODOS NECESARIOS:

    @Query("DELETE FROM products")
    suspend fun deleteAll()

    @Query("DELETE FROM products WHERE syncStatus = 'SYNCED'")
    suspend fun deleteAllSynced()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(products: List<ProductEntity>)

    @Query("SELECT * FROM products WHERE sellerId = :sellerId")
    suspend fun getProductsBySeller(sellerId: String): List<ProductEntity>
}