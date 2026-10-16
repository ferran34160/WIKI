package cat.cartes.esports.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.cartes.esports.R
import cat.cartes.esports.data.Carta
import cat.cartes.esports.data.banderaPais
import java.util.Locale

private val FonsCarta = Color(0xFF111117)
private val TintaCarta = Color(0xFF14122B)

/** Parell de colors (a dalt, a baix) del fons de la carta segons la raresa. */
private fun fonsRaresa(raresa: Int): Pair<Color, Color> = when (raresa) {
    1 -> Color(0xFFE6E8EC) to Color(0xFFB7BDC8)
    2 -> Color(0xFFCDF2DA) to Color(0xFF7FD6A0)
    3 -> Color(0xFFC4E1FB) to Color(0xFF6FB1F0)
    4 -> Color(0xFFDCC5F7) to Color(0xFFA070E0)
    else -> Color(0xFFFFDB8A) to Color(0xFFFF9A3C)
}

/** Colors de la vora (clar, intens, clar) segons la raresa; les llegendàries porten un degradat daurat. */
private fun coloresVora(raresa: Int): List<Color> = when (raresa) {
    1 -> listOf(Color(0xFFE0E0E0), Color(0xFF8E8E93), Color(0xFFE0E0E0))
    2 -> listOf(Color(0xFFA5F5BF), Color(0xFF2E9E5B), Color(0xFFA5F5BF))
    3 -> listOf(Color(0xFF9CD0FF), Color(0xFF1E6FCC), Color(0xFF9CD0FF))
    4 -> listOf(Color(0xFFE9C6FF), Color(0xFF8E3FD0), Color(0xFFE9C6FF))
    else -> listOf(Color(0xFFFFF1A6), Color(0xFFFFB300), Color(0xFFFF7A00), Color(0xFFFFF1A6))
}

private fun forcaResplendor(raresa: Int): Float = when (raresa) {
    1 -> 0f
    2 -> 0.5f
    3 -> 0.8f
    4 -> 1.1f
    else -> 1.5f
}

/** Vora amb degradat metàl·lic; a les èpiques i llegendàries el degradat es mou. */
@Composable
private fun Modifier.voraRaresa(raresa: Int, forma: Shape): Modifier {
    val transicio = rememberInfiniteTransition(label = "vora")
    val t by transicio.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "vora_t",
    )
    val x = if (raresa >= 4) t * 1200f else 0f
    val pinzell = Brush.linearGradient(
        colors = coloresVora(raresa),
        start = Offset(x, 0f),
        end = Offset(x + 600f, 600f),
        tileMode = TileMode.Mirror,
    )
    return this.border(if (raresa >= 4) 3.5.dp else 2.5.dp, pinzell, forma)
}

/** Halo de color de la raresa al voltant de la carta, fet amb capes translúcides. */
private fun Modifier.resplendor(color: Color, forca: Float): Modifier = if (forca <= 0f) this else drawBehind {
    val pas = 2.5.dp.toPx()
    val radi = 18.dp.toPx()
    for (i in 7 downTo 1) {
        val d = i * pas
        drawRoundRect(
            color.copy(alpha = 0.07f * forca),
            topLeft = Offset(-d, -d),
            size = Size(size.width + 2 * d, size.height + 2 * d),
            cornerRadius = CornerRadius(radi + d),
        )
    }
}

/** Franja de brillantor que recorre la carta (rares i millors). */
@Composable
private fun Modifier.holo(actiu: Boolean): Modifier {
    if (!actiu) return this
    val transicio = rememberInfiniteTransition(label = "holo")
    val t by transicio.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "holo_t",
    )
    return this.drawWithContent {
        drawContent()
        val x = size.width * 2.2f * t - size.width * 0.6f
        drawRect(
            Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.30f), Color.Transparent),
                start = Offset(x - size.width * 0.35f, 0f),
                end = Offset(x + size.width * 0.35f, size.height),
            ),
        )
    }
}

private val TraçMotiu = Color.White.copy(alpha = 0.11f)

/** Línies del terreny de joc de cada esport, dibuixades en blanc translúcid sobre el fons. */
private fun DrawScope.motiuEsport(esport: String?) {
    val w = size.width
    val h = size.height
    val tr = Stroke(width = maxOf(2f, w * 0.012f))
    when (esport) {
        "futbol" -> {
            // Mig camp, cercle central i àrea de penal.
            drawRect(TraçMotiu, Offset(w * 0.06f, h * 0.06f), Size(w * 0.88f, h * 0.9f), style = tr)
            drawLine(TraçMotiu, Offset(w * 0.06f, h * 0.55f), Offset(w * 0.94f, h * 0.55f), tr.width)
            drawCircle(TraçMotiu, w * 0.2f, Offset(w / 2, h * 0.55f), style = tr)
            drawCircle(TraçMotiu, w * 0.015f, Offset(w / 2, h * 0.55f))
            drawRect(TraçMotiu, Offset(w * 0.22f, h * 0.78f), Size(w * 0.56f, h * 0.18f), style = tr)
            drawRect(TraçMotiu, Offset(w * 0.36f, h * 0.89f), Size(w * 0.28f, h * 0.07f), style = tr)
            drawArc(TraçMotiu, 215f, 110f, false, Offset(w * 0.36f, h * 0.7f), Size(w * 0.28f, w * 0.28f), style = tr)
        }
        "basquet" -> {
            // Mitja pista: línia de fons, bombolla, cercle de tirs lliures i línia de triples.
            drawRect(TraçMotiu, Offset(w * 0.06f, h * 0.06f), Size(w * 0.88f, h * 0.9f), style = tr)
            drawRect(TraçMotiu, Offset(w * 0.36f, h * 0.64f), Size(w * 0.28f, h * 0.32f), style = tr)
            drawArc(TraçMotiu, 180f, 180f, false, Offset(w * 0.36f, h * 0.64f - w * 0.14f), Size(w * 0.28f, w * 0.28f), style = tr)
            drawLine(TraçMotiu, Offset(w * 0.14f, h * 0.96f), Offset(w * 0.14f, h * 0.8f), tr.width)
            drawLine(TraçMotiu, Offset(w * 0.86f, h * 0.96f), Offset(w * 0.86f, h * 0.8f), tr.width)
            drawArc(TraçMotiu, 180f, 180f, false, Offset(w * 0.14f, h * 0.8f - w * 0.36f), Size(w * 0.72f, w * 0.72f), style = tr)
            drawCircle(TraçMotiu, w * 0.025f, Offset(w / 2, h * 0.9f), style = tr)
        }
        "tennis" -> {
            // Pista: línies de fons i laterals, línies de servei, xarxa i línia central.
            drawRect(TraçMotiu, Offset(w * 0.1f, h * 0.36f), Size(w * 0.8f, h * 0.6f), style = tr)
            drawLine(TraçMotiu, Offset(w * 0.2f, h * 0.36f), Offset(w * 0.2f, h * 0.96f), tr.width)
            drawLine(TraçMotiu, Offset(w * 0.8f, h * 0.36f), Offset(w * 0.8f, h * 0.96f), tr.width)
            drawLine(TraçMotiu, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), tr.width)
            drawLine(TraçMotiu, Offset(w * 0.2f, h * 0.82f), Offset(w * 0.8f, h * 0.82f), tr.width)
            drawLine(TraçMotiu, Offset(w / 2, h * 0.5f), Offset(w / 2, h * 0.82f), tr.width)
            drawLine(TraçMotiu, Offset(w * 0.06f, h * 0.66f), Offset(w * 0.94f, h * 0.66f), tr.width * 1.6f)
        }
        "atletisme" -> {
            // Pista d'atletisme vista de dalt: carrils concèntrics.
            for (i in 0..5) {
                val amp = w * (0.9f - i * 0.1f)
                val alt = h * (0.55f - i * 0.065f)
                drawRoundRect(
                    TraçMotiu,
                    topLeft = Offset((w - amp) / 2, h * 0.97f - alt),
                    size = Size(amp, alt),
                    cornerRadius = CornerRadius(amp / 2),
                    style = tr,
                )
            }
        }
        else -> {
            val centre = Offset(w * 0.95f, h * 0.85f)
            for (i in 1..7) drawCircle(TraçMotiu, w * 0.22f * i, centre, style = tr)
        }
    }
}

/** Fons amb degradat vertical i el motiu de l'esport. */
private fun Modifier.fonsTexturat(dalt: Color, baix: Color, esport: String?): Modifier = this
    .background(Brush.verticalGradient(listOf(dalt, baix)))
    .drawBehind { motiuEsport(esport) }

/** Text de mida fixa (la mateixa a totes les cartes); si no hi cap, es talla amb punts suspensius. */
@Composable
private fun TextFix(
    text: String,
    mida: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = TintaCarta,
    pes: FontWeight = FontWeight.Normal,
    maxLinies: Int = 1,
    alineacio: TextAlign = TextAlign.Start,
) {
    Text(
        text,
        modifier = modifier,
        fontSize = mida,
        lineHeight = (mida.value * 1.2f).sp,
        fontWeight = pes,
        color = color,
        maxLines = maxLinies,
        overflow = TextOverflow.Ellipsis,
        textAlign = alineacio,
    )
}

private val SEPARADOR_FRASES = Regex("(?<=[.!?])(?<!\\b\\p{Lu}[.])\\s+(?=[\\p{Lu}«\"'¿¡(])")

/**
 * Text de la carta amb la mateixa mida de lletra a totes: mostra [prefix] (premis) i, a continuació, tantes
 * frases senceres de [text] com càpiguen a l'espai disponible. Així mai es talla una frase a mitges.
 */
@Composable
private fun TextEnCaixa(prefix: String?, text: String, mida: TextUnit, color: Color, modifier: Modifier = Modifier) {
    val mesurador = rememberTextMeasurer()
    val estil = remember(mida, color) {
        TextStyle(fontSize = mida, lineHeight = (mida.value * 1.2f).sp, color = color)
    }
    BoxWithConstraints(modifier) {
        val ample = constraints.maxWidth
        val alt = constraints.maxHeight
        val visible = remember(prefix, text, estil, ample, alt) {
            fun cap(candidat: String) = mesurador.measure(
                AnnotatedString(candidat), estil, constraints = Constraints(maxWidth = ample),
            ).size.height <= alt
            val base = if (prefix.isNullOrBlank()) "" else prefix.trimEnd() + "\n\n"
            val frases = text.trim().split(SEPARADOR_FRASES)
            var millor = ""
            for (k in 1..frases.size) {
                val candidat = base + frases.take(k).joinToString(" ")
                if (cap(candidat)) millor = candidat else break
            }
            millor.ifEmpty { base + frases.first() }
        }
        val liniesMax = remember(estil, alt) {
            val alcada = mesurador.measure(AnnotatedString("A"), estil).size.height.coerceAtLeast(1)
            (alt / alcada).coerceAtLeast(1)
        }
        Text(visible, style = estil, maxLines = liniesMax, overflow = TextOverflow.Ellipsis)
    }
}

/** Etiqueta arrodonida (raresa, any). */
@Composable
private fun Etiqueta(text: String, fons: Color, escala: Float, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier
            .background(fons, RoundedCornerShape(50))
            .padding(horizontal = (7 * escala).dp, vertical = (2 * escala).dp),
        fontSize = (10 * escala).sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        maxLines = 1,
        overflow = TextOverflow.Clip,
    )
}

private fun vida(carta: Carta): String? {
    val n = carta.anyNaixement ?: return null
    val d = carta.defuncio?.take(4)
    return if (d != null) "$n–$d" else n
}

/**
 * Carta d'esportista. [compacta] és la versió de quadrícula (sense peu de dades).
 * Si hi ha [extracte] (i [premis]), es mostren dins la carta amb una mida de lletra fixa: només s'hi posen
 * les frases senceres que hi caben.
 */
@Composable
fun CartaView(
    carta: Carta,
    modifier: Modifier = Modifier,
    compacta: Boolean = false,
    quantitat: Int = 0,
    extracte: String? = null,
    premis: String? = null,
    pais: String? = null,
    nova: Boolean? = null,
    credit: String? = null,
    onCredit: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val colorMarc = colorRaresa(carta.raresa)
    val (dalt, baix) = fonsRaresa(carta.raresa)
    val forma = RoundedCornerShape(18.dp)
    val ambText = extracte != null || premis != null
    val bandera = remember(carta.paisQid) { banderaPais(carta.paisQid) }
    val esport = nomEsport(carta.esport)
    val etiquetaEsport = listOfNotNull(esport.takeIf { it.isNotBlank() }, pais?.takeIf { it.isNotBlank() })
        .joinToString(" · ")
    BoxWithConstraints(
        modifier
            .aspectRatio(0.68f)
            .resplendor(colorMarc, forcaResplendor(carta.raresa))
            .clip(forma)
            .voraRaresa(carta.raresa, forma)
            .fonsTexturat(dalt, baix, carta.esport)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .holo(carta.raresa >= 3),
    ) {
        // Tot el text s'escala amb l'ample de la carta; així cap a la quadrícula i al detall.
        val escala = (maxWidth.value / 180f).coerceIn(0.75f, 1.7f)
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(if (ambText) 0.42f else 0.58f).fillMaxWidth()) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = TintaCarta.copy(alpha = 0.18f),
                    modifier = Modifier.align(Alignment.Center).fillMaxSize(0.55f),
                )
                if (carta.fotoUrl != null) {
                    FotoCarta(carta.fotoUrl, carta.nom, Modifier.fillMaxSize())
                }
                Etiqueta(
                    nomRaresa(carta.raresa),
                    colorMarc,
                    escala,
                    Modifier.align(Alignment.TopStart).padding((8 * escala).dp),
                )
                bandera?.let {
                    Text(
                        it,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding((8 * escala).dp)
                            .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50))
                            .padding(horizontal = (6 * escala).dp, vertical = (2 * escala).dp),
                        fontSize = (16 * escala).sp,
                    )
                }
                nova?.let {
                    Etiqueta(
                        if (it) stringResource(R.string.nova) else stringResource(R.string.repetida),
                        if (it) Color(0xFF1FA463) else Color(0xFF6B6890),
                        escala,
                        Modifier.align(Alignment.BottomEnd).padding((8 * escala).dp),
                    )
                }
                if (quantitat > 1) {
                    Etiqueta(
                        "×$quantitat",
                        Color.Black.copy(alpha = 0.65f),
                        escala,
                        Modifier.align(Alignment.BottomStart).padding((8 * escala).dp),
                    )
                }
            }
            Column(
                Modifier
                    .weight(if (ambText) 0.58f else 0.42f)
                    .fillMaxWidth()
                    .padding(horizontal = (10 * escala).dp, vertical = (4 * escala).dp),
            ) {
                TextFix(
                    carta.nom,
                    mida = ((if (ambText) 12 else 16) * escala).sp,
                    pes = FontWeight.ExtraBold,
                    maxLinies = 2,
                )
                TextFix(
                    etiquetaEsport,
                    mida = ((if (ambText) 9 else 11) * escala).sp,
                    color = TintaCarta.copy(alpha = 0.7f),
                )
                if (!compacta) {
                    if (ambText) {
                        TextEnCaixa(
                            premis,
                            extracte.orEmpty(),
                            mida = (6.4f * escala).sp,
                            color = TintaCarta.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = (4 * escala).dp),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(TintaCarta.copy(alpha = 0.2f)),
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = (3 * escala).dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextFix(
                            vida(carta).orEmpty(),
                            mida = ((if (ambText) 9 else 11) * escala).sp,
                            pes = FontWeight.SemiBold,
                        )
                        carta.alcadaM?.let {
                            TextFix(
                                String.format(Locale.getDefault(), "%.2f m", it),
                                mida = ((if (ambText) 9 else 11) * escala).sp,
                                pes = FontWeight.SemiBold,
                                alineacio = TextAlign.End,
                            )
                        }
                    }
                    if (credit != null) {
                        TextFix(
                            credit,
                            mida = (6.5f * escala).sp,
                            modifier = if (onCredit != null) Modifier.clickable(onClick = onCredit) else Modifier,
                            color = TintaCarta.copy(alpha = 0.55f),
                        )
                    }
                }
            }
        }
    }
}

/** Carta que encara no tens: silueta amb el color de la seva raresa. */
@Composable
fun CartaSilueta(raresa: Int, modifier: Modifier = Modifier) {
    val color = colorRaresa(raresa)
    val forma = RoundedCornerShape(18.dp)
    Box(
        modifier
            .aspectRatio(0.68f)
            .clip(forma)
            .border(1.5.dp, color.copy(alpha = 0.4f), forma)
            .background(FonsCarta),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            tint = color.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxSize(0.55f),
        )
    }
}
