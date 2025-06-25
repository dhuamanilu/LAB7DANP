package com.example.remarket.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.remarket.data.model.Product

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val sellerId: String,
    val brand: String,
    val model: String,
    val storage: String,
    val price: Double,
    val imei: String,
    val description: String,
    val images: List<String>,
    val boxImageUrl: String?,
    val invoiceUrl: String?,
    val status: String,
    val active: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val syncStatus: SyncStatus,         // NUEVO enum: PENDING/SYNCED/ERROR
    val lastModified: Long              // timestamp de la última edición local
)
// ProductEntity -> Product
fun ProductEntity.toDomain(): Product = Product(
    id = id,
    sellerId = sellerId,
    brand = brand,
    model = model,
    storage = storage,
    price = price,
    imei = imei,
    description = description,
    images = images,
    box = boxImageUrl ?: "",
    invoiceUri = invoiceUrl ?: "",
    status = status,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt
)
