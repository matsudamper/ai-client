package net.matsudamper.gptclient.localmodel

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.matsudamper.gptclient.EXTRA_OPEN_SETTINGS
import net.matsudamper.gptclient.LOCAL_MODEL_DOWNLOAD_NOTIFICATION_CHANNEL_ID

class LocalModelDownloadWorker(
    context: android.content.Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val modelId = inputData.getString(KEY_MODEL_ID)
            ?.let(::LocalModelId)
            ?: return Result.failure()
        val model = AndroidLocalModels.find(modelId)
            ?: return Result.failure()

        return runCatching {
            val destinationFile = LocalModelRepositoryImpl.getModelFile(applicationContext, modelId)
            val tempFile = File(destinationFile.parentFile, "${destinationFile.name}.download")
            destinationFile.parentFile?.mkdirs()
            tempFile.delete()

            if (destinationFile.exists()) {
                Result.success()
            } else {
                downloadToFile(
                    model = model,
                    tempFile = tempFile,
                    destinationFile = destinationFile,
                )
                showCompletedNotification(
                    title = "モデルのダウンロード完了",
                    message = "${model.displayName} を利用できるようになりました",
                    notificationId = modelId.value.hashCode(),
                )
                Result.success()
            }
        }.getOrElse { throwable ->
            LocalModelRepositoryImpl.getTempModelFile(applicationContext, modelId).delete()
            showCompletedNotification(
                title = "モデルのダウンロード失敗",
                message = throwable.message ?: "${model.displayName} のダウンロードに失敗しました",
                notificationId = modelId.value.hashCode(),
            )
            Result.failure()
        }
    }

    private suspend fun downloadToFile(
        model: LocalModelDefinition,
        tempFile: File,
        destinationFile: File,
    ) = withContext(Dispatchers.IO) {
        val downloadUrl = requireNotNull(model.downloadUrl) {
            "Model does not support file download: ${model.modelId}"
        }
        val connection = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 30_000
            readTimeout = 30_000
            requestMethod = "GET"
        }
        try {
            connection.connect()
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("HTTP $responseCode")
            }

            val totalBytes = connection.contentLengthLong.takeIf { it > 0L }
            var downloadedBytes = 0L
            var lastProgress = -1

            setForeground(createForegroundInfo(model, progress = null))

            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val readSize = input.read(buffer)
                        if (readSize <= 0) break
                        if (isStopped) {
                            throw IOException("ダウンロードが中断されました")
                        }
                        output.write(buffer, 0, readSize)
                        downloadedBytes += readSize

                        val progress = totalBytes
                            ?.takeIf { it > 0L }
                            ?.let { ((downloadedBytes * 100) / it).toInt().coerceIn(0, 100) }
                        if (progress != null && progress != lastProgress) {
                            lastProgress = progress
                            setProgressAsync(createProgressData(progress))
                            setForeground(createForegroundInfo(model, progress))
                        }
                    }
                }
            }

            if (!tempFile.renameTo(destinationFile)) {
                tempFile.copyTo(destinationFile, overwrite = true)
                tempFile.delete()
            }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun createForegroundInfo(
        model: LocalModelDefinition,
        progress: Int?,
    ): ForegroundInfo {
        val contentText = when (progress) {
            null -> "ダウンロード中..."
            else -> "ダウンロード中... $progress%"
        }
        val notification =
            NotificationCompat.Builder(
                applicationContext,
                LOCAL_MODEL_DOWNLOAD_NOTIFICATION_CHANNEL_ID,
            ).setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(model.displayName)
                .setContentText(contentText)
                .setContentIntent(createSettingsPendingIntent())
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setProgress(
                    100,
                    progress ?: 0,
                    progress == null,
                ).build()

        return ForegroundInfo(
            model.modelId.value.hashCode(),
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun showCompletedNotification(
        title: String,
        message: String,
        notificationId: Int,
    ) {
        if (
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification =
            NotificationCompat.Builder(
                applicationContext,
                LOCAL_MODEL_DOWNLOAD_NOTIFICATION_CHANNEL_ID,
            ).setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(createSettingsPendingIntent())
                .setAutoCancel(true)
                .build()
        NotificationManagerCompat.from(applicationContext)
            .notify(notificationId, notification)
    }

    private fun createSettingsPendingIntent(): PendingIntent {
        val launchIntent =
            applicationContext.packageManager.getLaunchIntentForPackage(
                applicationContext.packageName,
            )?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_OPEN_SETTINGS, true)
            } ?: Intent().apply {
                setPackage(applicationContext.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_OPEN_SETTINGS, true)
            }

        return PendingIntent.getActivity(
            applicationContext,
            REQUEST_CODE_SETTINGS,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val KEY_MODEL_ID = "model_id"
        private const val KEY_PROGRESS = "progress"
        private const val REQUEST_CODE_SETTINGS = 10_001

        fun createInputData(modelId: LocalModelId): Data =
            Data.Builder()
                .putString(KEY_MODEL_ID, modelId.value)
                .build()

        fun createProgressData(progress: Int): Data =
            Data.Builder()
                .putInt(KEY_PROGRESS, progress)
                .build()

        fun getProgress(workData: Data): Float? =
            workData.getInt(KEY_PROGRESS, -1)
                .takeIf { it >= 0 }
                ?.div(100f)

        fun getUniqueWorkName(modelId: LocalModelId): String = "local_model_download_${modelId.value}"
    }
}
