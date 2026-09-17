package com.example.flix360.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker
import com.example.flix360.core.RetrofitClient
import com.example.flix360.core.SessionManager
import com.example.flix360.data.local.FlixDatabase
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    private val db by lazy { FlixDatabase.getInstance(applicationContext) }
    private val pendingDao by lazy { db.pendingScanDao() }
    private val gson = Gson()

    override suspend fun doWork(): ListenableWorker.Result = withContext(Dispatchers.IO) {
        // Adjuntar sesión para Retrofit
        RetrofitClient.attachSessionManager(SessionManager(applicationContext))
        val api = RetrofitClient.api

        val pendings = try { pendingDao.getAll() } catch (e: Exception) { emptyList() }
        if (pendings.isEmpty()) return@withContext ListenableWorker.Result.success()

        var hadNetworkFailure = false

        for (item in pendings) {
            var duplicateDetected = false
            val epcs: List<String> = try {
                gson.fromJson(item.epcsJson, Array<String>::class.java)?.toList() ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            try {
                for (epc in epcs) {
                    val tagResp = api.createRfidTag(com.example.flix360.data.remote.dto.RfidTagRequest(epc))
                    if (!tagResp.isSuccessful) {
                        if (isDuplicateError(tagResp)) {
                            duplicateDetected = true
                            // No lanzamos, continuamos con otras etiquetas para intentar crear las que faltan
                        } else {
                            // Otro error HTTP: tratamos como transitorio para reintentar
                            hadNetworkFailure = true
                            break
                        }
                    } else {
                        val rfidId = tagResp.body()!!.id
                        val assetResp = api.createInventoryAsset(
                            com.example.flix360.data.remote.dto.AssetRequest(
                                productId = item.productId,
                                locationId = item.locationId,
                                rfidTagId = rfidId,
                                quantity = 1.0,
                                inventoryMode = "unit"
                            )
                        )
                        if (!assetResp.isSuccessful) {
                            if (isDuplicateError(assetResp)) {
                                duplicateDetected = true
                            } else {
                                hadNetworkFailure = true
                                break
                            }
                        }
                    }
                }
            } catch (io: IOException) {
                // Conectividad: reintentar más tarde
                hadNetworkFailure = true
            } catch (_: Exception) {
                // Cualquier otra excepción la tratamos como transitoria por ahora
                hadNetworkFailure = true
            }

            // Si hubo duplicados, descartamos el registro y notificamos al usuario
            if (duplicateDetected) {
                runCatching { pendingDao.delete(item.id) }
                notifyDuplicatesDiscarded()
            } else if (!hadNetworkFailure) {
                // Si todo fue OK para este item (sin fallas), lo eliminamos
                runCatching { pendingDao.delete(item.id) }
            } else {
                // Mantener en cola si tuvimos fallas de red/servidor transitorias
            }
        }

        return@withContext if (hadNetworkFailure) ListenableWorker.Result.retry() else ListenableWorker.Result.success()
    }

    private fun isDuplicateError(resp: Response<*>): Boolean {
        val code = resp.code()
        if (code in 400..599) {
            val body = try { resp.errorBody()?.string().orEmpty() } catch (_: Exception) { "" }
            return body.contains("23505", ignoreCase = true) || body.contains("duplicate key", ignoreCase = true)
        }
        return false
    }

    private fun notifyDuplicatesDiscarded() {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "sync_offline"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "Sincronización Offline", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(ch)
        }
        val notif = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Sincronización Offline")
            .setContentText("Algunas etiquetas escaneadas sin conexión ya estaban asignadas y fueron descartadas")
            .setAutoCancel(true)
            .build()
        nm.notify(1001, notif)
    }
}
