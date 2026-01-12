package com.project.kataru.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.project.kataru.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// ============= ANIMATION SPECS =============

// Bouncy spring for playful interactions
val BouncySpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

// Snappy spring for quick responses
val SnappySpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium
)

// Gentle spring for subtle animations
val GentleSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessLow
)

// Smooth easing for transitions
val SmoothEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

// ============= MODIFIER EXTENSIONS =============

/**
 * Adds a pulsing glow effect - perfect for playing state
 */
@Composable
fun Modifier.pulsingGlow(
    enabled: Boolean,
    glowColor: Color = GlowPurple,
    minAlpha: Float = 0.3f,
    maxAlpha: Float = 0.8f
): Modifier {
    if (!enabled) return this
    
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val alpha by infiniteTransition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    
    return this.graphicsLayer {
        shadowElevation = 24f
        ambientShadowColor = glowColor.copy(alpha = alpha)
        spotShadowColor = glowColor.copy(alpha = alpha)
    }
}

/**
 * Adds a scale press effect for interactive elements
 */
fun Modifier.pressScale(pressed: Boolean): Modifier = composed {
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "pressScale"
    )
    this.scale(scale)
}

/**
 * Staggered item animation for lists
 */
@Composable
fun Modifier.staggeredAnimation(
    index: Int,
    visible: Boolean,
    delayPerItem: Long = 50L
): Modifier {
    var animationTriggered by remember { mutableStateOf(false) }
    
    LaunchedEffect(visible) {
        if (visible && !animationTriggered) {
            delay(index * delayPerItem)
            animationTriggered = true
        }
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "staggerAlpha"
    )
    
    val offsetY by animateFloatAsState(
        targetValue = if (animationTriggered) 0f else 30f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "staggerOffset"
    )
    
    return this
        .alpha(alpha)
        .offset { IntOffset(0, offsetY.roundToInt()) }
}

/**
 * Vinyl spinning animation for player
 */
@Composable
fun Modifier.vinylSpin(isPlaying: Boolean): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinylRotation"
    )
    
    val currentRotation by animateFloatAsState(
        targetValue = if (isPlaying) rotation else 0f,
        animationSpec = tween(300),
        label = "vinylState"
    )
    
    return if (isPlaying) {
        this.graphicsLayer { rotationZ = rotation }
    } else {
        this
    }
}

/**
 * Shimmer loading effect
 */
@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -300f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    
    return this.background(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.1f),
                Color.Transparent
            ),
            startX = translateX,
            endX = translateX + 200f
        )
    )
}

/**
 * Floating animation for FABs and special elements
 */
@Composable
fun Modifier.floatingAnimation(): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatOffset"
    )
    
    return this.offset { IntOffset(0, offsetY.roundToInt()) }
}

/**
 * Scale and fade entrance animation
 */
@Composable
fun Modifier.scaleInAnimation(visible: Boolean): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scaleIn"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(200),
        label = "fadeIn"
    )
    
    return this.scale(scale).alpha(alpha)
}

// ============= GLASSMORPHISM =============

/**
 * Glassmorphic surface background
 */
@Composable
fun GlassmorphicSurface(
    modifier: Modifier = Modifier,
    blur: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.03f)
                    )
                )
            )
            .background(SurfaceGlass.copy(alpha = 0.7f))
    ) {
        content()
    }
}

// ============= ANIMATED GRADIENT BACKGROUNDS =============

@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    
    val color1 by infiniteTransition.animateColor(
        initialValue = PurpleGradientStart,
        targetValue = GradientPurpleEnd.copy(alpha = 0.5f),
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradColor1"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color1,
                        PurpleGradientMid,
                        PurpleGradientEnd
                    )
                )
            )
    )
}

// ============= BUTTON BOUNCE =============

@Composable
fun Modifier.bounceClick(onClick: () -> Unit): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "bounce"
    )
    
    this
        .scale(scale)
        .graphicsLayer {
            this.transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
        }
}
