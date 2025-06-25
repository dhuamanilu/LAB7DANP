package com.example.remarket

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import com.example.remarket.data.sync.DataSyncWorker

@HiltAndroidApp
class ReMarketApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // OPCIÓN 1: Sincronización inicial (tu código actual)
        scheduleInitialSync()

        // OPCIÓN 2: También programa sincronización periódica
        schedulePeriodicSync()
    }
    private fun scheduleInitialSync() {
        // Sincronización inmediata cuando hay conexión
        val initialRequest = OneTimeWorkRequestBuilder<DataSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "initial_sync",
            ExistingWorkPolicy.REPLACE, // Cambiar a REPLACE para forzar nueva sync
            initialRequest
        )
    }
    private fun schedulePeriodicSync() {
        // Sincronización cada 15 minutos cuando hay WiFi
        val periodicRequest = PeriodicWorkRequestBuilder<DataSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.UNMETERED) // Solo WiFi
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "periodic_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
