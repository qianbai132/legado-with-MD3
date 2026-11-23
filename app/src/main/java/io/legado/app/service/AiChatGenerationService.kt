package io.legado.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import io.legado.app.R
import io.legado.app.constant.IntentAction
import io.legado.app.constant.NotificationId
import io.legado.app.ui.main.MainActivity
import io.legado.app.utils.startForegroundServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the process alive while an AI chat generation
 * is running in the background. The actual generation coroutine lives in the
 * application-scoped [io.legado.app.domain.usecase.AiChatAppScope]; this service
 * only provides the persistent notification and a partial wake lock so that
 * generation continues when the user switches to the reader or locks the screen.
 */
class AiChatGenerationService : android.app.Service() {

    companion object {
        const val CHANNEL_ID = "channel_ai_chat_generation"
        private const val EXTRA_TITLE = "aiChatGenTitle"

        fun start(context: Context, title: String) {
            val intent = Intent(context, AiChatGenerationService::class.java).apply {
                action = IntentAction.start
                putExtra(EXTRA_TITLE, title)
            }
            context.startForegroundServiceCompat(intent)
        }

        fun stop(context: Context) {
            runCatching {
                context.startService(
                    Intent(context, AiChatGenerationService::class.java).apply {
                        action = IntentAction.stop
                    }
                )
            }
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var wakeLock: PowerManager.WakeLock? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val existing = nm.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.ai_chat),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    enableLights(false)
                    enableVibration(false)
                    setSound(null, null)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun buildNotification(title: String): android.app.Notification {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            MainActivity.createLauncherIntent(this),
            flags,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, AiChatGenerationService::class.java).apply { action = IntentAction.stop },
            flags,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_network_check)
            .setContentTitle(getString(R.string.ai_chat_generating_title))
            .setContentText(title)
            .setContentIntent(openIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(R.drawable.ic_stop_black_24dp, getString(R.string.stop), stopIntent)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            IntentAction.start -> {
                val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
                startForeground(NotificationId.AiChatGeneration, buildNotification(title))
                acquireWakeLock()
            }

            IntentAction.stop -> stopSelfSafely()
        }
        return START_NOT_STICKY
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "legado:AiChatGeneration",
        ).apply { setReferenceCounted(false); acquire(10 * 60 * 1000L) }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun stopSelfSafely() {
        scope.launch {
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        releaseWakeLock()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null
}
