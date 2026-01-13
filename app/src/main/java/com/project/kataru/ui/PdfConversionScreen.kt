package com.project.kataru.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.project.kataru.tts.PdfConversionManager.ConversionState
import com.project.kataru.ui.theme.*

/*
 * PdfConversionDialog - Displays conversion progress when converting PDF to audio.
 * Shows different states: initializing, extracting text, generating audio, etc.
 * Provides cancel option and success/error feedback.
 */

@Composable
fun PdfConversionDialog(
    state: ConversionState,
    onDismiss: () -> Unit,
    onCancel: () -> Unit
) {
    val isComplete = state is ConversionState.Success || state is ConversionState.Error

    AlertDialog(
        onDismissRequest = { if (isComplete) onDismiss() },
        containerColor = SurfaceCard,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = when (state) {
                    is ConversionState.Success -> "Conversion Complete"
                    is ConversionState.Error -> "Conversion Failed"
                    else -> "Converting PDF"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status icon or spinner
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            when (state) {
                                is ConversionState.Success -> AccentPrimary.copy(alpha = 0.1f)
                                is ConversionState.Error -> Color.Red.copy(alpha = 0.1f)
                                else -> AccentPrimary.copy(alpha = 0.1f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when (state) {
                        is ConversionState.Success -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = AccentPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        is ConversionState.Error -> {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error",
                                tint = Color.Red,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        else -> {
                            // Spinning loader
                            val infiniteTransition = rememberInfiniteTransition(label = "spinner")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "rotation"
                            )
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(48.dp)
                                    .rotate(rotation),
                                color = AccentPrimary,
                                strokeWidth = 4.dp
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

                // Progress bar for states with progress
                when (state) {
                    is ConversionState.ExtractingText -> {
                        LinearProgressIndicator(
                            progress = { state.currentPage.toFloat() / state.totalPages.coerceAtLeast(1) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AccentPrimary,
                            trackColor = SurfaceGlass
                        )
                        Text(
                            text = "Page ${state.currentPage} of ${state.totalPages}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    is ConversionState.GeneratingAudio -> {
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = AccentPrimary,
                            trackColor = SurfaceGlass
                        )
                        Text(
                            text = "${state.progress}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
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
                            color = Color.Red.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            if (isComplete) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
            }
        },
        dismissButton = {
            if (!isComplete) {
                TextButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = TextMuted
                    )
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

private fun getStatusMessage(state: ConversionState): String {
    return when (state) {
        is ConversionState.Idle -> "Preparing..."
        is ConversionState.Initializing -> "Initializing TTS engine..."
        is ConversionState.ExtractingText -> "Extracting text from PDF..."
        is ConversionState.GeneratingAudio -> "Generating speech..."
        is ConversionState.WritingFile -> "Saving audio file..."
        is ConversionState.Success -> "Conversion successful!"
        is ConversionState.Error -> "An error occurred"
    }
}
