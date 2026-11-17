package pushkar.chorus.music.ui.screens.lockscreen

import android.content.Intent
import android.provider.MediaStore
import android.text.format.DateFormat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import pushkar.chorus.music.LocalPlayerConnection
import pushkar.chorus.music.R
import pushkar.chorus.music.ui.player.rememberArtworkMeshPalette
import pushkar.chorus.music.constants.CropAlbumArtKey
import pushkar.chorus.music.constants.LockscreenPlayerStyle
import pushkar.chorus.music.constants.LockscreenPlayerStyleKey
import pushkar.chorus.music.constants.ThumbnailCornerRadiusKey
import pushkar.chorus.music.utils.makeTimeString
import pushkar.chorus.music.utils.rememberEnumPreference
import pushkar.chorus.music.utils.rememberPreference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LockscreenPlayer(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current ?: return

    // Smart OS & Hardware layout adaptation
    val osConfig = remember { PhoneOsAdapter.getStyleConfig() }

    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val isLiked = currentSong?.song?.liked == true

    // Universal Hardware Biometric Detection
    val biometricType = remember { LockscreenBiometricHelper.detectBiometricType(context) }

    // Dynamic Theme Color Palette sampled from current track's album art
    val meshPalette = rememberArtworkMeshPalette(mediaMetadata?.thumbnailUrl)
    val primaryThemeColor = meshPalette.colors.firstOrNull() ?: Color(0xFF242430)
    val secondaryThemeColor = meshPalette.colors.getOrNull(1) ?: Color(0xFF161620)
    val darkBaseThemeColor = meshPalette.colors.getOrNull(2) ?: Color(0xFF0A0A0D)

    val cropAlbumArt by rememberPreference(CropAlbumArtKey, true)
    val thumbnailCornerRadius by rememberPreference(ThumbnailCornerRadiusKey, defaultValue = 3f)

    val (preferredStyle, setPreferredStyle) = rememberEnumPreference(
        LockscreenPlayerStyleKey,
        defaultValue = LockscreenPlayerStyle.CARD
    )
    var currentStyle by remember(preferredStyle) { mutableStateOf(preferredStyle) }

    // Position updates
    var position by remember { mutableLongStateOf(playerConnection.player.currentPosition) }
    var duration by remember { mutableLongStateOf(playerConnection.player.duration) }
    var dragPosition by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(playerConnection.player, isPlaying) {
        while (isActive) {
            position = playerConnection.player.currentPosition
            duration = playerConnection.player.duration
            delay(500)
        }
    }

    // Time & Date for lock screen header
    val is24Hour = DateFormat.is24HourFormat(context)
    val currentTimeMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (isActive) {
            value = System.currentTimeMillis()
            delay(1000)
        }
    }
    val timeFormat = remember(is24Hour) {
        SimpleDateFormat(if (is24Hour) "HH:mm" else "h:mm", Locale.getDefault())
    }
    val dateFormat = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
    }
    val timeString = remember(currentTimeMillis, timeFormat) {
        timeFormat.format(Date(currentTimeMillis))
    }
    val dateStringRaw = remember(currentTimeMillis, dateFormat) {
        dateFormat.format(Date(currentTimeMillis))
    }
    val dateString = remember(dateStringRaw, osConfig.dateAllCaps) {
        if (osConfig.dateAllCaps) dateStringRaw.uppercase(Locale.getDefault()) else dateStringRaw
    }

    var verticalDragOffset by remember { mutableStateOf(0f) }

    // Pulsing animation for swipe up prompt
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val chevronAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chevronAlpha"
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val isCompactHeight = screenHeight < 700.dp

        // Background: Either Full Immersive Artwork (with top/bottom frosted blur) or Dynamic Album Theme Color
        if (currentStyle == LockscreenPlayerStyle.IMMERSIVE) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = mediaMetadata?.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            currentStyle = LockscreenPlayerStyle.CARD
                            setPreferredStyle(LockscreenPlayerStyle.CARD)
                        }
                )

                // Top frosted blur & gradient vignette for clock and status bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Bottom frosted blur & gradient vignette for controls and unlock area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(460.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.40f),
                                    Color.Black.copy(alpha = 0.80f),
                                    Color.Black.copy(alpha = 0.95f)
                                )
                            )
                        )
                )
            }
        } else {
            // Card mode: Dynamic Album Theme Color Background (Replaces phone lockscreen)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                primaryThemeColor.copy(alpha = 0.55f),
                                secondaryThemeColor.copy(alpha = 0.35f),
                                darkBaseThemeColor.copy(alpha = 0.88f),
                                Color(0xFF08080A)
                            )
                        )
                    )
            )
            // Ambient radial glow centered behind floating card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryThemeColor.copy(alpha = 0.38f),
                                Color.Transparent
                            ),
                            radius = 850f
                        )
                    )
            )
        }

        // Foreground Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 8.dp,
                    bottom = osConfig.bottomFingerprintSafePadding
                )
        ) {
            // Smart Lockscreen Clock & Date Header (Auto-adapted to Phone OS)
            Spacer(Modifier.height(4.dp))
            Text(
                text = timeString,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = osConfig.clockFontWeight,
                    fontFamily = osConfig.clockFontFamily,
                    fontSize = if (currentStyle == LockscreenPlayerStyle.IMMERSIVE || isCompactHeight) {
                        (osConfig.clockFontSize.value * 0.85f).sp
                    } else {
                        osConfig.clockFontSize
                    },
                    letterSpacing = osConfig.clockLetterSpacing
                ),
                color = Color.White
            )
            Text(
                text = dateString,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = osConfig.dateFontWeight,
                    letterSpacing = osConfig.dateLetterSpacing
                ),
                color = Color.White.copy(alpha = 0.85f)
            )

            Spacer(Modifier.height(if (isCompactHeight) 8.dp else 16.dp))

            // Center Artwork Card (Auto-scaled with guaranteed clearance from controls)
            if (currentStyle == LockscreenPlayerStyle.CARD) {
                val cardDimension = min(screenWidth * 0.56f, screenHeight * 0.26f)
                val cornerRadiusDp = (thumbnailCornerRadius * 6).coerceIn(12f, 28f).dp

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    // Ambient drop-shadow behind floating card
                    Box(
                        modifier = Modifier
                            .size(cardDimension)
                            .shadow(
                                elevation = 24.dp,
                                shape = RoundedCornerShape(cornerRadiusDp),
                                spotColor = Color.Black.copy(alpha = 0.8f),
                                ambientColor = Color.Black.copy(alpha = 0.6f)
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(cardDimension)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(cornerRadiusDp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                currentStyle = LockscreenPlayerStyle.IMMERSIVE
                                setPreferredStyle(LockscreenPlayerStyle.IMMERSIVE)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = mediaMetadata?.thumbnailUrl,
                            contentDescription = null,
                            contentScale = if (cropAlbumArt) ContentScale.Crop else ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.height(if (isCompactHeight) 12.dp else 22.dp))
            } else {
                Spacer(Modifier.weight(1f))
            }

            // Player Container (In IMMERSIVE mode, wrapped in OS-adapted frosted card)
            val containerModifier = if (currentStyle == LockscreenPlayerStyle.IMMERSIVE) {
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(osConfig.cardCornerRadius))
                    .background(Color(0xFF1C1C1E).copy(alpha = osConfig.glassCardAlpha))
                    .border(1.dp, Color.White.copy(alpha = osConfig.glassBorderAlpha), RoundedCornerShape(osConfig.cardCornerRadius))
                    .padding(horizontal = 20.dp, vertical = if (isCompactHeight) 12.dp else 16.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
            }

            Column(modifier = containerModifier) {
                // Metadata & Like Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = mediaMetadata?.title ?: stringResource(R.string.music_player),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isCompactHeight) 18.sp else 20.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = mediaMetadata?.artists?.joinToString(", ") { it.name }.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = if (isCompactHeight) 13.sp else 14.sp
                            ),
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                        )
                    }

                    IconButton(
                        onClick = { playerConnection.toggleLike() },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isLiked) R.drawable.favorite else R.drawable.favorite_border
                            ),
                            contentDescription = null,
                            tint = if (isLiked) Color(0xFFFF4081) else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Seekbar Scrubber
                val effectivePos = dragPosition ?: position
                val effectiveDuration = if (duration > 0 && duration != C.TIME_UNSET) duration else 0L

                Slider(
                    value = effectivePos.coerceIn(0L, effectiveDuration).toFloat(),
                    valueRange = 0f..(if (effectiveDuration > 0) effectiveDuration.toFloat() else 1f),
                    onValueChange = { dragPosition = it.toLong() },
                    onValueChangeFinished = {
                        dragPosition?.let { playerConnection.player.seekTo(it) }
                        dragPosition = null
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = makeTimeString(effectivePos),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Text(
                        text = if (effectiveDuration > 0) makeTimeString(effectiveDuration) else "--:--",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }

                Spacer(Modifier.height(if (isCompactHeight) 8.dp else 12.dp))

                // Primary Playback Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle Mode Toggle
                    val shuffleMode = playerConnection.player.shuffleModeEnabled
                    IconButton(
                        onClick = { playerConnection.player.shuffleModeEnabled = !shuffleMode },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (shuffleMode) R.drawable.shuffle_on else R.drawable.shuffle
                            ),
                            contentDescription = null,
                            tint = if (shuffleMode) Color.White else Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Previous Track
                    FilledIconButton(
                        onClick = { playerConnection.player.seekToPrevious() },
                        shape = osConfig.buttonShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(if (isCompactHeight) 48.dp else 52.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Play / Pause (Chorus Signature Shape Morph)
                    val playPauseRoundness by animateDpAsState(
                        targetValue = if (isPlaying) 18.dp else 32.dp,
                        animationSpec = tween(durationMillis = 120, easing = LinearEasing),
                        label = "playPauseRoundness"
                    )
                    FilledIconButton(
                        onClick = { playerConnection.togglePlayPause() },
                        shape = RoundedCornerShape(playPauseRoundness),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.size(if (isCompactHeight) 58.dp else 64.dp)
                    ) {
                        Icon(
                            painter = painterResource(
                                if (isPlaying) R.drawable.pause else R.drawable.play
                            ),
                            contentDescription = null,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Next Track
                    FilledIconButton(
                        onClick = { playerConnection.player.seekToNext() },
                        shape = osConfig.buttonShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(if (isCompactHeight) 48.dp else 52.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Repeat Mode Toggle
                    val repeatMode = playerConnection.player.repeatMode
                    IconButton(
                        onClick = {
                            playerConnection.player.repeatMode = when (repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                else -> Player.REPEAT_MODE_OFF
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        val (iconRes, iconTint) = when (repeatMode) {
                            Player.REPEAT_MODE_OFF -> R.drawable.repeat to Color.White.copy(alpha = 0.45f)
                            Player.REPEAT_MODE_ONE -> R.drawable.repeat_one_on to Color.White
                            Player.REPEAT_MODE_ALL -> R.drawable.repeat_on to Color.White
                            else -> R.drawable.repeat to Color.White.copy(alpha = 0.45f)
                        }
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Bottom Area: Biometrics, Unlock Prompt, and Lockscreen Corner Shortcuts
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .padding(bottom = 6.dp)
            ) {
                // Bottom-left Shortcut: Assistant / Voice
                IconButton(
                    onClick = {
                        try {
                            val voiceIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(voiceIntent)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.mic),
                        contentDescription = "Voice Assistant",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Center Unlock Affordance & Universal Adaptive Biometric Target
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { verticalDragOffset = 0f },
                                onDragEnd = {
                                    if (verticalDragOffset < -30f) {
                                        onUnlock()
                                    }
                                    verticalDragOffset = 0f
                                },
                                onDragCancel = { verticalDragOffset = 0f },
                                onVerticalDrag = { _, dragAmount ->
                                    verticalDragOffset += dragAmount
                                }
                            )
                        }
                        .clickable { onUnlock() }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.expand_less),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = chevronAlpha),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (biometricType == BiometricDisplayType.SIDE_MOUNTED_FINGERPRINT) {
                            "Touch side sensor or swipe up"
                        } else {
                            "Swipe up to unlock"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.White.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center
                    )

                    if (biometricType == BiometricDisplayType.IN_DISPLAY_FINGERPRINT) {
                        Spacer(Modifier.height(8.dp))
                        // Exact circular biometric scanner target ring replica for in-display phones
                        val sensorRingScale by infiniteTransition.animateFloat(
                            initialValue = 0.98f,
                            targetValue = 1.02f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "sensorRingScale"
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(60.dp)
                                .graphicsLayer {
                                    scaleX = sensorRingScale
                                    scaleY = sensorRingScale
                                }
                                .clip(CircleShape)
                                .border(1.75.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                                .clickable { onUnlock() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f))
                            )
                        }
                    }
                }

                // Bottom-right Shortcut: Camera
                IconButton(
                    onClick = {
                        try {
                            val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(cameraIntent)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lockscreen_camera),
                        contentDescription = "Camera",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
