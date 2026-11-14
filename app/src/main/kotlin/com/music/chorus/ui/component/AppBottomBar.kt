@file:Suppress("OPT_IN_USAGE_ERROR", "OPT_IN_USAGE")

package pushkar.chorus.music.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import pushkar.chorus.music.ui.player.BottomSheetPlayer
import kotlinx.coroutines.launch
import pushkar.chorus.music.R
import pushkar.chorus.music.constants.FloatingToolbarBottomPadding
import pushkar.chorus.music.constants.FloatingToolbarHorizontalPadding
import pushkar.chorus.music.constants.NavigationBarAnimationSpec
import pushkar.chorus.music.constants.NavigationBarHeight
import pushkar.chorus.music.ui.screens.Screens
import pushkar.chorus.music.ui.utils.resetHeightOffset

@Composable
fun AppBottomBar(
    navController: NavController,
    currentRoute: String?,
    navigationItems: List<Screens>,
    playerBottomSheetState: BottomSheetState,
    shouldShowNavigationBar: Boolean,
    showRail: Boolean,
    isOffline: Boolean,
    useNewMiniPlayerDesign: Boolean,
    pureBlack: Boolean,
    bottomInset: Dp,
    bottomInsetDp: Dp,
    topAppBarScrollBehavior: TopAppBarScrollBehavior,
    onShuffleClick: (() -> Unit)?,
    isNavBarHidden: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val baseBg = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer

    val navigationBarHeight by animateDpAsState(
        targetValue = if (shouldShowNavigationBar && !showRail && !isOffline) NavigationBarHeight else 0.dp,
        animationSpec = NavigationBarAnimationSpec,
        label = "navBarHeight",
    )

    val scrollHideOffset = 0f

    val onNavItemClick: (Screens, Boolean) -> Unit =
        remember(navController, coroutineScope, topAppBarScrollBehavior, playerBottomSheetState) {
            { screen: Screens, isSelected: Boolean ->
                if (playerBottomSheetState.isExpanded) {
                    playerBottomSheetState.collapseSoft()
                }

                if (isSelected) {
                    navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                    coroutineScope.launch {
                        topAppBarScrollBehavior.state.resetHeightOffset()
                    }
                } else {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }

    if (!showRail && currentRoute != "update" && currentRoute != "ambient_mode" && currentRoute != "uptime" && currentRoute?.startsWith(
            "settings"
        ) != true
    ) {
        Box(modifier = modifier) {
            val isPlayerExpandingOrExpanded = playerBottomSheetState.progress > 0.05f
            val navBarZIndex = if (isPlayerExpandingOrExpanded) 1f else 3f
            val playerZIndex = 2f

            Box(modifier = Modifier.zIndex(playerZIndex)) {
                BottomSheetPlayer(
                    state = playerBottomSheetState,
                    navController = navController,
                    pureBlack = pureBlack
                )
            }

            val navSlideDistance = bottomInset + FloatingToolbarBottomPadding + NavigationBarHeight + 48.dp
            val hideFraction = if (NavigationBarHeight > 0.dp) {
                (1f - (navigationBarHeight.coerceAtMost(NavigationBarHeight) / NavigationBarHeight)).coerceIn(0f, 1f)
            } else 1f

            val slideOffset = navSlideDistance * playerBottomSheetState.progress.coerceIn(0f, 1f)
            val hideOffset = navSlideDistance * hideFraction
            val navOffsetY = slideOffset + hideOffset

            if (!isOffline && playerBottomSheetState.progress < 1f && (shouldShowNavigationBar || navigationBarHeight > 0.dp)) {
                Box(
                    modifier = Modifier
                        .zIndex(navBarZIndex)
                        .align(Alignment.BottomCenter)
                        .height(bottomInset + FloatingToolbarBottomPadding + NavigationBarHeight)
                        .offset(y = navOffsetY)
                        .graphicsLayer {
                            val playerAlpha = (1f - playerBottomSheetState.progress * 3f).coerceIn(0f, 1f)
                            val navAlpha = (1f - hideFraction).coerceIn(0f, 1f)
                            alpha = playerAlpha * navAlpha
                        },
                ) {
                    FloatingNavigationToolbar(
                        items = navigationItems,
                        pureBlack = pureBlack,
                        isSelected = { screen ->
                            currentRoute == screen.route || currentRoute?.startsWith("${screen.route}/") == true
                        },
                        onItemClick = onNavItemClick,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(
                                start = FloatingToolbarHorizontalPadding,
                                end = FloatingToolbarHorizontalPadding,
                                bottom = bottomInset + FloatingToolbarBottomPadding,
                            )
                            .height(NavigationBarHeight)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .height(bottomInsetDp)
                    .graphicsLayer {
                        val progress = playerBottomSheetState.progress
                        alpha = if (progress > 0f || !shouldShowNavigationBar) 0f else 1f
                    }
                    .background(baseBg)
            )
        }
    } else {
    if (currentRoute != "update" && currentRoute != "ambient_mode" && currentRoute != "uptime" && currentRoute?.startsWith(
            "settings"
        ) != true
    ) {
        BottomSheetPlayer(
            state = playerBottomSheetState,
            navController = navController,
            pureBlack = pureBlack
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(bottomInsetDp)
            .graphicsLayer {
                val progress = playerBottomSheetState.progress
                alpha = if (progress > 0f || !shouldShowNavigationBar) 0f else 1f
            }
            .background(baseBg)
    )
}
}
