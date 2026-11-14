package pushkar.chorus.music.ui.component

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.music.innertube.YouTube
import pushkar.chorus.music.LocalDatabase
import pushkar.chorus.music.R
import pushkar.chorus.music.constants.InnerTubeCookieKey
import pushkar.chorus.music.db.entities.PlaylistEntity
import pushkar.chorus.music.extensions.isSyncEnabled
import pushkar.chorus.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.util.logging.Logger

/**
 * Full-height playlist creation sheet with cover art picker, clearable title field, and YouTube sync option.
 */
@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
    onPlaylistCreated: ((String) -> Unit)? = null,
) {
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isSignedIn = innerTubeCookie.isNotEmpty()

    var title by remember { mutableStateOf(initialTextFieldValue.orEmpty()) }
    var cover by remember { mutableStateOf<Uri?>(null) }
    var syncedPlaylist by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val canSave = title.isNotBlank() && !saving

    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) cover = uri
    }

    val offset = remember { Animatable(1f) }
    LaunchedEffect(Unit) { offset.animateTo(0f, spring(dampingRatio = 1f, stiffness = 380f)) }

    fun close() {
        coroutineScope.launch {
            offset.animateTo(1f, tween(220))
            onDismiss()
        }
    }

    fun create() {
        if (!canSave) return
        saving = true
        val playlistName = title.trim()
        coroutineScope.launch(Dispatchers.IO) {
            val wantsSync = syncedPlaylist
            val browseId = when {
                !wantsSync -> null
                !isSignedIn -> {
                    Logger.getLogger("CreatePlaylistDialog").warning("Not signed in")
                    null
                }
                else -> runCatching { YouTube.createPlaylist(playlistName) }
                    .onFailure { Logger.getLogger("CreatePlaylistDialog").warning("Playlist sync failed: $it") }
                    .getOrNull()
            }

            val playlistEntity = try {
                val base = PlaylistEntity(
                    name = playlistName,
                    browseId = browseId,
                    bookmarkedAt = LocalDateTime.now(),
                    isEditable = true,
                )
                val thumbnail = cover?.let { persistPlaylistCover(context, it, base.id) }?.toString()
                base.copy(thumbnailUrl = thumbnail).also { playlist ->
                    database.query {
                        insert(playlist)
                    }
                }
            } catch (error: Exception) {
                Logger.getLogger("CreatePlaylistDialog").warning("Local playlist creation failed: $error")
                withContext(Dispatchers.Main) {
                    saving = false
                    Toast.makeText(context, context.getString(R.string.playlist_creation_failed), Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(
                        if (wantsSync && browseId == null) R.string.playlist_created_sync_failed
                        else R.string.playlist_created
                    ),
                    Toast.LENGTH_SHORT
                ).show()
                onPlaylistCreated?.invoke(playlistEntity.id)
                onDismiss()
            }
        }
    }

    Dialog(
        onDismissRequest = ::close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        BackHandler(onBack = ::close)
        val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val sheetColor = MaterialTheme.colorScheme.surface
        val labelColor = MaterialTheme.colorScheme.onSurface
        val secondaryLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        val accentColor = MaterialTheme.colorScheme.primary
        val cornerShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f * (1f - offset.value))),
        ) {
            val travel = with(LocalDensity.current) { maxHeight.toPx() }
            Column(
                Modifier
                    .padding(top = statusTop + 10.dp)
                    .fillMaxSize()
                    .graphicsLayer { translationY = offset.value * travel }
                    .clip(cornerShape)
                    .background(sheetColor)
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 10.dp)
                        .height(44.dp),
                ) {
                    IconButton(
                        onClick = ::close,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close),
                            contentDescription = stringResource(R.string.cancel),
                            tint = labelColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = stringResource(R.string.new_playlist),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = labelColor,
                        modifier = Modifier.align(Alignment.Center),
                    )

                    IconButton(
                        onClick = ::create,
                        enabled = canSave,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (canSave) accentColor else MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check),
                            contentDescription = stringResource(R.string.create_playlist),
                            tint = if (canSave) Color.White else secondaryLabelColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Cover: square well; tapping launches photo picker
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .size(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable { pickCover.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                ) {
                    if (cover != null) {
                        AsyncImage(
                            model = cover,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(accentColor),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.add),
                                contentDescription = stringResource(R.string.choose_playlist_cover),
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                val focus = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(200)
                    runCatching { focus.requestFocus() }
                }
                val fieldStyle = TextStyle(
                    fontSize = 20.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = labelColor,
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(top = 36.dp, start = 24.dp, end = 24.dp)
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    if (title.isEmpty()) {
                        Text(
                            text = stringResource(R.string.playlist_title),
                            style = fieldStyle.copy(color = secondaryLabelColor.copy(alpha = 0.6f))
                        )
                    }
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        textStyle = fieldStyle,
                        cursorBrush = SolidColor(accentColor),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { create() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focus),
                    )
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                }

                if (allowSyncing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                            .height(52.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.sync_playlist),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = labelColor,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = syncedPlaylist,
                            onCheckedChange = {
                                coroutineScope.launch {
                                    when {
                                        !isSignedIn && !syncedPlaylist -> Toast.makeText(
                                            context, context.getString(R.string.not_logged_in_youtube), Toast.LENGTH_SHORT,
                                        ).show()
                                        !context.isSyncEnabled() -> Toast.makeText(
                                            context, context.getString(R.string.sync_disabled), Toast.LENGTH_SHORT,
                                        ).show()
                                        else -> syncedPlaylist = !syncedPlaylist
                                    }
                                }
                            },
                        )
                    }
                    Text(
                        text = stringResource(R.string.allows_for_sync_witch_youtube),
                        fontSize = 13.sp,
                        color = secondaryLabelColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * Persists the chosen cover image file into context.filesDir so it survives cache cleanup.
 */
internal fun persistPlaylistCover(context: Context, src: Uri, playlistId: String): Uri? = runCatching {
    val safeId = playlistId.replace(Regex("[^A-Za-z0-9_-]"), "_")
    val dir = File(context.filesDir, "playlist_covers").apply { mkdirs() }
    val dest = File(dir, "${safeId}_${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(src)?.use { input ->
        dest.outputStream().use { out -> input.copyTo(out) }
    } ?: return@runCatching null
    if (dest.length() == 0L) {
        dest.delete()
        return@runCatching null
    }
    dir.listFiles { f -> f != dest && f.name.startsWith("${safeId}_") }?.forEach { it.delete() }
    dest.toUri()
}.getOrNull()
