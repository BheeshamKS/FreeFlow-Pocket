package com.bheeshamks.freeflow.ime

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.bheeshamks.freeflow.core.data.model.BloomAnimationStyle
import com.bheeshamks.freeflow.core.data.model.BloomColorPreset
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * In-flight bloom touch particle representing a touch point and animated progress (0f..1f).
 */
@Stable
class BloomParticle(
    val origin: Offset,
    val initialColor: Color,
    val animationStyle: BloomAnimationStyle,
    val maxRadiusPx: Float,
    val durationMs: Int
) {
    val progress = Animatable(0f)

    suspend fun run() {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMs)
        )
    }
}

/**
 * Controller holding active Keys Cafe bloom particles.
 */
@Stable
class KeysCafeController {
    val activeParticles = mutableStateListOf<BloomParticle>()

    fun triggerBloom(
        offset: Offset,
        colorPreset: BloomColorPreset,
        style: BloomAnimationStyle,
        maxRadiusPx: Float,
        durationMs: Int
    ) {
        val color = Color(colorPreset.primaryColor)
        val particle = BloomParticle(
            origin = offset,
            initialColor = color,
            animationStyle = style,
            maxRadiusPx = maxRadiusPx,
            durationMs = durationMs
        )
        activeParticles.add(particle)
    }
}

@Composable
fun rememberKeysCafeController(): KeysCafeController {
    return remember { KeysCafeController() }
}

/**
 * High-performance hardware-accelerated canvas modifier rendering multi-layered
 * concentric radial bloom effects behind the keyboard without triggering key grid recomposition.
 */
@Composable
fun KeysCafeBloomCanvas(
    controller: KeysCafeController,
    modifier: Modifier = Modifier
) {
    // Collect active particles and drive their animations
    val particles = controller.activeParticles

    particles.forEach { particle ->
        LaunchedEffect(particle) {
            particle.run()
            particles.remove(particle)
        }
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                for (i in 0 until particles.size) {
                    val p = particles.getOrNull(i) ?: continue
                    val t = p.progress.value
                    if (t >= 1f) continue

                    drawBloomEffect(p, t)
                }
            }
    )
}

private fun DrawScope.drawBloomEffect(particle: BloomParticle, progress: Float) {
    val alpha = (1f - progress).coerceIn(0f, 1f)
    val currentRadius = particle.maxRadiusPx * (0.25f + 0.75f * progress)

    when (particle.animationStyle) {
        BloomAnimationStyle.RADIAL_BLOOM -> {
            // Samsung Keys Cafe signature: Concentric soft multi-ring gradient bloom
            val gradient = Brush.radialGradient(
                colors = listOf(
                    particle.initialColor.copy(alpha = 0.55f * alpha),
                    particle.initialColor.copy(alpha = 0.35f * alpha),
                    particle.initialColor.copy(alpha = 0.15f * alpha),
                    Color.Transparent
                ),
                center = particle.origin,
                radius = currentRadius
            )
            drawCircle(
                brush = gradient,
                radius = currentRadius,
                center = particle.origin
            )

            // Inner bright core
            val innerRadius = currentRadius * 0.45f
            drawCircle(
                color = particle.initialColor.copy(alpha = 0.4f * alpha),
                radius = innerRadius,
                center = particle.origin
            )
        }
        BloomAnimationStyle.SONIC_RIPPLE -> {
            // Concentric shockwave strokes expanding outward
            val ringCount = 3
            for (r in 0 until ringCount) {
                val ringOffset = (progress + (r * 0.2f)) % 1f
                val ringRadius = particle.maxRadiusPx * ringOffset
                val ringAlpha = (1f - ringOffset) * alpha * 0.7f
                drawCircle(
                    color = particle.initialColor.copy(alpha = ringAlpha),
                    radius = ringRadius,
                    center = particle.origin,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f * (1f - ringOffset))
                )
            }
        }
        BloomAnimationStyle.NEON_PULSE -> {
            // Intense bright keycap bloom with sharp falloff
            val glowRadius = currentRadius * 0.7f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.8f * alpha),
                        particle.initialColor.copy(alpha = 0.6f * alpha),
                        Color.Transparent
                    ),
                    center = particle.origin,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = particle.origin
            )
        }
        BloomAnimationStyle.MINIMALIST -> {
            // Subtle, understated soft halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        particle.initialColor.copy(alpha = 0.25f * alpha),
                        Color.Transparent
                    ),
                    center = particle.origin,
                    radius = currentRadius * 0.6f
                ),
                radius = currentRadius * 0.6f,
                center = particle.origin
            )
        }
    }
}
