package pushkar.chorus.music.ui.player

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pushkar.chorus.music.constants.ReduceDynamicBlurKey
import pushkar.chorus.music.utils.rememberPreference
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val FallbackColors = listOf(
    Color(0xFF141416),
    Color(0xFF1E1E22),
    Color(0xFF16161A),
    Color(0xFF101012),
)

private val meshPaletteCache = android.util.LruCache<String, List<Color>>(64)
private val meshPrewarmScope =
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

fun prewarmMeshPalette(context: android.content.Context, url: String?) {
    if (url.isNullOrEmpty() || meshPaletteCache.get(url) != null) return
    val appContext = context.applicationContext
    meshPrewarmScope.launch {
        try {
            val request = ImageRequest.Builder(appContext)
                .data(url)
                .size(128)
                .allowHardware(false)
                .build()
            val result = SingletonImageLoader.get(appContext).execute(request)
            val bitmap = (result as? SuccessResult)?.image?.toBitmap() ?: return@launch
            val extracted = withContext(Dispatchers.Default) { paletteOf(bitmap) }
            meshPaletteCache.put(url, extracted)
        } catch (_: Exception) {
        }
    }
}

@Immutable
data class MeshPalette(val colors: List<Color>)

@Composable
fun MeshGradientBackground(
    palette: MeshPalette,
    modifier: Modifier = Modifier,
    trackKey: Any? = null,
    driftMillis: Int = 8_000,
    transitionMillis: Int = 1_400,
) {
    val (reduceMotion) = rememberPreference(ReduceDynamicBlurKey, false)

    val tuned = remember(palette.colors) {
        (palette.colors.ifEmpty { FallbackColors } + FallbackColors)
            .take(4)
            .map { it.tuned() }
    }

    val colorSpec: AnimationSpec<Color> = remember(reduceMotion, transitionMillis) {
        if (reduceMotion) {
            snap()
        } else {
            tween(
                transitionMillis,
                easing = androidx.compose.animation.core.CubicBezierEasing(0.37f, 0f, 0.63f, 1f),
            )
        }
    }
    val animatedColors = tuned.mapIndexed { index, color ->
        animateColorAsState(color, colorSpec, label = "meshColor$index")
    }
    val baseColor = animateColorAsState(
        tuned.first().dimmed(),
        colorSpec,
        label = "meshBase",
    )

    val phase = remember { Animatable(0f) }
    LaunchedEffect(trackKey, reduceMotion) {
        if (reduceMotion) {
            phase.snapTo(0f)
        } else {
            phase.animateTo(
                targetValue = phase.value + DriftRadians,
                animationSpec = tween(driftMillis, easing = FastOutSlowInEasing),
            )
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = 1.3f
                scaleY = 1.3f
            }
            .blur(64.dp),
    ) {
        val drift = phase.value
        drawRect(baseColor.value)

        animatedColors.forEachIndexed { index, colorState ->
            val color = colorState.value
            val anchor = BlobAnchors[index]
            val speed = BlobSpeeds[index]
            val center = Offset(
                x = (anchor.x + 0.16f * cos(drift * speed + index * 1.7f)) * size.width,
                y = (anchor.y + 0.16f * sin(drift * speed * 0.9f + index * 2.3f)) * size.height,
            )
            val radius = size.maxDimension * 0.62f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0f)),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }

        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Black.copy(alpha = 0.10f),
                    Color.Black.copy(alpha = 0.38f),
                ),
            ),
        )
    }
}

@Composable
fun rememberArtworkMeshPalette(imageUrl: String?, canvasFrame: Bitmap? = null): MeshPalette {
    val context = LocalContext.current
    val currentCanvasFrame by androidx.compose.runtime.rememberUpdatedState(canvasFrame)
    val cached = remember(imageUrl) { imageUrl?.let(meshPaletteCache::get) }
    var palette by remember(imageUrl) {
        mutableStateOf(MeshPalette(cached ?: FallbackColors))
    }

    LaunchedEffect(imageUrl) {
        if (imageUrl == null) return@LaunchedEffect
        if (cached != null) {
            palette = MeshPalette(cached)
            return@LaunchedEffect
        }
        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .size(128)
            .allowHardware(false)
            .build()
        val result = SingletonImageLoader.get(context).execute(request)
        val bitmap = (result as? SuccessResult)?.image?.toBitmap() ?: return@LaunchedEffect
        val extracted = withContext(Dispatchers.Default) { paletteOf(bitmap) }
        meshPaletteCache.put(imageUrl, extracted)
        if (currentCanvasFrame == null) palette = MeshPalette(extracted)
    }

    LaunchedEffect(canvasFrame) {
        val frame = canvasFrame ?: return@LaunchedEffect
        val extracted = withContext(Dispatchers.Default) { paletteOf(frame) }
        palette = MeshPalette(extracted)
    }
    return palette
}

private const val DriftRadians = (PI * 0.45f).toFloat()

private val BlobAnchors = listOf(
    Offset(0.20f, 0.25f),
    Offset(0.80f, 0.20f),
    Offset(0.75f, 0.80f),
    Offset(0.25f, 0.75f),
)

private val BlobSpeeds = listOf(1f, -0.7f, 0.85f, -1.15f)

private fun paletteOf(bitmap: Bitmap): List<Color> {
    fun swatchesOf(builder: Palette.Builder): List<Color> =
        builder.maximumColorCount(24).generate().swatches
            .sortedByDescending { it.population }
            .map { Color(it.rgb) }

    val found = swatchesOf(Palette.from(bitmap)).ifEmpty {
        swatchesOf(Palette.from(bitmap).clearFilters())
    }

    val distinct = found.distinctEnough()
    return when {
        distinct.isEmpty() -> FallbackColors
        distinct.size >= 4 -> distinct.take(4)
        else -> distinct.expandedToFour()
    }
}

private fun List<Color>.distinctEnough(): List<Color> {
    val kept = mutableListOf<Color>()
    forEach { color -> if (kept.none { it.isCloseTo(color) }) kept += color }
    return kept
}

private fun Color.isCloseTo(other: Color): Boolean {
    val a = hsl()
    val b = other.hsl()
    val hueGap = abs(a[0] - b[0]).let { min(it, 360f - it) }
    return hueGap < 15f && abs(a[2] - b[2]) < 0.12f
}

private fun List<Color>.expandedToFour(): List<Color> {
    val out = toMutableList()
    var step = 1
    while (out.size < 4) {
        out += this[(out.size - size) % size].shifted(24f * step, 0.12f * step)
        step++
    }
    return out
}

private fun Color.shifted(hue: Float, lightness: Float): Color {
    val hsl = hsl()
    hsl[0] = (hsl[0] + hue) % 360f
    hsl[2] = (hsl[2] + lightness).coerceIn(0.2f, 0.7f)
    return Color(ColorUtils.HSLToColor(hsl))
}

private fun Color.hsl(): FloatArray =
    FloatArray(3).also { ColorUtils.colorToHSL(toArgb(), it) }

internal fun Color.tuned(): Color {
    val hsl = hsl()
    hsl[1] = (hsl[1] * 1.35f).coerceAtMost(1f)
    hsl[2] = hsl[2].coerceIn(0.28f, 0.58f)
    return Color(ColorUtils.HSLToColor(hsl))
}

internal fun Color.dimmed(): Color {
    val hsl = hsl()
    hsl[2] = 0.12f
    return Color(ColorUtils.HSLToColor(hsl))
}
