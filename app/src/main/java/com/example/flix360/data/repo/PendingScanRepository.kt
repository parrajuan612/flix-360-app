package com.example.flix360.data.repo

import android.content.Context
import androidx.work.*
import com.example.flix360.data.local.FlixDatabase
import com.example.flix360.data.local.entity.PendingScanEntity
import com.example.flix360.data.sync.SyncWorker
import java.util.concurrent.TimeUnit

class PendingScanRepository(context: Context) {
    private val appContext = context.applicationContext
    private val db = FlixDatabase.getInstance(appContext)

    suspend fun enqueue(productId: String, locationId: String, epcs: List<String>) {
        val json = epcs.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
        db.pendingScanDao().insert(
            PendingScanEntity(productId = productId, locationId = locationId, epcsJson = json)
        )
        enqueueSyncWork()
    }

    private fun enqueueSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val work = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork("sync_offline", ExistingWorkPolicy.KEEP, work)
    }

    suspend fun all() = db.pendingScanDao().getAll()
    suspend fun clearAll() = db.pendingScanDao().clearAll()
}
