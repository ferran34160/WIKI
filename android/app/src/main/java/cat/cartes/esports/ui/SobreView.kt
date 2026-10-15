package cat.cartes.esports.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.cartes.esports.R
import cat.cartes.esports.game.Sobres
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Or = Color(0xFFFFD54A)

private fun estrella(centre: Offset, radiExt: Float): Path {
    val radiInt = radiExt * 0.45f
    return Path().apply {
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) radiExt else radiInt
            val a = -PI / 2 + i * PI / 5
            val p = Offset(centre.x + (r * cos(a)).toFloat(), centre.y + (r * sin(a)).toFloat())
            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
        }
        close()
    }
}

/** Una carta en miniatura (inclinada) per al ventall del sobre. */
private fun DrawScope.miniCarta(cx: Float, cy: Float, angle: Float, dalt: Color, baix: Color, s: Float) {
    rotate(angle, pivot = Offset(cx, cy)) {
        val tl = Offset(cx - 38 * s, cy - 56 * s)
        val mida = Size(76 * s, 112 * s)
        drawRoundRect(
            Brush.linearGradient(listOf(dalt, baix), start = tl, end = tl + Offset(mida.width, mida.height)),
            tl, mida, CornerRadius(9 * s),
        )
        drawRoundRect(Color.White.copy(alpha = 0.7f), tl, mida, CornerRadius(9 * s), style = Stroke(2 * s))
        drawRoundRect(
            Color.White.copy(alpha = 0.22f), Offset(cx - 30 * s, cy - 48 * s), Size(60 * s, 58 * s), CornerRadius(5 * s),
        )
        drawRoundRect(
            Color.Black.copy(alpha = 0.25f), Offset(cx - 30 * s, cy + 16 * s), Size(44 * s, 7 * s), CornerRadius(3.5f * s),
        )
        drawRoundRect(
            Color.Black.copy(alpha = 0.18f), Offset(cx - 30 * s, cy + 28 * s), Size(30 * s, 5 * s), CornerRadius(2.5f * s),
        )
    }
}

/**
 * Sobre de cartes dibuixat: paper metàl·lic fosc amb vores dentades i ratllat de segellat, raigs daurats,
 * un ventall de cartes i un emblema. Fa un halo que pulsa al voltant.
 */
@Composable
fun SobreView(modifier: Modifier = Modifier) {
    val transicio = rememberInfiniteTransition(label = "sobre")
    val halo by transicio.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "halo",
    )
    BoxWithConstraints(modifier.aspectRatio(0.58f)) {
        val ample = maxWidth.value
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val s = w / 290f

            drawCircle(
                Brush.radialGradient(
                    listOf(Color(0xFFFFB300).copy(alpha = halo), Color.Transparent),
                    center = center,
                    radius = w * 0.95f,
                ),
                radius = w * 0.95f,
                center = center,
            )

            val dent = h * 0.022f
            val dents = 16
            val pas = w / dents
            val forma = Path().apply {
                moveTo(0f, dent)
                for (i in 0 until dents) {
                    lineTo(pas * (i + 0.5f), 0f)
                    lineTo(pas * (i + 1), dent)
                }
                lineTo(w, h - dent)
                for (i in dents downTo 1) {
                    lineTo(pas * (i - 0.5f), h)
                    lineTo(pas * (i - 1), h - dent)
                }
                close()
            }
            drawPath(
                forma,
                Brush.linearGradient(
                    0f to Color(0xFF2A1166), 0.45f to Color(0xFF6A35E8), 1f to Color(0xFF170845),
                    start = Offset.Zero, end = Offset(w, h),
                ),
            )

            clipPath(forma) {
                // Raigs daurats des del centre.
                val c0 = Offset(w / 2, h * 0.5f)
                for (i in 0 until 18) {
                    val a0 = (i * 20) * PI / 180
                    val a1 = a0 + 10 * PI / 180
                    val raigs = Path().apply {
                        moveTo(c0.x, c0.y)
                        lineTo(c0.x + h * cos(a0).toFloat(), c0.y + h * sin(a0).toFloat())
                        lineTo(c0.x + h * cos(a1).toFloat(), c0.y + h * sin(a1).toFloat())
                        close()
                    }
                    drawPath(raigs, Or.copy(alpha = 0.07f))
                }
                // Reflex hologràfic i brillantor del paper.
                drawRect(
                    Brush.linearGradient(
                        0f to Color(0xFFFF6AD5).copy(alpha = 0f),
                        0.3f to Color(0xFF6AFFD8).copy(alpha = 0.18f),
                        0.6f to Color(0xFFFFE26A).copy(alpha = 0.18f),
                        1f to Color(0xFF8A6AFF).copy(alpha = 0f),
                        start = Offset.Zero, end = Offset(w, h),
                    ),
                )
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.32f), Color.Transparent),
                        start = Offset(0f, h * 0.2f), end = Offset(w, h * 0.6f),
                    ),
                )
                // Ratllat de segellat a dalt i a baix.
                val banda = h * 0.07f
                for (y0 in listOf(0f, h - banda)) {
                    var x = 0f
                    var i = 0
                    while (x < w) {
                        drawLine(
                            Color.White.copy(alpha = if (i % 2 == 1) 0.16f else 0.03f),
                            Offset(x, y0), Offset(x, y0 + banda), strokeWidth = 2 * s,
                        )
                        x += 4 * s
                        i++
                    }
                }
                // Marc daurat.
                drawRoundRect(
                    Or.copy(alpha = 0.55f), Offset(16 * s, h * 0.09f), Size(w - 32 * s, h * 0.82f),
                    CornerRadius(14 * s), style = Stroke(1.5f * s),
                )
                // Ventall de cartes.
                miniCarta(w / 2 - 58 * s, h * 0.33f, -16f, Color(0xFF9CD0FF), Color(0xFF1E6FCC), s)
                miniCarta(w / 2 + 58 * s, h * 0.33f, 16f, Color(0xFFFFF1A6), Color(0xFFFF9A3C), s)
                miniCarta(w / 2, h * 0.31f, 0f, Color(0xFFE9C6FF), Color(0xFF8E3FD0), s)
                // Emblema.
                val e = Offset(w / 2, h * 0.62f)
                drawCircle(Color.Black.copy(alpha = 0.3f), 48 * s, e)
                drawCircle(
                    Brush.radialGradient(
                        0f to Color(0xFFFFF0A8), 0.6f to Color(0xFFFFB300), 1f to Color(0xFFC97800),
                        center = e + Offset(-8.8f * s, -13.2f * s), radius = 70.4f * s,
                    ),
                    44 * s, e,
                )
                drawCircle(Color(0xFFFFF6C8), 44 * s, e, style = Stroke(2.5f * s))
                drawCircle(Color(0xFF7A4A00).copy(alpha = 0.45f), 35 * s, e, style = Stroke(2 * s))
                drawPath(estrella(e, 29 * s), Color(0xFF3A1B8F))
            }
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = maxHeight * 0.115f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.sobre_nom).uppercase(),
                fontSize = (ample * 0.069f).sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (ample * 0.01f).sp,
                color = Color(0xFFFFE082),
            )
            Text(
                stringResource(R.string.sobre_cartes_n, Sobres.CARTES_PER_SOBRE).uppercase(),
                fontSize = (ample * 0.041f).sp,
                letterSpacing = (ample * 0.007f).sp,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}
