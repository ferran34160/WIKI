package cat.cartes.esports.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val FoliaFosca = Color(0xFF5B2FD0)
private val FoliaClara = Color(0xFFA27CFF)

/** (centre x, centre y, angle, raresa) de les cartes translúcides que decoren el sobre. */
private val Decoracio = listOf(
    listOf(0.28f, 0.20f, -18f, 4),
    listOf(0.74f, 0.25f, 14f, 2),
    listOf(0.24f, 0.62f, 12f, 5),
    listOf(0.78f, 0.66f, -14f, 3),
    listOf(0.50f, 0.88f, 8f, 1),
)

private fun estrella(centre: Offset, radiExt: Float): Path {
    val radiInt = radiExt * 0.42f
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

/** Sobre de cartes dibuixat: paper metàl·lic amb vores dentades, cartes de colors i un halo que pulsa. */
@Composable
fun SobreView(modifier: Modifier = Modifier) {
    val transicio = rememberInfiniteTransition(label = "sobre")
    val halo by transicio.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "halo",
    )
    Canvas(modifier.aspectRatio(0.58f)) {
        val w = size.width
        val h = size.height

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
        drawPath(forma, Brush.horizontalGradient(listOf(FoliaFosca, FoliaClara, FoliaFosca)))

        clipPath(forma) {
            // Reflex diagonal del paper metàl·lic.
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.28f), Color.Transparent),
                    start = Offset(0f, h * 0.15f),
                    end = Offset(w, h * 0.55f),
                ),
            )
            // Línies de segellat a dalt i a baix.
            for (y in listOf(dent * 2.6f, h - dent * 2.6f)) {
                drawLine(Color.White.copy(alpha = 0.25f), Offset(0f, y), Offset(w, y), strokeWidth = 2f)
            }
            // Cartes de colors de raresa, translúcides.
            Decoracio.forEach { d ->
                val cx = w * (d[0] as Float)
                val cy = h * (d[1] as Float)
                val mida = Size(w * 0.30f, w * 0.30f / 0.68f)
                rotate(d[2] as Float, pivot = Offset(cx, cy)) {
                    drawRoundRect(
                        colorRaresa(d[3] as Int).copy(alpha = 0.55f),
                        topLeft = Offset(cx - mida.width / 2, cy - mida.height / 2),
                        size = mida,
                        cornerRadius = CornerRadius(w * 0.04f),
                    )
                }
            }
            // Emblema central.
            val c = Offset(w / 2, h * 0.46f)
            drawCircle(Color.Black.copy(alpha = 0.25f), w * 0.235f, c + Offset(0f, w * 0.01f))
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFFFFE082), Color(0xFFFFB300)), center = c, radius = w * 0.22f),
                w * 0.22f,
                c,
            )
            drawPath(estrella(c, w * 0.14f), Color(0xFF3A1B8F))
        }
    }
}
