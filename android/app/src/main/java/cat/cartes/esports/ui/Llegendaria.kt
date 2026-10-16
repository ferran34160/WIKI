package cat.cartes.esports.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

private val Or = Color(0xFFFFD54A)
private val OrClar = Color(0xFFFFF2B3)

/** Temps en bucle de 0 a 1 (període en ms). Es llegeix només en dibuixar, perquè no recomposi. */
@Composable
fun rememberTemps(periodeMs: Int): State<Float> {
    val transicio = rememberInfiniteTransition(label = "temps")
    return transicio.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(periodeMs, easing = LinearEasing), RepeatMode.Restart),
        label = "temps_t",
    )
}

/** Inclinació del mòbil (-1..1 en cada eix), suavitzada. Només escolta el sensor mentre [actiu]. */
@Composable
fun rememberInclinacio(actiu: Boolean): State<Offset> {
    val context = LocalContext.current
    val estat = remember { mutableStateOf(Offset.Zero) }
    DisposableEffect(actiu) {
        if (!actiu) return@DisposableEffect onDispose { }
        val gestor = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = gestor?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val oient = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val x = (-e.values[0] / 9.8f).coerceIn(-1f, 1f)
                val y = (e.values[1] / 9.8f).coerceIn(-1f, 1f)
                val a = estat.value
                estat.value = Offset(a.x + (x - a.x) * 0.12f, a.y + (y - a.y) * 0.12f)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (gestor != null && sensor != null) gestor.registerListener(oient, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { gestor?.unregisterListener(oient) }
    }
    return estat
}

/** Espurna de quatre puntes centrada a l'origen. */
private fun espurna(r: Float): Path = Path().apply {
    val c = r * 0.2f
    moveTo(0f, -r); lineTo(c, -c); lineTo(r, 0f); lineTo(c, c)
    lineTo(0f, r); lineTo(-c, c); lineTo(-r, 0f); lineTo(-c, -c); close()
}

private class Espurna(val x: Float, val y: Float, val mida: Float, val fase: Float, val velocitat: Float)

private val COLORS_FOIL = listOf(
    Color(0xFFFF4D6D), Color(0xFFFFD166), Color(0xFF06D6A0), Color(0xFF4CC9F0), Color(0xFFB388FF), Color(0xFFFF4D6D),
)

/**
 * Efectes de les cartes que destaquen. Llegendàries (5): reflex hologràfic que es mou amb la inclinació del mòbil,
 * espurnes, marc interior i cantonades daurades. Èpiques (4): una versió molt més suau (reflex i poques espurnes).
 */
@Composable
fun Modifier.efectesRaresa(raresa: Int, llavor: Int): Modifier {
    if (raresa < 4) return this
    val llegendaria = raresa >= 5
    val temps = rememberTemps(if (llegendaria) 3600 else 5200)
    val inclinacio = rememberInclinacio(actiu = llegendaria)
    val espurnes = remember(llavor, raresa) {
        val rnd = Random(llavor)
        List(if (llegendaria) 14 else 6) {
            Espurna(
                x = 0.06f + rnd.nextFloat() * 0.88f, y = 0.05f + rnd.nextFloat() * 0.9f,
                mida = 3f + rnd.nextFloat() * 6f, fase = rnd.nextFloat(), velocitat = 1f + rnd.nextInt(3),
            )
        }
    }
    return this.drawWithContent {
        drawContent()
        val u = temps.value
        val w = size.width
        val h = size.height
        // Reflex hologràfic: el degradat d'arc de Sant Martí es desplaça amb el temps i amb la inclinació.
        val desp = (u * 2f - 1f) * w * 0.5f + inclinacio.value.x * w * 0.9f + inclinacio.value.y * h * 0.3f
        val alfa = if (llegendaria) 0.30f else 0.14f
        drawRect(
            Brush.linearGradient(
                colors = COLORS_FOIL.map { it.copy(alpha = alfa) },
                start = Offset(desp, 0f), end = Offset(desp + w * 0.9f, h * 0.9f), tileMode = TileMode.Mirror,
            ),
            blendMode = BlendMode.Overlay,
        )
        // Espurnes que s'encenen i s'apaguen.
        espurnes.forEach { e ->
            val fase = (u * e.velocitat + e.fase) % 1f
            val brillantor = max(0f, sin(fase * 2f * PI.toFloat()))
            val a = brillantor * brillantor * brillantor
            if (a > 0.02f) {
                val r = e.mida.dp.toPx() * (0.6f + 0.6f * a)
                withTransform({ translate(e.x * w, e.y * h) }) {
                    drawPath(espurna(r), OrClar.copy(alpha = a))
                    drawCircle(Color.White.copy(alpha = a * 0.8f), r * 0.18f)
                }
            }
        }
        if (llegendaria) ornamentsDaurats()
    }
}

/** Marc interior fi i cantonades en angle, en daurat. */
private fun DrawScope.ornamentsDaurats() {
    val w = size.width
    val h = size.height
    val marge = 7.dp.toPx()
    drawRoundRect(
        Or.copy(alpha = 0.75f), Offset(marge, marge), Size(w - 2 * marge, h - 2 * marge),
        CornerRadius(14.dp.toPx()), style = Stroke(1.dp.toPx()),
    )
    val llarg = 20.dp.toPx()
    val gruix = 2.5f.dp.toPx()
    val m = marge + 3.dp.toPx()
    for ((cx, cy, sx, sy) in listOf(
        listOf(m, m, 1f, 1f), listOf(w - m, m, -1f, 1f), listOf(m, h - m, 1f, -1f), listOf(w - m, h - m, -1f, -1f),
    )) {
        drawLine(Or, Offset(cx, cy), Offset(cx + sx * llarg, cy), gruix)
        drawLine(Or, Offset(cx, cy), Offset(cx, cy + sy * llarg), gruix)
    }
}

/** Cinta de raresa: daurada amb un reflex que la recorre per a les llegendàries, lila per a les èpiques. */
@Composable
fun EtiquetaRaresa(raresa: Int, text: String, escala: Float, modifier: Modifier = Modifier) {
    if (raresa < 4) {
        Etiqueta(text, colorRaresa(raresa), escala, modifier)
        return
    }
    val llegendaria = raresa >= 5
    val temps = rememberTemps(2600)
    val colors = if (llegendaria) listOf(Color(0xFFFFF1A6), Color(0xFFFFB300), Color(0xFFFF8F00))
    else listOf(Color(0xFFD9A8FF), Color(0xFF9C27B0))
    val forma = RoundedCornerShape(50)
    Text(
        if (llegendaria) "★ $text" else text,
        modifier = modifier
            .background(Brush.horizontalGradient(colors), forma)
            .border(1.dp, Color.White.copy(alpha = 0.7f), forma)
            .drawWithContent {
                drawContent()
                val x = (temps.value * 2.4f - 0.7f) * size.width
                drawRect(
                    Brush.linearGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.65f), Color.Transparent),
                        start = Offset(x - size.width * 0.25f, 0f), end = Offset(x + size.width * 0.25f, size.height),
                    ),
                )
            }
            .padding(horizontal = (8 * escala).dp, vertical = (2.5f * escala).dp),
        fontSize = (10.5f * escala).sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (llegendaria) Color(0xFF3A2400) else Color.White,
        maxLines = 1,
        overflow = TextOverflow.Clip,
    )
}

/** Text amb degradat daurat i un tret de llum que hi passa (per al nom de les llegendàries). */
@Composable
fun TextDaurat(
    text: String, mida: Float, interlineat: Float, modifier: Modifier = Modifier, maxLinies: Int = 1,
) {
    val temps = rememberTemps(3200)
    val x = temps.value * 600f - 100f
    Text(
        text,
        modifier = modifier,
        style = TextStyle(
            brush = Brush.linearGradient(
                listOf(Color(0xFFFFE082), Color(0xFFFFB300), Color(0xFFFFFFFF), Color(0xFFFFB300), Color(0xFFFFE082)),
                start = Offset(x, 0f), end = Offset(x + 300f, 120f), tileMode = TileMode.Mirror,
            ),
            fontSize = mida.sp, lineHeight = interlineat.sp, fontWeight = FontWeight.ExtraBold,
        ),
        maxLines = maxLinies,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Raigs daurats que giren lentament, per al fons quan surt una llegendària. */
@Composable
fun RaigsDaurats(modifier: Modifier = Modifier) {
    val temps = rememberTemps(14000)
    Canvas(modifier.fillMaxSize()) {
        val centre = Offset(size.width / 2f, size.height * 0.45f)
        val llarg = size.maxDimension
        rotate(temps.value * 360f, pivot = centre) {
            for (i in 0 until 16) {
                val a0 = (i * 22.5f) * PI / 180
                val a1 = a0 + 8f * PI / 180
                val raig = Path().apply {
                    moveTo(centre.x, centre.y)
                    lineTo(centre.x + llarg * kotlin.math.cos(a0).toFloat(), centre.y + llarg * kotlin.math.sin(a0).toFloat())
                    lineTo(centre.x + llarg * kotlin.math.cos(a1).toFloat(), centre.y + llarg * kotlin.math.sin(a1).toFloat())
                    close()
                }
                drawPath(
                    raig,
                    Brush.radialGradient(
                        listOf(Or.copy(alpha = 0.32f), Color.Transparent), center = centre, radius = llarg * 0.7f,
                    ),
                )
            }
        }
    }
}
