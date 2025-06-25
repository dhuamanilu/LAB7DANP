package com.example.remarket.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.remarket.data.repository.IProductRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DataSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: IProductRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            repository.syncPendingProducts()
            // 2. NUEVO: Sincronizar todos los productos del servidor a local
            repository.syncAllProducts() // Necesitas agregar este método
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
