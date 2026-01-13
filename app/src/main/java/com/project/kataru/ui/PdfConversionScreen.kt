package com.project.kataru.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.kataru.tts.PdfConversionManager.ConversionState
import com.project.kataru.ui.theme.*

/*
 * PdfConversionDialog - Displays conversion progress when converting PDF to audio.
 * Premium glassmorphic design with smooth animations matching the app theme.
 * Shows different states: initializing, extracting text, generating audio, etc.
 */

@Composable
fun PdfConversionDialog(
    state: ConversionState,
    onDismiss: () -> Unit,
    onCancel: () -> Unit,
    accentColor: Color = AccentPrimary
) {
    val isComplete = state is ConversionState.Success || state is ConversionState.Error

    AlertDialog(
        onDismissRequest = { if (isComplete) onDismiss() },
        containerColor = Color.Transparent,
        shape = RoundedCornerShape(28.dp),
        title = null,
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1A1A1F),
                                Color(0xFF121215)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.White.copy(alpha = 0.04f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Text(
                        text = when (state) {
                            is ConversionState.Success -> "Conversion Complete"
                            is ConversionState.Error -> "Conversion Failed"
                            else -> "Converting PDF"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 0.5.sp
                    )

                    // Status icon with glow
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = when (state) {
                                        is ConversionState.Success -> listOf(
                                            accentColor.copy(alpha = 0.2f),
                                            accentColor.copy(alpha = 0.05f)
                                        )
                                        is ConversionState.Error -> listOf(
                                            Color(0xFFFF5252).copy(alpha = 0.2f),
                                            Color(0xFFFF5252).copy(alpha = 0.05f)
                                        )
                                        else -> listOf(
                                            accentColor.copy(alpha = 0.15f),
                                            accentColor.copy(alpha = 0.05f)
                                        )
                                    }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when (state) {
                            is ConversionState.Success -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = accentColor,
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            is ConversionState.Error -> {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(44.dp)
                                )
                            }
                            else -> {
                                // Smooth spinning loader
                                val infiniteTransition = rememberInfiniteTransition(label = "spinner")
                                val rotation by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "rotation"
                                )
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .rotate(rotation),
                                    color = accentColor,
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    }

                    // Status message
                    Text(
                        text = getStatusMessage(state),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )

                    // Progress bar for applicable states
                    when (state) {
                        is ConversionState.ExtractingText -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { state.currentPage.toFloat() / state.totalPages.coerceAtLeast(1) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = accentColor,
                                    trackColor = Color.White.copy(alpha = 0.1f)
                                )
                                Text(
                                    text = "Page ${state.currentPage} of ${state.totalPages}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                        is ConversionState.GeneratingAudio -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = { state.progress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = accentColor,
                                    trackColor = Color.White.copy(alpha = 0.1f)
                                )
                                Text(
                                    text = "${state.progress}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                        is ConversionState.Success -> {
                            Text(
                                text = "\"${state.fileName}\" has been added to your library",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                        is ConversionState.Error -> {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFF8A80),
                                textAlign = TextAlign.Center
                            )
                        }
                        else -> {}
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Action buttons
                    if (isComplete) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "Done",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        TextButton(
                            onClick = onCancel,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = TextMuted
                            )
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

private fun getStatusMessage(state: ConversionState): String {
    return when (state) {
        is ConversionState.Idle -> "Preparing..."
        is ConversionState.CheckingModel -> "Initializing TTS..."
        is ConversionState.DownloadingModel -> "Preparing voice..."
        is ConversionState.ExtractingText -> "Extracting text from PDF..."
        is ConversionState.GeneratingAudio -> "Generating speech..."
        is ConversionState.WritingFile -> "Saving audio file..."
        is ConversionState.Success -> "Conversion successful!"
        is ConversionState.Error -> "An error occurred"
    }
}
