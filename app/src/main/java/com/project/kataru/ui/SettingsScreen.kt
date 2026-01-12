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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.project.kataru.data.SettingsManager
import com.project.kataru.ui.theme.*

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onRescanClick: () -> Unit
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
        modifier = Modifier.fillMaxSize()
    ) {
        // Premium Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            AccentPrimary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, bottom = 16.dp, top = 28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedSettingsBackButton(onClick = onBackClick)
                
                Box {
                    Text(
                        text = "Settings",
                        modifier = Modifier
                            .padding(start = 4.dp, top = 6.dp)
                            .alpha(0.5f)
                            .blur(8.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentPrimary
                    )
                    Text(
                        text = "Settings",
                        modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Source Folder
            SettingsSection(title = "Source Folder") {
                SettingsCard {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = if (currentSourceUri != null) {
                                "📁 " + currentSourceUri.lastPathSegment?.replace("primary:", "")?.take(40)
                            } else {
                                "Using default device scan"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (currentSourceUri != null) AccentSecondary else TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        AnimatedSettingsButton(
                            text = "Select Source Folder",
                            icon = Icons.Rounded.FolderOpen,
                            onClick = { folderPickerLauncher.launch(null) },
                            isPrimary = true
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
            
            // Section: Library
            SettingsSection(title = "Library") {
                SettingsCard {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Scan your device for audiobooks",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        AnimatedSettingsButton(
                            text = "Rescan Library",
                            icon = Icons.Rounded.Refresh,
                            onClick = onRescanClick,
                            isPrimary = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // App Version
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Kataru Audiobook Player",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Version 1.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
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
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.White.copy(alpha = 0.02f)
                    )
                )
            )
            .background(CardBackground)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.1f),
                        Color.White.copy(alpha = 0.02f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
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
    isDestructive: Boolean = false
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
            colors = listOf(AccentPrimary, GradientPurpleEnd)
        )
        isDestructive -> Brush.linearGradient(
            colors = listOf(AccentTertiary.copy(alpha = 0.8f), AccentTertiary.copy(alpha = 0.6f))
        )
        else -> Brush.linearGradient(
            colors = listOf(SurfaceGlass, SurfaceGlass)
        )
    }
    
    val contentColor = when {
        isPrimary || isDestructive -> Color.White
        else -> TextPrimary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .border(
                width = if (isPrimary || isDestructive) 0.dp else 1.dp,
                color = if (isPrimary || isDestructive) Color.Transparent else CardBorder,
                shape = RoundedCornerShape(14.dp)
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
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}
