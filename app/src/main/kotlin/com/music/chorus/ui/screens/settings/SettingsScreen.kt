package pushkar.chorus.music.ui.screens.settings

import pushkar.chorus.music.R
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavController
import pushkar.chorus.music.BuildConfig
import pushkar.chorus.music.LocalPlayerAwareWindowInsets
import pushkar.chorus.music.ui.component.IconButton
import pushkar.chorus.music.ui.component.Material3SettingsGroup
import pushkar.chorus.music.ui.component.Material3SettingsItem
import pushkar.chorus.music.ui.screens.Screens
import pushkar.chorus.music.ui.utils.backToMain
import pushkar.chorus.music.chorusmusic.updater.getUpdateAvailableState

@Composable
fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
highlightKey: String? = null) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val isAndroid12OrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val isUpdateAvailable = getUpdateAvailableState(context) && pushkar.chorus.music.chorusmusic.updater.getAutoUpdateCheckSetting(context)

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val searchLower = searchQuery.lowercase()

    val accountText = stringResource(R.string.account)
    val appearanceText = stringResource(R.string.appearance)
    val playerText = stringResource(R.string.player_and_audio)
    val contentText = stringResource(R.string.content)
    val aiLyricsText = stringResource(R.string.ai_lyrics_translation)
    val privacyText = stringResource(R.string.privacy)
    val storageText = stringResource(R.string.storage)
    val backupText = stringResource(R.string.backup_restore)
    val systemUpdateText = stringResource(R.string.system_update)
    val aboutText = stringResource(R.string.about)

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal))
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(
                Modifier.windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Top
                    )
                )
            )
            Spacer(Modifier.height(8.dp))

        val searchBarShape = RoundedCornerShape(24.dp)
        Surface(
            shape = searchBarShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 16.dp)
                .height(52.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { searchQuery = "" },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        val itemsList = buildList {
            if (accountText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == accountText),
                        icon = painterResource(R.drawable.account),
                        title = { Text(accountText) },
                        onClick = { navController.navigate("settings/account") }
                    )
                )
            }

            if (aiLyricsText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
                        isHighlighted = (highlightKey == aiLyricsText),
                        customIcon = {
                            Text(
                                text = "Ai",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                color = if (highlightKey == aiLyricsText)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            )
                        },
                        title = { Text(aiLyricsText) },
                        onClick = { navController.navigate("settings/ai") }
                    )
                )
            }

            if (appearanceText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == appearanceText),
                        icon = painterResource(R.drawable.palette),
                        title = { Text(appearanceText) },
                        onClick = { navController.navigate("settings/appearance") }
                    )
                )
            }
            if (playerText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == playerText),
                        icon = painterResource(R.drawable.play),
                        title = { Text(playerText) },
                        onClick = { navController.navigate("settings/player") }
                    )
                )
            }

            if (contentText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == contentText),
                        icon = painterResource(R.drawable.language),
                        title = { Text(contentText) },
                        onClick = { navController.navigate("settings/content") }
                    )
                )
            }

            if (privacyText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == privacyText),
                        icon = painterResource(R.drawable.security),
                        title = { Text(privacyText) },
                        onClick = { navController.navigate("settings/privacy") }
                    )
                )
            }
            if (storageText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == storageText),
                        icon = painterResource(R.drawable.storage),
                        title = { Text(storageText) },
                        onClick = { navController.navigate("settings/storage") }
                    )
                )
            }
            if (backupText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == backupText),
                        icon = painterResource(R.drawable.restore),
                        title = { Text(backupText) },
                        onClick = { navController.navigate("settings/backup_restore") }
                    )
                )
            }
            if (systemUpdateText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == systemUpdateText),
                        icon = painterResource(if (isUpdateAvailable) R.drawable.ic_launcher_nobg else R.drawable.update),
                        title = { Text(systemUpdateText) },
                        description = if (isUpdateAvailable) {
                            {
                                Text(
                                    text = stringResource(R.string.update_available),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else null,
                        onClick = { navController.navigate("settings/update") }
                    )
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if ("supported links".contains(searchLower)) {
                    add(
                        Material3SettingsItem(
                            isHighlighted = (highlightKey == "supported links"),
                            icon = painterResource(R.drawable.link),
                            title = { Text("Supported Links") },
                            onClick = {
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    when (e) {
                                        is ActivityNotFoundException, is SecurityException -> {
                                            Toast.makeText(context, "Cannot open settings", Toast.LENGTH_SHORT).show()
                                        }
                                        else -> {
                                            Toast.makeText(context, "An error occurred", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                    )
                }
            }
            if (aboutText.lowercase().contains(searchLower)) {
                add(
                    Material3SettingsItem(
    isHighlighted = (highlightKey == aboutText),
                        icon = painterResource(R.drawable.info),
                        title = { Text(aboutText) },
                        onClick = { navController.navigate("settings/about") }
                    )
                )
            }
        }

        val finalItemsList = if (searchQuery.isNotEmpty()) {
            val subSettings = getAllSearchableSettings()

            val matchedSubSettings = subSettings
                .filter { it.first.lowercase().contains(searchLower) }
                .map { (title, parentTitle, route) ->
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.search),
                        title = { Text(title) },
                        description = { Text(parentTitle) },
                        onClick = {
                            val encodedTitle = android.net.Uri.encode(title)
                            val finalRoute = if (route.contains("?")) "$route&highlightKey=$encodedTitle" else "$route?highlightKey=$encodedTitle"
                            navController.navigate(finalRoute)
                        }
                    )
                }

            itemsList + matchedSubSettings
        } else {
            itemsList
        }

        if (finalItemsList.isEmpty() && searchQuery.isNotEmpty()) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "No settings found for \"$searchQuery\"",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        } else {
            Material3SettingsGroup(scrollState = scrollState, items = finalItemsList)
        }

        Spacer(modifier = Modifier.height(50.dp))
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom)
            )
        )
    }

        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                )
            },
            navigationIcon = {
                pushkar.chorus.music.ui.component.TopBarBackButton(navController)
            },
            actions = {},
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
            modifier = Modifier
        )
    }
}
