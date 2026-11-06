package io.nekohasekai.sagernet.bg

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
import android.os.Build
import android.text.format.Formatter
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.nekohasekai.sagernet.R
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.database.ProxyEntity
import io.nekohasekai.sagernet.database.SagerDatabase
import io.nekohasekai.sagernet.ktx.app
import io.nekohasekai.sagernet.ktx.runOnMainDispatcher
import io.nekohasekai.sagernet.utils.Theme
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * User can customize visibility of notification since Android 8.
 * The default visibility:
 *
 * Android 8.x: always visible due to system limitations
 * VPN:         always invisible because of VPN notification/icon
 * Other:       always visible
 *
 * See also: https://github.com/aosp-mirror/platform_frameworks_base/commit/070d142993403cc2c42eca808ff3fafcee220ac4
 */
class ServiceNotification(
    private val service: BaseService.Interface, title: String,
    channel: String, visible: Boolean = false,
) {
    companion object {
        const val notificationId = 1
        val flags =
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0) or PendingIntent.FLAG_UPDATE_CURRENT

        fun genTitle(ent: ProxyEntity): String {
            val gn = if (DataStore.showGroupInNotification)
                runCatching { SagerDatabase.groupDao.getById(ent.groupId)?.displayName() }.getOrNull() else null
            val profileName = ent.displayName()
            return if (!gn.isNullOrBlank() && !profileName.startsWith("$gn · ")) {
                "$gn · $profileName"
            } else {
                profileName
            }
        }
    }

    private var lastTitle: String? = null
    private var lastSpeedText: String? = null

    // OneBox V1.0.0: 自动动态栏通知（Android 16 实况胶囊 / Live Updates）已移除。
    // OneBox V1.0.1: 前台服务通知本身保留，标题静态；正文在已连接且有下载流量时显示下载速度
    // （见 postNotificationDownloadSpeed），未连接或无流量时恢复无正文。
    @Suppress("UNUSED_PARAMETER")
    suspend fun postStateUpdate(state: BaseService.State) {
        update()
    }

    suspend fun postNotificationTitle(newTitle: String) {
        if (newTitle == lastTitle) return
        lastTitle = newTitle
        useBuilder {
            it.setContentTitle(newTitle)
        }
        update()
    }

    /**
     * OneBox V1.0.1：通知正文显示下载速度（仅下载，不含上传）。
     * 仅在已连接且有下载流量时显示；未连接或无流量时传入 0，恢复无正文的静态通知。
     * 由 TrafficLooper 在现有流量轮询节奏下调用，不另起定时器；正文文本不变时不重复刷新通知。
     * 标题与通知本身不动。
     */
    suspend fun postNotificationDownloadSpeed(downloadBytesPerSec: Long) {
        val text = if (downloadBytesPerSec > 0L) {
            "↓ " + Formatter.formatFileSize(service as Context, downloadBytesPerSec) + "/s"
        } else null
        if (text == lastSpeedText) return
        lastSpeedText = text
        useBuilder {
            it.setContentText(text)
        }
        update()
    }

    suspend fun postNotificationWakeLockStatus(acquired: Boolean) {
        updateActions()
        useBuilder {
            it.priority =
                if (acquired) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW
        }
        update()
    }

    private val builder = NotificationCompat.Builder(service as Context, channel)
        .setWhen(0)
        .setShowWhen(false)
        .setTicker(service.getString(R.string.forward_success))
        .setContentTitle(title)
        .setOnlyAlertOnce(true)
        .setContentIntent(SagerNet.configureIntent(service))
        .setSmallIcon(R.drawable.ic_throne_tile)
        .setCategory(NotificationCompat.CATEGORY_SERVICE)
        .setPriority(if (visible) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_MIN)
        .setOngoing(true)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

    private val buildLock = Mutex()

    private suspend fun useBuilder(f: (NotificationCompat.Builder) -> Unit) {
        buildLock.withLock {
            f(builder)
        }
    }

    init {
        service as Context

        Theme.apply(app)
        Theme.apply(service)

        runOnMainDispatcher {
            updateActions()
            show()
        }
    }

    // OneBox V1.0.3：通知栏只显示订阅信息与下载速度，不再放操作按钮。
    private suspend fun updateActions() {
        service as Context
        useBuilder {
            it.clearActions()
        }
    }

    private suspend fun show() =
        useBuilder {
            try {
                if (Build.VERSION.SDK_INT >= 34) {
                    (service as Service).startForeground(
                        notificationId,
                        it.build(),
                        FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
                    )
                } else {
                    (service as Service).startForeground(notificationId, it.build())
                }
            } catch (e: Exception) {
                Toast.makeText(
                    SagerNet.application,
                    "startForeground: $e",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    private suspend fun update() = useBuilder {
        NotificationManagerCompat.from(service as Service).notify(notificationId, it.build())
    }

    fun destroy() {
        val s = service as? Service
        if (s != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                s.stopForeground(Service.STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                s.stopForeground(true)
            }
            NotificationManagerCompat.from(s).cancel(notificationId)
        }
        SagerNet.notification.cancel(notificationId)
    }
}
