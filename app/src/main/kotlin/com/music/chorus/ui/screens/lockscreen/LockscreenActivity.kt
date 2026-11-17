package pushkar.chorus.music.ui.screens.lockscreen

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import pushkar.chorus.music.LocalDatabase
import pushkar.chorus.music.LocalPlayerConnection
import pushkar.chorus.music.constants.LockscreenPlayerEnabledKey
import pushkar.chorus.music.db.MusicDatabase
import pushkar.chorus.music.playback.LockscreenPlayerManager
import pushkar.chorus.music.playback.MusicService
import pushkar.chorus.music.playback.PlayerConnection
import pushkar.chorus.music.ui.theme.chorusmusicTheme
import pushkar.chorus.music.utils.dataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LockscreenActivity : ComponentActivity() {

    @Inject
    lateinit var database: MusicDatabase

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private var isBound = false

    private val userPresentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT) {
                Timber.tag(TAG).d("ACTION_USER_PRESENT received, finishing LockscreenActivity")
                finish()
            }
        }
    }

    private val dismissReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_DISMISS) {
                Timber.tag(TAG).d("ACTION_DISMISS received, finishing LockscreenActivity")
                finish()
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicService.MusicBinder) {
                try {
                    playerConnection = PlayerConnection(this@LockscreenActivity, service, database, lifecycleScope)
                    Timber.tag(TAG).d("PlayerConnection created in LockscreenActivity")
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Failed to create PlayerConnection in LockscreenActivity")
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerConnection = null
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over keyguard (do NOT turn screen on automatically so sleep and lock work properly)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }

        // Edge-to-edge transparent system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        // Bind to MusicService
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        isBound = true

        // Register user present & dismiss receivers
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        val dismissFilter = IntentFilter(ACTION_DISMISS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(userPresentReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            registerReceiver(dismissReceiver, dismissFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(userPresentReceiver, filter)
            registerReceiver(dismissReceiver, dismissFilter)
        }

        // Dismiss launch notification immediately so status bar remains clean
        val nm = getSystemService(NotificationManager::class.java)
        nm?.cancel(LockscreenPlayerManager.LOCKSCREEN_NOTIFICATION_ID)

        // Monitor if lockscreen player is disabled while activity is open
        lifecycleScope.launch {
            dataStore.data
                .map { it[LockscreenPlayerEnabledKey] ?: false }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (!enabled) {
                        Timber.tag(TAG).d("Lockscreen player disabled in settings, finishing")
                        finish()
                    }
                }
        }

        // Monitor player state: dismiss immediately if song is removed or playback is idle
        lifecycleScope.launch {
            while (isActive) {
                val pc = playerConnection
                if (pc != null) {
                    val metadata = pc.mediaMetadata.value
                    val isIdle = pc.player.playbackState == androidx.media3.common.Player.STATE_IDLE
                    val hasMedia = pc.player.currentMediaItem != null || pc.player.mediaItemCount > 0
                    if ((metadata == null && !hasMedia) || (isIdle && !hasMedia)) {
                        Timber.tag(TAG).d("No song playing/loaded in player, auto-finishing LockscreenActivity")
                        finish()
                        break
                    }
                }
                delay(600)
            }
        }

        onBackPressedDispatcher.addCallback(this) {
            unlockAndDismiss()
        }

        setContent {
            chorusmusicTheme(
                darkTheme = true,
                pureBlack = true,
                themeColor = Color.Black
            ) {
                if (playerConnection != null) {
                    CompositionLocalProvider(
                        LocalPlayerConnection provides playerConnection,
                        LocalDatabase provides database,
                    ) {
                        LockscreenPlayer(
                            onUnlock = ::unlockAndDismiss
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent)
                    )
                }
            }
        }
    }

    private fun unlockAndDismiss() {
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguardManager?.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    finish()
                }

                override fun onDismissError() {
                    finish()
                }

                override fun onDismissCancelled() {
                    // Stay on lock screen player if unlock cancelled
                }
            })
        } else {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        val nm = getSystemService(NotificationManager::class.java)
        nm?.cancel(LockscreenPlayerManager.LOCKSCREEN_NOTIFICATION_ID)
    }

    override fun onDestroy() {
        val nm = getSystemService(NotificationManager::class.java)
        nm?.cancel(LockscreenPlayerManager.LOCKSCREEN_NOTIFICATION_ID)
        try {
            unregisterReceiver(userPresentReceiver)
        } catch (_: Exception) {}
        try {
            unregisterReceiver(dismissReceiver)
        } catch (_: Exception) {}

        if (isBound) {
            try {
                unbindService(serviceConnection)
            } catch (_: Exception) {}
            isBound = false
        }
        playerConnection = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "LockscreenActivity"
        const val ACTION_DISMISS = "pushkar.chorus.music.ACTION_DISMISS_LOCKSCREEN"
    }
}
