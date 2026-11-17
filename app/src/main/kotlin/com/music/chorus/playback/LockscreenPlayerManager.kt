package pushkar.chorus.music.playback

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import pushkar.chorus.music.R
import pushkar.chorus.music.constants.LockscreenPlayerEnabledKey
import pushkar.chorus.music.constants.LockscreenPlayerStyle
import pushkar.chorus.music.constants.LockscreenPlayerStyleKey
import pushkar.chorus.music.ui.screens.lockscreen.LockscreenActivity
import pushkar.chorus.music.utils.dataStore
import timber.log.Timber

class LockscreenPlayerManager(
    private val service: MusicService,
    private val scope: CoroutineScope,
) {
    private var isReceiverRegistered = false
    private var preferenceJob: Job? = null
    var isEnabled: Boolean = false
        private set
    var preferredStyle: LockscreenPlayerStyle = LockscreenPlayerStyle.CARD
        private set

    private val keyguardManager by lazy {
        service.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    }

    private val notificationManager by lazy {
        service.getSystemService(NotificationManager::class.java)
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Timber.tag(TAG).d("ACTION_SCREEN_OFF received - allowing phone to sleep")
                }
                Intent.ACTION_SCREEN_ON -> {
                    Timber.tag(TAG).d("ACTION_SCREEN_ON received")
                    if (keyguardManager?.isKeyguardLocked == true) {
                        tryLaunchLockscreenPlayer()
                    }
                }
                Intent.ACTION_USER_PRESENT -> {
                    Timber.tag(TAG).d("ACTION_USER_PRESENT received - dismissing lockscreen activity")
                    dismissLockscreenPlayer()
                }
            }
        }
    }

    fun start() {
        createNotificationChannel()
        preferenceJob?.cancel()
        preferenceJob = scope.launch(Dispatchers.Main) {
            service.dataStore.data
                .distinctUntilChanged()
                .collect { prefs ->
                    val newEnabled = prefs[LockscreenPlayerEnabledKey] ?: false
                    val newStyleStr = prefs[LockscreenPlayerStyleKey] ?: LockscreenPlayerStyle.CARD.name
                    val newStyle = try {
                        LockscreenPlayerStyle.valueOf(newStyleStr)
                    } catch (e: Exception) {
                        LockscreenPlayerStyle.CARD
                    }

                    val changed = (newEnabled != isEnabled || newStyle != preferredStyle)
                    isEnabled = newEnabled
                    preferredStyle = newStyle

                    if (isEnabled) {
                        registerReceiver()
                    } else {
                        unregisterReceiver()
                        dismissLockscreenPlayer()
                    }

                    if (changed) {
                        Timber.tag(TAG).d("Lockscreen player pref updated: enabled=$isEnabled, style=$preferredStyle")
                        service.updateLockscreenMediaPresentation()
                    }
                }
        }
    }

    fun onPlaybackStateChanged(isPlaying: Boolean) {
        if (!isEnabled) return

        val player = try {
            service.player
        } catch (_: Exception) {
            return
        }

        val hasTrack = player.currentMediaItem != null || player.mediaItemCount > 0

        if (isPlaying && hasTrack) {
            if (keyguardManager?.isKeyguardLocked == true) {
                tryLaunchLockscreenPlayer()
            }
        } else if (!hasTrack || player.playbackState == androidx.media3.common.Player.STATE_IDLE) {
            // Dismiss immediately when songs are removed from the player or playback is idle
            dismissLockscreenPlayer()
        }

        service.updateLockscreenMediaPresentation()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                LOCKSCREEN_CHANNEL_ID,
                "Lock Screen Music Player",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Displays the Chorus music player over the lock screen during playback"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun registerReceiver() {
        if (isReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                service.registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                service.registerReceiver(screenReceiver, filter)
            }
            isReceiverRegistered = true
            Timber.tag(TAG).d("Screen receiver registered for Lockscreen player")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to register screen receiver")
        }
    }

    private fun unregisterReceiver() {
        if (!isReceiverRegistered) return
        try {
            service.unregisterReceiver(screenReceiver)
            isReceiverRegistered = false
            Timber.tag(TAG).d("Screen receiver unregistered for Lockscreen player")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to unregister screen receiver")
        }
    }

    fun tryLaunchLockscreenPlayer() {
        if (!isEnabled) return
        if (!MusicService.isRunning) return
        val player = try {
            service.player
        } catch (_: Exception) {
            return
        }

        val hasTrack = player.currentMediaItem != null || player.mediaItemCount > 0
        if (!hasTrack) return
        if (!player.isPlaying && player.playbackState != androidx.media3.common.Player.STATE_READY) return

        try {
            val intent = Intent(service, LockscreenActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
            }

            val pendingIntent = PendingIntent.getActivity(
                service,
                LOCKSCREEN_NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(service)
            } else true

            // 1. Direct startActivity (unrestricted by Android if overlay permission is granted)
            if (hasOverlay) {
                try {
                    service.startActivity(intent)
                    Timber.tag(TAG).d("Started LockscreenActivity directly via overlay permission")
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Direct startActivity with overlay failed")
                }
            }

            // 2. Full-Screen Intent via High-Priority Notification (Standard Android Keyguard wake)
            val currentMediaItem = player.currentMediaItem
            val songTitle = currentMediaItem?.mediaMetadata?.title?.toString() ?: "Chorus Music"
            val songArtist = currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Now Playing"

            val notification = NotificationCompat.Builder(service, LOCKSCREEN_CHANNEL_ID)
                .setSmallIcon(R.drawable.chorus_music_icon)
                .setContentTitle(songTitle)
                .setContentText(songArtist)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(pendingIntent, true)
                .setAutoCancel(true)
                .setOngoing(false)
                .build()

            notificationManager?.notify(LOCKSCREEN_NOTIFICATION_ID, notification)
            Timber.tag(TAG).d("Dispatched lockscreen launch via fullScreenIntent notification")

            // 3. PendingIntent.send fallback with background launch options (Android 14+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    val options = android.app.ActivityOptions.makeBasic().apply {
                        @Suppress("DEPRECATION")
                        setPendingIntentBackgroundActivityStartMode(
                            android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                        )
                    }
                    pendingIntent.send(service, 0, null, null, null, null, options.toBundle())
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start LockscreenActivity")
        }
    }

    fun dismissLockscreenPlayer() {
        try {
            service.sendBroadcast(
                Intent(LockscreenActivity.ACTION_DISMISS).setPackage(service.packageName)
            )
            notificationManager?.cancel(LOCKSCREEN_NOTIFICATION_ID)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error dismissing lockscreen player")
        }
    }

    fun stop() {
        preferenceJob?.cancel()
        preferenceJob = null
        unregisterReceiver()
        dismissLockscreenPlayer()
    }

    companion object {
        private const val TAG = "LockscreenPlayerMgr"
        const val LOCKSCREEN_NOTIFICATION_ID = 8881
        private const val LOCKSCREEN_CHANNEL_ID = "chorus_lockscreen_player_channel"
    }
}
