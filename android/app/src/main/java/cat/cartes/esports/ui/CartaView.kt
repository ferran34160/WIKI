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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.cartes.esports.data.Carta
import coil.compose.AsyncImage
import java.util.Locale

private val FonsCarta = Color(0xFF1E1B3A)
private val TintaCarta = Color(0xFF14122B)

/** Parell de colors (a dalt, a baix) del fons de la carta segons la raresa. */
private fun fonsRaresa(raresa: Int): Pair<Color, Color> = when (raresa) {
    1 -> Color(0xFFE6E8EC) to Color(0xFFB7BDC8)
    2 -> Color(0xFFCDF2DA) to Color(0xFF7FD6A0)
    3 -> Color(0xFFC4E1FB) to Color(0xFF6FB1F0)
    4 -> Color(0xFFDCC5F7) to Color(0xFFA070E0)
    else -> Color(0xFFFFDB8A) to Color(0xFFFF9A3C)
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

/** Fons amb degradat vertical i anells translúcids a l'estil de les ones d'una carta tipus holo. */
private fun Modifier.fonsTexturat(dalt: Color, baix: Color): Modifier = this
    .background(Brush.verticalGradient(listOf(dalt, baix)))
    .drawBehind {
        val traç = Stroke(width = size.width * 0.035f)
        val centre = Offset(size.width * 0.95f, size.height * 0.85f)
        for (i in 1..7) {
            drawCircle(
                color = Color.White.copy(alpha = 0.16f),
                radius = size.width * 0.22f * i,
                center = centre,
                style = traç,
            )
        }
    }

/** Text d'una o dues línies que redueix la mida fins que cap dins l'espai disponible. */
@Composable
private fun TextAjustat(
    text: String,
    mida: TextUnit,
    minim: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = TintaCarta,
    pes: FontWeight = FontWeight.Normal,
    maxLinies: Int = 1,
    alineacio: TextAlign = TextAlign.Start,
) {
    var actual by remember(text, mida) { mutableStateOf(mida) }
    Text(
        text,
        modifier = modifier,
        fontSize = actual,
        fontWeight = pes,
        color = color,
        maxLines = maxLinies,
        overflow = TextOverflow.Ellipsis,
        textAlign = alineacio,
        onTextLayout = { r ->
            if ((r.didOverflowHeight || r.didOverflowWidth) && actual.value > minim.value) {
                actual = (actual.value * 0.9f).sp
            }
        },
    )
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

/** Carta d'esportista. [compacta] és la versió de quadrícula (sense peu de dades). */
@Composable
fun CartaView(
    carta: Carta,
    modifier: Modifier = Modifier,
    compacta: Boolean = false,
    quantitat: Int = 0,
    onClick: (() -> Unit)? = null,
) {
    val colorMarc = colorRaresa(carta.raresa)
    val (dalt, baix) = fonsRaresa(carta.raresa)
    val forma = RoundedCornerShape(18.dp)
    BoxWithConstraints(
        modifier
            .aspectRatio(0.68f)
            .clip(forma)
            .border(1.5.dp, colorMarc.copy(alpha = 0.9f), forma)
            .fonsTexturat(dalt, baix)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .holo(carta.raresa >= 3),
    ) {
        // Tot el text s'escala amb l'ample de la carta; així cap a la quadrícula i al detall.
        val escala = (maxWidth.value / 180f).coerceIn(0.75f, 1.7f)
        val fonsFoto = lerp(dalt, baix, 0.58f)
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(0.58f).fillMaxWidth()) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = TintaCarta.copy(alpha = 0.18f),
                    modifier = Modifier.align(Alignment.Center).fillMaxSize(0.55f),
                )
                if (carta.fotoUrl != null) {
                    AsyncImage(
                        model = carta.fotoUrl,
                        contentDescription = carta.nom,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                // Degradat que fon la foto amb el fons de la carta.
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.38f)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, fonsFoto))),
                )
                Etiqueta(
                    nomRaresa(carta.raresa),
                    colorMarc,
                    escala,
                    Modifier.align(Alignment.TopStart).padding((8 * escala).dp),
                )
                if (quantitat > 1) {
                    Etiqueta(
                        "×$quantitat",
                        Color.Black.copy(alpha = 0.65f),
                        escala,
                        Modifier.align(Alignment.TopEnd).padding((8 * escala).dp),
                    )
                }
            }
            Column(
                Modifier
                    .weight(0.42f)
                    .fillMaxWidth()
                    .padding(horizontal = (10 * escala).dp, vertical = (4 * escala).dp),
            ) {
                TextAjustat(
                    carta.nom,
                    mida = (16 * escala).sp,
                    minim = (9 * escala).sp,
                    pes = FontWeight.ExtraBold,
                    maxLinies = 2,
                )
                TextAjustat(
                    nomEsport(carta.esport),
                    mida = (11 * escala).sp,
                    minim = (8 * escala).sp,
                    color = TintaCarta.copy(alpha = 0.7f),
                )
                if (!compacta) {
                    Spacer(Modifier.weight(1f))
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
                        TextAjustat(
                            vida(carta).orEmpty(),
                            mida = (11 * escala).sp,
                            minim = (8 * escala).sp,
                            pes = FontWeight.SemiBold,
                        )
                        carta.alcadaM?.let {
                            TextAjustat(
                                String.format(Locale.getDefault(), "%.2f m", it),
                                mida = (11 * escala).sp,
                                minim = (8 * escala).sp,
                                pes = FontWeight.SemiBold,
                                alineacio = TextAlign.End,
                            )
                        }
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
