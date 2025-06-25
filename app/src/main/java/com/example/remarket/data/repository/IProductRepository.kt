package com.example.remarket.data.repository

import com.example.remarket.data.local.ProductEntity
import com.example.remarket.data.model.Product
import com.example.remarket.data.model.ProductDto
import com.example.remarket.data.network.ProductRequest
import com.example.remarket.util.Resource
import kotlinx.coroutines.flow.Flow

interface IProductRepository {
    suspend fun getAllProducts(): Flow<List<Product>>
    suspend fun createProduct(request: ProductRequest): Resource<Unit>
    suspend fun getProductById(productId: String): Flow<Resource<Product>>
    suspend fun toggleFavorite(productId: String): Flow<Boolean>
    suspend fun reportProduct(productId: String, reason: String): Flow<Boolean>
    suspend fun syncPendingProducts(): Unit
    suspend fun getPendingProducts(): List<ProductEntity>
    // NUEVO: Sincronizar todos los productos desde el servidor
    suspend fun syncAllProducts(): Unit
}
