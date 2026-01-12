package com.project.kataru.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.kataru.data.SettingsManager
import com.project.kataru.ui.theme.*

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onRescanClick: () -> Unit,
    accentColor: Color,
    onAccentColorChange: (Color) -> Unit,
    skipForwardInterval: Long,
    onSkipForwardChange: (Long) -> Unit,
    skipBackwardInterval: Long,
    onSkipBackwardChange: (Long) -> Unit,
    onClearHistory: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = SettingsManager(context)
    val currentSourceUri = settingsManager.sourceFolderUri

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            settingsManager.sourceFolderUri = uri
            onRescanClick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Premium Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, bottom = 16.dp, top = 28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedSettingsBackButton(onClick = onBackClick)
                
                Text(
                    text = "Settings",
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Library Section
            item {
                SettingsSection(title = "Library") {
                    SettingsCard(accentColor = accentColor) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SettingsActionItem(
                                icon = Icons.Rounded.Refresh,
                                title = "Rescan Library",
                                subtitle = "Scan device for new audiobooks",
                                onClick = onRescanClick,
                                accentColor = accentColor
                            )
                            
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = CardBorder
                            )
                            
                            SettingsActionItem(
                                icon = Icons.Rounded.RestartAlt, // Using RestartAlt as Delete/Clear icon
                                title = "Clear History",
                                subtitle = "Remove all playback progress",
                                onClick = onClearHistory,
                                accentColor = accentColor,
                                isDestructive = true
                            )
                        }
                    }
                }
            }

            // Appearance Section
            item {
                SettingsSection(title = "Appearance") {
                    SettingsCard(accentColor = accentColor) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Accent Color",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val colors = listOf(
                                    AccentPrimary,
                                    Color(0xFF00E5FF), // Cyan
                                    Color(0xFF76FF03), // Lime
                                    Color(0xFFFF1744), // Red
                                    Color(0xFFFF9100)  // Orange
                                )
                                
                                colors.forEach { color ->
                                    ColorPickerItem(
                                        color = color,
                                        isSelected = color == accentColor,
                                        onClick = { onAccentColorChange(color) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Playback Section
            item {
                SettingsSection(title = "Playback") {
                    SettingsCard(accentColor = accentColor) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Skip Forward",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val intervals = listOf(10000L, 30000L, 60000L)
                                intervals.forEach { interval ->
                                    FilterChip(
                                        selected = skipForwardInterval == interval,
                                        onClick = { onSkipForwardChange(interval) },
                                        label = "${interval / 1000}s",
                                        accentColor = accentColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Text(
                                text = "Skip Backward",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val intervals = listOf(10000L, 30000L, 60000L)
                                intervals.forEach { interval ->
                                    FilterChip(
                                        selected = skipBackwardInterval == interval,
                                        onClick = { onSkipBackwardChange(interval) },
                                        label = "${interval / 1000}s",
                                        accentColor = accentColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Source Folder Section
            item {
                SettingsSection(title = "Source Folder") {
                    SettingsCard(accentColor = accentColor) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = if (currentSourceUri != null) {
                                    "📁 " + currentSourceUri.lastPathSegment?.replace("primary:", "")?.take(40)
                                } else {
                                    "Using default device scan"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (currentSourceUri != null) accentColor else TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            AnimatedSettingsButton(
                                text = "Select Source Folder",
                                icon = Icons.Rounded.FolderOpen,
                                onClick = { folderPickerLauncher.launch(null) },
                                isPrimary = true,
                                accentColor = accentColor
                            )
                            
                            if (currentSourceUri != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                AnimatedSettingsButton(
                                    text = "Reset to Default",
                                    icon = Icons.Rounded.RestartAlt,
                                    onClick = {
                                        settingsManager.clearSourceFolder()
                                        onRescanClick()
                                    },
                                    isDestructive = true
                                )
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Kataru v1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) accentColor.copy(alpha = 0.25f) else SurfaceGlass.copy(alpha = 0.3f)
            )
            .border(
                width = 1.dp,
                color = if (selected) accentColor.copy(alpha = 0.8f) else CardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) accentColor else TextSecondary
        )
    }
}

@Composable
private fun AnimatedSettingsBackButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "backScale"
    )
    
    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.scale(scale)
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back",
            tint = TextPrimary
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        content()
    }
}

@Composable
private fun SettingsCard(
    accentColor: Color = AccentPrimary,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceGlass.copy(alpha = 0.6f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.15f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        content()
    }
}

@Composable
private fun AnimatedSettingsButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isPrimary: Boolean = false,
    isDestructive: Boolean = false,
    accentColor: Color = AccentPrimary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "buttonScale"
    )
    
    val backgroundColor = when {
        isPrimary -> Brush.linearGradient(
            colors = listOf(accentColor, accentColor.copy(alpha = 0.8f))
        )
        isDestructive -> Brush.linearGradient(
            colors = listOf(AccentTertiary.copy(alpha = 0.8f), AccentTertiary.copy(alpha = 0.6f))
        )
        else -> Brush.linearGradient(
            colors = listOf(SurfaceGlass.copy(alpha = 0.5f), SurfaceGlass.copy(alpha = 0.3f))
        )
    }
    
    val contentColor = when {
        isPrimary || isDestructive -> Color.White
        else -> TextPrimary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = if (isPrimary || isDestructive) 0.dp else 1.dp,
                color = if (isPrimary || isDestructive) Color.Transparent else CardBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    accentColor: Color,
    isDestructive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "actionItemScale"
    )

    val contentColor = if (isDestructive) AccentTertiary else TextPrimary
    val iconColor = if (isDestructive) AccentTertiary else accentColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.size(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ColorPickerItem(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) Color.White else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}
