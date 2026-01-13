package com.project.kataru.ui

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalView

/**
 * Performs a haptic feedback tick (CLOCK_TICK) which feels premium and subtle.
 */
@Composable
fun rememberHapticFeedback(): () -> Unit {
    val view = LocalView.current
    return remember(view) {
        {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }
}

/**
 * Extension modifier to add haptic feedback to clickable.
 * Note: This replaces the standard clickable, so use carefully if you need custom interaction sources.
 */
fun Modifier.hapticClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val haptic = rememberHapticFeedback()
    val interactionSource = remember { MutableInteractionSource() }
    
    this.clickable(
        interactionSource = interactionSource,
        indication = androidx.compose.foundation.LocalIndication.current,
        enabled = enabled,
        onClick = {
            haptic()
            onClick()
        }
    )
}

/**
 * Extension modifier to add haptic feedback to clickable without indication (ripple).
 */
fun Modifier.hapticClickableNoIndication(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onClick: () -> Unit
): Modifier = composed {
    val haptic = rememberHapticFeedback()
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    
    this.clickable(
        interactionSource = actualInteractionSource,
        indication = null,
        enabled = enabled,
        onClick = {
            haptic()
            onClick()
        }
    )
}
