package cat.cartes.esports.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.cartes.esports.data.Carta
import coil.compose.AsyncImage

private val FonsCarta = Color(0xFF1E1B3A)

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

private fun gruixMarc(raresa: Int) = when (raresa) {
    1 -> 1.5.dp
    2 -> 2.dp
    3 -> 3.dp
    4 -> 4.dp
    else -> 5.dp
}

/** Carta d'esportista. [compacta] és la versió de quadrícula (sense dades). */
@Composable
fun CartaView(
    carta: Carta,
    modifier: Modifier = Modifier,
    compacta: Boolean = false,
    quantitat: Int = 0,
    onClick: (() -> Unit)? = null,
) {
    val color = colorRaresa(carta.raresa)
    val forma = RoundedCornerShape(14.dp)
    Box(
        modifier
            .aspectRatio(0.68f)
            .clip(forma)
            .background(color)
            .padding(gruixMarc(carta.raresa))
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .holo(carta.raresa >= 3),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(FonsCarta),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(color.copy(alpha = 0.85f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    nomRaresa(carta.raresa),
                    fontSize = if (compacta) 10.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    maxLines = 1,
                )
                Text(
                    nomEsport(carta.esport),
                    fontSize = if (compacta) 10.sp else 12.sp,
                    color = Color.Black,
                    maxLines = 1,
                )
            }
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxSize(0.6f),
                )
                if (carta.fotoUrl != null) {
                    AsyncImage(
                        model = carta.fotoUrl,
                        contentDescription = carta.nom,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                carta.anyNaixement?.let { any ->
                    Text(
                        any,
                        fontSize = 11.sp,
                        color = Color.Black,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(color.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
                if (quantitat > 1) {
                    Text(
                        "×$quantitat",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp)) {
                val senseCognom = carta.cognom == null || carta.nomPila == null
                if (!senseCognom && !compacta) {
                    Text(
                        carta.nomPila.orEmpty(),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    if (senseCognom) carta.nom else if (compacta) carta.nom else carta.cognom.orEmpty(),
                    fontSize = if (compacta) 12.sp else 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Carta que encara no tens: silueta amb el color de la seva raresa. */
@Composable
fun CartaSilueta(raresa: Int, modifier: Modifier = Modifier) {
    val color = colorRaresa(raresa)
    Box(
        modifier
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.35f))
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
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
}
