package pushkar.chorus.music.ui.screens

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
 import androidx.compose.foundation.lazy.grid.GridCells
 import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
 import androidx.compose.foundation.lazy.grid.items
  import androidx.compose.material3.CenterAlignedTopAppBar
 import androidx.compose.material3.Icon
 import androidx.compose.material3.Text
 import androidx.compose.material3.TopAppBarDefaults
 import androidx.compose.material3.TopAppBarScrollBehavior
 import androidx.compose.ui.graphics.Color
 import androidx.compose.ui.text.font.FontWeight
 import pushkar.chorus.music.ui.component.TopBarBackButton
 import androidx.compose.runtime.Composable
 import androidx.compose.runtime.collectAsState
 import androidx.compose.runtime.getValue
 import androidx.compose.runtime.rememberCoroutineScope
 import androidx.compose.ui.Modifier
 import androidx.compose.ui.res.painterResource
 import androidx.compose.ui.unit.dp
 import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
 import androidx.navigation.NavController
 import com.music.innertube.models.AlbumItem
 import com.music.innertube.models.ArtistItem
 import com.music.innertube.models.PlaylistItem
 import pushkar.chorus.music.LocalPlayerAwareWindowInsets
 import pushkar.chorus.music.LocalPlayerConnection
 import pushkar.chorus.music.R
 import pushkar.chorus.music.constants.GridItemSize
 import pushkar.chorus.music.constants.GridItemsSizeKey
 import pushkar.chorus.music.constants.GridThumbnailHeight
 import pushkar.chorus.music.ui.component.IconButton
 import pushkar.chorus.music.ui.component.LocalMenuState
 import pushkar.chorus.music.ui.component.YouTubeGridItem
 import pushkar.chorus.music.ui.component.shimmer.GridItemPlaceHolder
 import pushkar.chorus.music.ui.component.shimmer.ShimmerHost
 import pushkar.chorus.music.ui.menu.YouTubeAlbumMenu
 import pushkar.chorus.music.ui.menu.YouTubeArtistMenu
 import pushkar.chorus.music.ui.menu.YouTubePlaylistMenu
 import pushkar.chorus.music.ui.utils.backToMain
 import pushkar.chorus.music.utils.rememberEnumPreference
 import pushkar.chorus.music.viewmodels.BrowseViewModel

 @Composable
 fun BrowseScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    browseId: String?,
    viewModel: BrowseViewModel = hiltViewModel(),
) {
     val menuState = LocalMenuState.current
     val playerConnection = LocalPlayerConnection.current ?: return
     val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
     val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

     val title by viewModel.title.collectAsState()
     val items by viewModel.items.collectAsState()

     val coroutineScope = rememberCoroutineScope()
     val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

     Box(modifier = Modifier.fillMaxSize()) {
         LazyVerticalGrid(
             columns = GridCells.Adaptive(minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp),
             contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
         ) {
             items?.let { items ->
                 items(
                     items = items.distinctBy { it.id },
                     key = { it.id }
                 ) { item ->
                     YouTubeGridItem(
                         item = item,
                         isPlaying = isPlaying,
                         fillMaxWidth = true,
                         coroutineScope = coroutineScope,
                         modifier = Modifier
                             .combinedClickable(
                                 onClick = {
                                     when (item) {
                                         is AlbumItem -> navController.navigate("album/${item.id}")
                                         is PlaylistItem -> navController.navigate("online_playlist/${item.id}")
                                         is ArtistItem -> navController.navigate("artist/${item.id}")
                                         else -> {}
                                     }
                                 },
                                 onLongClick = {
                                     menuState.show {
                                         when (item) {
                                             is AlbumItem ->
                                                 YouTubeAlbumMenu(
                                                     albumItem = item,
                                                     navController = navController,
                                                     onDismiss = menuState::dismiss
                                                 )

                                             is PlaylistItem -> {
                                                 YouTubePlaylistMenu(
                                                     playlist = item,
                                                     coroutineScope = coroutineScope,
                                                     onDismiss = menuState::dismiss
                                                 )
                                             }

                                             is ArtistItem -> {
                                                 YouTubeArtistMenu(
                                                     artist = item,
                                                     onDismiss = menuState::dismiss
                                                 )
                                             }

                                             else -> {}
                                         }
                                     }
                                 }
                             )
                     )
                 }

                 if (items.isEmpty()) {
                     items(8) {
                         ShimmerHost {
                             GridItemPlaceHolder(fillMaxWidth = true)
                         }
                     }
                 }
             }
         }

         CenterAlignedTopAppBar(
             title = { Text(title ?: "", fontWeight = FontWeight.Bold) },
             navigationIcon = {
                 TopBarBackButton(navController = navController)
             },
             colors = TopAppBarDefaults.topAppBarColors(
                 containerColor = Color.Transparent,
                 scrolledContainerColor = Color.Transparent,
             ),
             scrollBehavior = scrollBehavior,
         )
     }
 }
