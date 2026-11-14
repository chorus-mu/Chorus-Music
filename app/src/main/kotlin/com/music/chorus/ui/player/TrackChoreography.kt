package pushkar.chorus.music.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import pushkar.chorus.music.models.MediaMetadata
import kotlinx.coroutines.delay

@Stable
class TrackChoreography internal constructor(
    val artwork: State<MediaMetadata?>,
    val text: State<MediaMetadata?>,
    val detail: State<MediaMetadata?>,
    val ambient: State<MediaMetadata?>,
)

internal const val STAGE_ARTWORK_MS = 90L
internal const val STAGE_TEXT_MS = 200L
internal const val STAGE_DETAIL_MS = 280L
internal const val STAGE_AMBIENT_MS = 360L

@Composable
fun rememberTrackChoreography(metadata: MediaMetadata?): TrackChoreography {
    val artwork = remember { mutableStateOf(metadata) }
    val text = remember { mutableStateOf(metadata) }
    val detail = remember { mutableStateOf(metadata) }
    val ambient = remember { mutableStateOf(metadata) }

    LaunchedEffect(metadata) {
        val sameTrack = ambient.value?.id == metadata?.id
        if (sameTrack || ambient.value == null || metadata == null) {
            artwork.value = metadata
            text.value = metadata
            detail.value = metadata
            ambient.value = metadata
            return@LaunchedEffect
        }
        delay(STAGE_ARTWORK_MS)
        artwork.value = metadata
        delay(STAGE_TEXT_MS - STAGE_ARTWORK_MS)
        text.value = metadata
        delay(STAGE_DETAIL_MS - STAGE_TEXT_MS)
        detail.value = metadata
        delay(STAGE_AMBIENT_MS - STAGE_DETAIL_MS)
        ambient.value = metadata
    }

    return remember { TrackChoreography(artwork, text, detail, ambient) }
}
