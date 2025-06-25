package com.example.remarket.data.local

import com.example.remarket.data.local.Converters

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ProductEntity::class    // << aquí
    ],
    version = 1,
    exportSchema = false
)

@TypeConverters(Converters::class)  // solo si necesitas convertir List<String> o SyncStatus
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
}
