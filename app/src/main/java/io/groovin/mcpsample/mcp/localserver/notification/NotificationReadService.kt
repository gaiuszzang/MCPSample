package io.groovin.mcpsample.mcp.localserver.notification

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import io.groovin.mcpsample.util.logd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


class NotificationReadService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        serviceScope.launch {
            sbn?.let {
                val packageName = it.packageName
                val extras = it.notification.extras
                val title = extras.getString(Notification.EXTRA_TITLE)
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)
                val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
                val appName = packageManager.getApplicationLabel(
                    packageManager.getApplicationInfo(packageName, 0)
                ).toString()
                val datetime = System.currentTimeMillis()
                logd("Notification", "[$packageName / $appName] $title: $text, $subText | $datetime")
                val userNotification = UserNotification(
                    title = title ?: "",
                    text = text?.toString() ?: "",
                    subText = subText?.toString(),
                    packageName = packageName,
                    appName = appName,
                    datetime = datetime
                )
                val dbDao = NotificationDatabaseProvider.getDao(this@NotificationReadService)
                dbDao.insert(userNotification)
                logd("Notification", "db inserted")
                dbDao.deleteOldNotifications(100)
                logd("Notification", "old notifications deleted")
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

fun isNotificationServiceEnabled(context: Context): Boolean {
    val cn = ComponentName(context, NotificationReadService::class.java)
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat?.contains(cn.flattenToString()) ?: false
}
