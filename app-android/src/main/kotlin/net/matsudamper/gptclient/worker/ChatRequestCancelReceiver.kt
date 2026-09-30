package net.matsudamper.gptclient.worker

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import java.util.UUID

/**
 * WorkManager 標準のキャンセルは Work が既に終わっていると何もしないため、
 * プロセスが落ちて残った進捗通知もここで消す。
 */
class ChatRequestCancelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val workId = intent.getStringExtra(EXTRA_WORK_ID)?.let(UUID::fromString) ?: return
        NotificationManagerCompat.from(context).cancel(ChatRequestWorker.getProgressNotificationId(workId))
        // onReceive を抜けるとプロセスが止められ、キャンセルが永続化されない可能性があるため完了まで待つ
        val pendingResult = goAsync()
        WorkManager.getInstance(context).cancelWorkById(workId).result
            .addListener({ pendingResult.finish() }, Runnable::run)
    }

    companion object {
        private const val EXTRA_WORK_ID = "work_id"

        fun createPendingIntent(
            context: Context,
            workId: UUID,
            notificationId: Int,
        ): PendingIntent {
            val intent = Intent(context, ChatRequestCancelReceiver::class.java)
                .putExtra(EXTRA_WORK_ID, workId.toString())
            return PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
