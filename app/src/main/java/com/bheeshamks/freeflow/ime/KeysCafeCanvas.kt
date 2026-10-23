package com.bheeshamks.freeflow.ime

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.ui.graphics.drawscope.Stroke
import com.bheeshamks.freeflow.core.data.model.BloomAnimationStyle
import com.bheeshamks.freeflow.core.data.model.BloomColorPreset

/**
 * Signature Keys Cafe animation easing: immediate tactile impulse with smooth, natural deceleration.
 */
val KeysCafeBloomEasing = CubicBezierEasing(0.08f, 0.82f, 0.17f, 1.0f)

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
            animationSpec = tween(
                durationMillis = durationMs,
                easing = KeysCafeBloomEasing
            )
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
 * Maintains a strict 120fps frame-rate target by delegating render updates directly to hardware draw calls.
 */
@Composable
fun KeysCafeBloomCanvas(
    controller: KeysCafeController,
    modifier: Modifier = Modifier
) {
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
                val size = particles.size
                for (i in 0 until size) {
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
    val currentRadius = particle.maxRadiusPx * (0.20f + 0.80f * progress)

    when (particle.animationStyle) {
        BloomAnimationStyle.RADIAL_BLOOM -> {
            // Layer 1: Outer concentric shockwave ring
            val outerRingRadius = currentRadius * 0.95f
            drawCircle(
                color = particle.initialColor.copy(alpha = 0.22f * alpha),
                radius = outerRingRadius,
                center = particle.origin,
                style = Stroke(width = (4f * (1f - progress)).coerceAtLeast(1f))
            )

            // Layer 2: Middle concentric soft bloom gradient
            val gradient = Brush.radialGradient(
                colors = listOf(
                    particle.initialColor.copy(alpha = 0.65f * alpha),
                    particle.initialColor.copy(alpha = 0.40f * alpha),
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

            // Layer 3: Concentric intense inner tactile core
            val innerCoreRadius = currentRadius * 0.35f
            drawCircle(
                color = particle.initialColor.copy(alpha = 0.50f * alpha),
                radius = innerCoreRadius,
                center = particle.origin
            )
        }
        BloomAnimationStyle.SONIC_RIPPLE -> {
            // Concentric shockwave strokes expanding outward
            val ringCount = 3
            for (r in 0 until ringCount) {
                val ringOffset = (progress + (r * 0.22f)) % 1f
                val ringRadius = particle.maxRadiusPx * ringOffset
                val ringAlpha = (1f - ringOffset) * alpha * 0.75f
                drawCircle(
                    color = particle.initialColor.copy(alpha = ringAlpha),
                    radius = ringRadius,
                    center = particle.origin,
                    style = Stroke(width = (3.5f * (1f - ringOffset)).coerceAtLeast(1f))
                )
            }
        }
        BloomAnimationStyle.NEON_PULSE -> {
            // Intense bright keycap bloom with sharp radial falloff
            val glowRadius = currentRadius * 0.75f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f * alpha),
                        particle.initialColor.copy(alpha = 0.65f * alpha),
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
            val haloRadius = currentRadius * 0.55f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        particle.initialColor.copy(alpha = 0.30f * alpha),
                        Color.Transparent
                    ),
                    center = particle.origin,
                    radius = haloRadius
                ),
                radius = haloRadius,
                center = particle.origin
            )
        }
    }
}
