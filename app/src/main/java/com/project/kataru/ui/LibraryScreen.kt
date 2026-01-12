package com.project.kataru.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.project.kataru.R
import com.project.kataru.data.AudioBook
import com.project.kataru.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    audioBooks: List<AudioBook>,
    isRefreshing: Boolean,
    onBookClick: (AudioBook) -> Unit,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    activeBook: AudioBook?,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    isGridView: Boolean,
    onToggleView: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onMiniPlayerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }

    // Trigger content animation
    LaunchedEffect(Unit) {
        delay(100)
        contentVisible = true
    }

    val filteredBooks = remember(audioBooks, searchQuery) {
        if (searchQuery.isBlank()) {
            audioBooks
        } else {
            audioBooks.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.author.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Premium Header with gradient accent
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, bottom = 12.dp, top = 28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Animated title
                Text(
                    text = "Library",
                    modifier = Modifier.padding(top = 6.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Animated icon buttons
                AnimatedIconButton(
                    onClick = onHistoryClick,
                    icon = Icons.Default.History,
                    contentDescription = "History"
                )
                AnimatedIconButton(
                    onClick = onToggleView,
                    icon = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                    contentDescription = "Toggle View"
                )
                AnimatedIconButton(
                    onClick = onSettingsClick,
                    icon = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        }

        // Enhanced Search Bar with glow effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Glow effect when focused
            if (isSearchFocused) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .blur(16.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    AccentSecondary.copy(alpha = 0.2f)
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }
            
            androidx.compose.material3.OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { 
                    Text(
                        "Search your library...",
                        color = TextMuted
                    ) 
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isSearchFocused) MaterialTheme.colorScheme.primary else TextMuted
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextMuted
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceGlass.copy(alpha = 0.8f),
                    unfocusedContainerColor = SurfaceCard.copy(alpha = 0.6f),
                    disabledContainerColor = SurfaceCard,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedIndicatorColor = CardBorder,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
        }

        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f)
        ) {
            when {
                audioBooks.isEmpty() -> {
                    EmptyLibraryState(onSettingsClick = onSettingsClick)
                }
                filteredBooks.isEmpty() -> {
                    NoResultsState(searchQuery = searchQuery)
                }
                isGridView -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(filteredBooks, key = { _, book -> book.id }) { index, book ->
                            AnimatedBookGridItem(
                                book = book,
                                index = index,
                                visible = contentVisible,
                                onClick = { onBookClick(book) }
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        itemsIndexed(filteredBooks, key = { _, book -> book.id }) { index, book ->
                            AnimatedBookListItem(
                                book = book,
                                index = index,
                                visible = contentVisible,
                                onClick = { onBookClick(book) }
                            )
                        }
                    }
                }
            }
        }

        // Mini Player with slide animation
        AnimatedVisibility(
            visible = activeBook != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            if (activeBook != null) {
                MiniPlayer(
                    book = activeBook,
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    onPlayPauseClick = onPlayPause,
                    onNextClick = onNext,
                    onPrevClick = onPrev,
                    onClick = onMiniPlayerClick
                )
            }
        }
    }
}

@Composable
private fun AnimatedIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "iconScale"
    )
    
    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.scale(scale)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TextSecondary
        )
    }
}

@Composable
private fun AnimatedBookListItem(
    book: AudioBook,
    index: Int,
    visible: Boolean,
    onClick: () -> Unit
) {
    var animationTriggered by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    LaunchedEffect(visible) {
        if (visible && !animationTriggered) {
            delay(index * 40L)
            animationTriggered = true
        }
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    
    val offsetX by animateFloatAsState(
        targetValue = if (animationTriggered) 0f else -50f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offset"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .graphicsLayer {
                translationX = offsetX
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art with subtle shadow
                Box {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(book.albumArtUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album Art",
                        placeholder = painterResource(R.drawable.ic_launcher_foreground),
                        error = painterResource(R.drawable.ic_launcher_foreground),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .shadow(8.dp, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceGlass)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedBookGridItem(
    book: AudioBook,
    index: Int,
    visible: Boolean,
    onClick: () -> Unit
) {
    var animationTriggered by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    LaunchedEffect(visible) {
        if (visible && !animationTriggered) {
            delay(index * 50L)
            animationTriggered = true
        }
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "alpha"
    )
    
    val offsetY by animateFloatAsState(
        targetValue = if (animationTriggered) 0f else 40f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "offset"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .graphicsLayer {
                translationY = offsetY
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.02f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Album art with gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(book.albumArtUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album Art",
                        placeholder = painterResource(R.drawable.ic_launcher_foreground),
                        error = painterResource(R.drawable.ic_launcher_foreground),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(12.dp, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                    )
                    

                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(onSettingsClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated sparkle icon
            val infiniteTransition = rememberInfiniteTransition(label = "sparkle")
            val rotation by infiniteTransition.animateFloat(
                initialValue = -10f,
                targetValue = 10f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "sparkleRotation"
            )
            
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer { rotationZ = rotation },
                tint = AccentPrimary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Your Library Awaits",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Add audiobooks to get started.\nSelect a source folder in settings.",
                textAlign = TextAlign.Center,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            androidx.compose.material3.Button(
                onClick = onSettingsClick,
                shape = RoundedCornerShape(16.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = AccentPrimary
                )
            ) {
                Text(
                    text = "Select Source Folder",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun NoResultsState(searchQuery: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "🔍",
                style = MaterialTheme.typography.displayMedium
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No results found",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Nothing matches \"$searchQuery\"",
                textAlign = TextAlign.Center,
                color = TextSecondary
            )
        }
    }
}
