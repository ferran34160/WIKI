package cat.cartes.esports.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.cartes.esports.R
import cat.cartes.esports.data.Carta
import cat.cartes.esports.data.EstilCarta
import cat.cartes.esports.data.banderaPais
import java.util.Locale

/** Estil de carta triat al perfil; el llegeix tota la interfície. */
val LocalEstilCarta = compositionLocalOf { EstilCarta.CLASSIC }

private fun colorEsport(esport: String?): Color = when (esport) {
    "futbol" -> Color(0xFF22C55E)
    "basquet" -> Color(0xFFFF7A1A)
    "tennis" -> Color(0xFFE6D83A)
    "atletisme" -> Color(0xFFE53E3E)
    else -> Color(0xFFFFB300)
}

private fun emojiEsport(esport: String?): String = when (esport) {
    "futbol" -> "⚽"
    "basquet" -> "🏀"
    "tennis" -> "🎾"
    "atletisme" -> "🏃"
    else -> "🏅"
}

/**
 * Carta d'estil «foto»: la foto ocupa tota la carta i la informació es redueix a un nom, una franja lateral
 * (país, esport, naixement i alçada) i la raresa. El text breu, els premis i el crèdit queden al dors de la fitxa.
 */
@Composable
fun CartaFoto(
    carta: Carta,
    modifier: Modifier = Modifier,
    compacta: Boolean = false,
    quantitat: Int = 0,
    nova: Boolean? = null,
    onClick: (() -> Unit)? = null,
) {
    val colorMarc = colorRaresa(carta.raresa)
    val accent = colorEsport(carta.esport)
    val forma = RoundedCornerShape(18.dp)
    val bandera = remember(carta.paisQid) { banderaPais(carta.paisQid) }
    BoxWithConstraints(
        modifier
            .aspectRatio(0.68f)
            .resplendor(colorMarc, forcaResplendor(carta.raresa))
            .clip(forma)
            .voraRaresa(carta.raresa, forma)
            .background(Color(0xFF0B0B10))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .holo(carta.raresa >= 3)
            .efectesRaresa(carta.raresa, carta.id.hashCode()),
    ) {
        val e = (maxWidth.value / 180f).coerceIn(0.75f, 1.7f)

        Icon(
            Icons.Default.Person,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.12f),
            modifier = Modifier.align(Alignment.Center).fillMaxSize(0.5f),
        )
        if (carta.fotoUrl != null) FotoCarta(carta.fotoUrl, carta.nom, Modifier.fillMaxSize())

        // Ombra a baix perquè el nom es llegeixi sobre qualsevol foto.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.38f)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)))),
        )

        // A dalt: raresa a l'esquerra; novetat, còpies i (a la graella) bandera a la dreta.
        EtiquetaRaresa(carta.raresa, nomRaresa(carta.raresa), e, Modifier.align(Alignment.TopStart).padding((8 * e).dp))
        Row(
            Modifier.align(Alignment.TopEnd).padding((8 * e).dp),
            horizontalArrangement = Arrangement.spacedBy((4 * e).dp),
        ) {
            nova?.let {
                Etiqueta(
                    if (it) stringResource(R.string.nova) else stringResource(R.string.repetida),
                    if (it) Color(0xFF1FA463) else Color(0xFF6B6890),
                    e,
                )
            }
            if (quantitat > 1) Etiqueta("×$quantitat", Color.Black.copy(alpha = 0.65f), e)
            if (compacta && bandera != null) {
                Text(
                    bandera,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(50))
                        .padding(horizontal = (5 * e).dp, vertical = (1 * e).dp),
                    fontSize = (13 * e).sp,
                )
            }
        }

        // A la graella, un distintiu daurat visible de lluny per a les llegendàries.
        if (compacta && carta.raresa >= 5) {
            Text(
                "★",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding((6 * e).dp)
                    .background(Brush.linearGradient(listOf(Color(0xFFFFF1A6), Color(0xFFFF9A3C))), CircleShape)
                    .padding(horizontal = (6 * e).dp, vertical = (2 * e).dp),
                fontSize = (13 * e).sp,
                color = Color(0xFF3A2400),
                fontWeight = FontWeight.ExtraBold,
            )
        }

        // Franja lateral amb país, esport, naixement i alçada (només a la carta gran).
        if (!compacta) {
            Column(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(top = (60 * e).dp)
                    .width((42 * e).dp)
                    .clip(RoundedCornerShape(topStart = (12 * e).dp, bottomStart = (12 * e).dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(vertical = (8 * e).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((6 * e).dp),
            ) {
                bandera?.let { Text(it, fontSize = (20 * e).sp) }
                Text(emojiEsport(carta.esport), fontSize = (18 * e).sp)
                Box(Modifier.width((22 * e).dp).height(2.dp).background(accent))
                Dada(carta.anyNaixement, stringResource(R.string.etiqueta_naix), accent, e)
                carta.alcadaM?.let {
                    Dada(String.format(Locale.getDefault(), "%.2f", it), stringResource(R.string.etiqueta_alcada), accent, e)
                }
            }
        }

        // Nom: el cognom gran i, a sobre, el nom de pila.
        val senseCognom = carta.cognom == null || carta.nomPila == null
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = (10 * e).dp, end = ((if (compacta) 10 else 50) * e).dp, bottom = (10 * e).dp),
        ) {
            if (!senseCognom) {
                Text(
                    carta.nomPila.orEmpty(),
                    fontSize = (9 * e).sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val nom = if (senseCognom) carta.nom else carta.cognom.orEmpty()
            val mida = (if (compacta) 12 else 17) * e
            if (carta.raresa >= 5) {
                TextDaurat(nom, mida, mida * 1.1f, maxLinies = 2)
            } else {
                Text(
                    nom,
                    fontSize = mida.sp,
                    lineHeight = (mida * 1.1f).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(Modifier.padding(top = (4 * e).dp).width((30 * e).dp).height(3.dp).background(accent))
        }
    }
}

@Composable
private fun Dada(valor: String?, etiqueta: String, accent: Color, e: Float) {
    if (valor == null) return
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            valor,
            fontSize = (11 * e).sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Text(etiqueta, fontSize = (6.5f * e).sp, color = accent, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
