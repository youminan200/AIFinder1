package kr.ac.pcu.aifinder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class OutingAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_OUTING_ALARM = "kr.ac.pcu.aifinder.ACTION_OUTING_ALARM"
        const val ACTION_MARK_ALL_COMPLETED = "kr.ac.pcu.aifinder.ACTION_MARK_ALL_COMPLETED"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "outing_checklist_channel"
        const val KEY_CHECKLIST = "checklist_items"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_OUTING_ALARM -> {
                showOutingChecklistNotification(context)
            }
            ACTION_MARK_ALL_COMPLETED -> {
                markAllChecklistCompleted(context)
            }
        }
    }

    private fun showOutingChecklistNotification(context: Context) {
        // Read checklist items from SharedPreferences
        val prefs = context.getSharedPreferences("outing_checklist", Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_CHECKLIST, null)
        
        val items = if (stored.isNullOrBlank()) {
            listOf("휴대폰", "지갑", "현관 열쇠", "우산", "보조 배터리")
                .map { ChecklistItem(it, false) }
        } else {
            stored.split("|")
                .filter { it.isNotBlank() }
                .mapNotNull { encoded ->
                    val parts = encoded.split("^")
                    val label = Uri.decode(parts.getOrNull(0).orEmpty())
                    if (label.isBlank()) null else ChecklistItem(label, parts.getOrNull(1) == "1")
                }
        }

        val uncheckedItems = items.filter { !it.checked }

        // If all items are checked, no notification is needed!
        if (uncheckedItems.isEmpty()) return

        val uncheckedNames = uncheckedItems.joinToString(", ") { it.label }
        val message = "외출 전 챙기셨나요? 아직 완료되지 않은 물품이 있습니다: $uncheckedNames"

        // Create notification channel
        createNotificationChannel(context)

        // Intent to open Main Activity
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent to Mark All Completed action
        val completeIntent = Intent(context, OutingAlarmReceiver::class.java).apply {
            action = ACTION_MARK_ALL_COMPLETED
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context, 1, completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("외출 전 소지품 확인")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_save,
                "모두 완료",
                completePendingIntent
            )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun markAllChecklistCompleted(context: Context) {
        val prefs = context.getSharedPreferences("outing_checklist", Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_CHECKLIST, null) ?: return

        val items = stored.split("|")
            .filter { it.isNotBlank() }
            .mapNotNull { encoded ->
                val parts = encoded.split("^")
                val label = Uri.decode(parts.getOrNull(0).orEmpty())
                if (label.isBlank()) null else ChecklistItem(label, true) // Force check true
            }

        val updatedString = items.joinToString("|") { item ->
            "${Uri.encode(item.label)}^1"
        }
        prefs.edit().putString(KEY_CHECKLIST, updatedString).apply()

        // Cancel the active notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "외출 체크 알림"
            val descriptionText = "외출 시 소지품을 챙길 수 있도록 일러주는 알림입니다."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private data class ChecklistItem(val label: String, val checked: Boolean)
}
