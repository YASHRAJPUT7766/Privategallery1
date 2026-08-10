package com.yash.privategallery.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.yash.privategallery.domain.model.MediaType
import com.yash.privategallery.ui.common.SecureScreenEffect

/**
 * Section 9: full-screen media viewer. Backed by a [HorizontalPager] over
 * the current collection (see [ViewerViewModel] docs for how the collection
 * is resolved) so swiping moves through exactly the same list the user was
 * just browsing, video and image items freely mixed (Section 26). Video
 * items render a static thumbnail + tap-to-open-player affordance rather
 * than embedding ExoPlayer directly in the pager, since a pager page should
 * stay lightweight when off-screen — the actual playback surface is
 * [VideoPlayerScreen], reached via [onOpenVideoPlayer].
 *
 * "Do NOT reset to the first image after returning from editing" (Section 9)
 * is satisfied by [ViewerViewModel.currentIndex] persisting in the
 * ViewModel (survives the editor round-trip via the back stack) rather than
 * being derived fresh from [startIndex] on every recomposition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    onBack: () -> Unit,
    onOpenVideoPlayer: (mediaId: Long, isPrivate: Boolean) -> Unit,
    onOpenEditor: (mediaId: Long) -> Unit,
    onOpenInfo: (mediaId: Long, isPrivate: Boolean) -> Unit,
    onShare: (mediaId: Long) -> Unit,
    isPrivateContext: Boolean,
    viewModel: ViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(
        initialPage = uiState.currentIndex,
        pageCount = { uiState.items.size }
    )

    LaunchedEffect(pagerState.currentPage) {
        viewModel.onPageChanged(pagerState.currentPage)
    }

    // Section 5/46: only the private viewer instance gets FLAG_SECURE — the
    // normal viewer must remain screenshot/share-capable as expected.
    if (isPrivateContext) {
        SecureScreenEffect()
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (uiState.items.isNotEmpty()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val item = uiState.items[page]
                when (item.mediaType) {
                    MediaType.IMAGE -> ZoomableImage(
                        model = item.contentUri ?: item.filePath,
                        contentDescription = item.displayName,
                        onTap = { viewModel.toggleToolbar() },
                        onSwipeDownToClose = onBack
                    )
                    MediaType.VIDEO -> VideoPagerPage(
                        thumbnailModel = item.contentUri ?: item.filePath,
                        onTap = { viewModel.toggleToolbar() },
                        onPlayClick = { onOpenVideoPlayer(item.id, isPrivateContext) }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.isToolbarVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxWidth().statusBarsPadding()
        ) {
            CenterAlignedTopAppBar(
                title = { Text(uiState.counterLabel, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Black.copy(alpha = 0.5f))
            )
        }

        ViewerBottomToolbar(
            isVisible = uiState.isToolbarVisible,
            isFavorite = uiState.currentItem?.isFavorite ?: false,
            isPrivateContext = isPrivateContext,
            onShare = { uiState.currentItem?.let { onShare(it.id) } },
            onEdit = { uiState.currentItem?.let { onOpenEditor(it.id) } },
            onToggleFavorite = { viewModel.toggleFavorite() },
            onDelete = { viewModel.deleteCurrent(onDeleted = { /* pager auto-advances as the list shrinks */ }) },
            onMove = { /* wired to Album picker once Albums UI (Stage 6) exists */ },
            onAddToAlbum = { /* wired to Album picker once Albums UI (Stage 6) exists */ },
            onTogglePrivate = {
                if (isPrivateContext) {
                    viewModel.moveCurrentToNormal { }
                } else {
                    viewModel.moveCurrentToPrivate { }
                }
            },
            onInfo = { uiState.currentItem?.let { onOpenInfo(it.id, isPrivateContext) } },
            onMore = { /* additional overflow actions land here as they're built */ },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun VideoPagerPage(
    thumbnailModel: Any?,
    onTap: () -> Unit,
    onPlayClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = thumbnailModel,
            contentDescription = "Video thumbnail",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .clickable { onTap() }
        )
        IconButton(
            onClick = onPlayClick,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play video",
                tint = Color.White,
                modifier = Modifier.size(72.dp)
            )
        }
    }
}
