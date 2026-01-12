package com.project.kataru.ui

import androidx.compose.animation.core.*
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.project.kataru.R
import com.project.kataru.data.AudioBook
import com.project.kataru.ui.theme.*

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    book: AudioBook,
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    playbackSpeed: Float,
    volume: Float,
    skipForwardInterval: Long,
    skipBackwardInterval: Long,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Vinyl rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    
    // Glow pulse animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    var isLocked by remember { androidx.compose.runtime.mutableStateOf(false) }
    var showSpeedSheet by remember { androidx.compose.runtime.mutableStateOf(false) }
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState()

    if (showSpeedSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showSpeedSheet = false },
            sheetState = sheetState,
            containerColor = MiniPlayerBackground,
            contentColor = TextPrimary,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .background(
                            color = TextMuted.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Speed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "0.5x",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    
                    Slider(
                        value = playbackSpeed,
                        onValueChange = onPlaybackSpeedChange,
                        valueRange = 0.5f..2.5f,
                        steps = 7,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = AccentPrimary,
                            activeTrackColor = AccentPrimary,
                            inactiveTrackColor = SliderInactive
                        )
                    )
                    
                    Text(
                        text = "2.5x",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                
                Text(
                    text = "${String.format("%.2f", playbackSpeed)}x",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AccentPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }



    var showVolumeSheet by remember { androidx.compose.runtime.mutableStateOf(false) }
    val volumeSheetState = androidx.compose.material3.rememberModalBottomSheetState()

    if (showVolumeSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showVolumeSheet = false },
            sheetState = volumeSheetState,
            containerColor = MiniPlayerBackground,
            contentColor = TextPrimary,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .size(width = 32.dp, height = 4.dp)
                        .background(
                            color = TextMuted.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Volume",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    Slider(
                        value = volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = AccentPrimary,
                            activeTrackColor = AccentPrimary,
                            inactiveTrackColor = SliderInactive
                        )
                    )
                    
                    Text(
                        text = "${(volume * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentPrimary,
                        modifier = Modifier.width(48.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 32.dp, start = 24.dp, end = 24.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar with Lock, Speed and Volume Control
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lock Button - Premium styled
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(if (isLocked) 8.dp else 4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        brush = if (isLocked) {
                            Brush.linearGradient(
                                colors = listOf(GradientPurpleStart, GradientPurpleEnd)
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(SurfaceGlass, SurfaceCard)
                            )
                        }
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = if (isLocked) {
                                listOf(AccentPrimary.copy(alpha = 0.6f), AccentTertiary.copy(alpha = 0.3f))
                            } else {
                                listOf(CardBorder, CardBorder)
                            }
                        ),
                        shape = CircleShape
                    )
                    .clickable { isLocked = !isLocked },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = if (isLocked) "Unlock" else "Lock",
                    tint = if (isLocked) Color.White else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(if (isLocked) 0.4f else 1f)) {
                    // Volume Button - Premium styled
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(4.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(SurfaceGlass, SurfaceCard)
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(CardBorder, Color.Transparent)
                                ),
                                shape = CircleShape
                            )
                            .clickable(enabled = !isLocked) { showVolumeSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Volume",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Speed Button - Premium styled with gradient accent
                    Box(
                        modifier = Modifier
                            .shadow(4.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(SurfaceGlass, SurfaceCard)
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(AccentPrimary.copy(alpha = 0.4f), CardBorder)
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable(enabled = !isLocked) { showSpeedSheet = true }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            style = MaterialTheme.typography.labelLarge,
                            color = AccentPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Album Art
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Ambient glow behind album art
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .alpha(if (isPlaying) glowAlpha else 0f)
                    .blur(40.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AccentPrimary,
                                AccentTertiary.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            
            // Album art
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
                    .size(300.dp)
                    .shadow(16.dp, CircleShape)
                    .clip(CircleShape)
                    .border(
                        width = 4.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                AccentPrimary.copy(alpha = 0.6f),
                                Color.Transparent,
                                AccentTertiary.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isLocked) 0.4f else 1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title and Author
            Text(
                text = book.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = book.author,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Timeline with gradient track
            Column(modifier = Modifier.fillMaxWidth()) {
                // Custom styled slider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Track background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(SliderInactive)
                    ) {
                        // Progress fill with gradient
                        val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .height(6.dp)
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            AccentPrimary,
                                            GradientPurpleEnd
                                        )
                                    ),
                                    shape = RoundedCornerShape(3.dp)
                                )
                        )
                    }
                    
                    // Invisible slider for interaction
                    Slider(
                        value = currentPosition.toFloat(),
                        onValueChange = { onSeek(it.toLong()) },
                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                        enabled = !isLocked,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = AccentPrimary,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent,
                            disabledThumbColor = AccentPrimary,
                            disabledActiveTrackColor = Color.Transparent,
                            disabledInactiveTrackColor = Color.Transparent
                        ),
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .shadow(8.dp, CircleShape)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color.White,
                                                AccentPrimary
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .border(2.dp, AccentPrimary, CircleShape)
                            )
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerControlButton(
                    onClick = onSkipPrevious,
                    icon = Icons.Default.SkipPrevious,
                    contentDescription = "Previous",
                    size = 32.dp,
                    enabled = !isLocked
                )

                DynamicSkipButton(
                    onClick = onSkipBackward,
                    interval = skipBackwardInterval,
                    isForward = false,
                    enabled = !isLocked
                )

                // Main Play/Pause button
                PlayPauseButton(
                    isPlaying = isPlaying,
                    onClick = onPlayPauseClick,
                    enabled = !isLocked
                )

                DynamicSkipButton(
                    onClick = onSkipForward,
                    interval = skipForwardInterval,
                    isForward = true,
                    enabled = !isLocked
                )

                PlayerControlButton(
                    onClick = onSkipNext,
                    icon = Icons.Default.SkipNext,
                    contentDescription = "Next",
                    size = 32.dp,
                    enabled = !isLocked
                )
            }
        }
    }
}

@Composable
private fun DynamicSkipButton(
    onClick: () -> Unit,
    interval: Long,
    isForward: Boolean,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "controlScale"
    )

    Box(
        modifier = Modifier
            .size(56.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(SurfaceGlass.copy(alpha = 0.5f))
            .border(1.dp, CardBorder, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Circular arrow icon
            Icon(
                imageVector = Icons.Default.Replay,
                contentDescription = if (isForward) "Forward $interval" else "Rewind $interval",
                modifier = Modifier
                    .size(32.dp)
                    .graphicsLayer {
                        if (isForward) {
                            scaleX = -1f // Flip horizontally for forward
                        }
                    },
                tint = TextPrimary
            )
            
            // Overlay text
            Text(
                text = "${interval / 1000}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = TextPrimary,
                modifier = Modifier.padding(top = 4.dp) // Push text down slightly to center in the open area
            )
        }
    }
}

@Composable
private fun PlayerControlButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    size: androidx.compose.ui.unit.Dp,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "controlScale"
    )
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(SurfaceGlass.copy(alpha = 0.5f))
            .border(1.dp, CardBorder, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(size),
            tint = TextPrimary
        )
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "playScale"
    )
    
    // Glow animation when playing
    val infiniteTransition = rememberInfiniteTransition(label = "playGlow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    Box(
        modifier = Modifier.size(76.dp),
        contentAlignment = Alignment.Center
    ) {
        // Animated glow ring when playing
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .scale(glowScale)
                    .alpha(0.4f)
                    .blur(12.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AccentPrimary,
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }
        
        Box(
            modifier = Modifier
                .size(68.dp)
                .scale(scale)
                .shadow(16.dp, CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AccentPrimary,
                            GradientPurpleEnd
                        )
                    ),
                    shape = CircleShape
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                modifier = Modifier.size(36.dp),
                tint = Color.White
            )
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
