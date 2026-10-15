package cat.cartes.esports.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.cartes.esports.R
import cat.cartes.esports.data.Carta
import cat.cartes.esports.game.JocViewModel
import cat.cartes.esports.game.ResultatSobre
import cat.cartes.esports.game.Sobres
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun PantallaInici(vm: JocViewModel, onObreCarta: (String) -> Unit) {
    var resultat by remember { mutableStateOf<ResultatSobre?>(null) }
    val res = resultat
    if (res != null) {
        ObertureSobre(res, onTanca = { resultat = null }, onObreCarta = onObreCarta)
        return
    }

    val guardats = vm.sobres.guardats
    val ms = vm.msFinsProperSobre()
    val minuts = ms / 60_000
    val segons = (ms / 1000) % 60
    val temps = "%02d:%02d".format(minuts, segons)

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.55f)
                .aspectRatio(0.68f)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.secondary)
                .padding(6.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$guardats",
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.sobres_guardats, guardats, Sobres.MAX_GUARDATS),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            if (guardats >= Sobres.MAX_GUARDATS) {
                stringResource(R.string.sobres_plens)
            } else {
                stringResource(R.string.proper_sobre, temps)
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { resultat = vm.obreSobre() },
            enabled = guardats > 0,
        ) {
            Text(
                if (guardats > 0) stringResource(R.string.obre_sobre)
                else stringResource(R.string.sense_sobres),
            )
        }
    }
}

@Composable
private fun ObertureSobre(
    res: ResultatSobre,
    onTanca: () -> Unit,
    onObreCarta: (String) -> Unit,
) {
    val girades = remember(res) { mutableStateListOf(*Array(res.cartes.size) { false }) }
    val hiHaLlegendaria = res.cartes.indices.any { girades[it] && res.cartes[it].raresa == 5 }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.toca_girar),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(res.cartes) { i, carta ->
                    CartaGirable(
                        carta = carta,
                        nova = carta.id in res.noves,
                        girada = girades[i],
                        onClick = {
                            if (!girades[i]) girades[i] = true else onObreCarta(carta.id)
                        },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { girades.indices.forEach { girades[it] = true } }) {
                    Text(stringResource(R.string.revela_totes))
                }
                Button(onClick = onTanca) { Text(stringResource(R.string.continua)) }
            }
        }
        if (hiHaLlegendaria) {
            Confeti()
            Text(
                stringResource(R.string.legendaria_alerta),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colorRaresa(5),
            )
        }
    }
}

@Composable
private fun CartaGirable(carta: Carta, nova: Boolean, girada: Boolean, onClick: () -> Unit) {
    val rotacio by animateFloatAsState(
        targetValue = if (girada) 0f else 180f,
        animationSpec = tween(450),
        label = "gira",
    )
    val densitat = LocalDensity.current.density
    Box(
        Modifier
            .graphicsLayer {
                rotationY = rotacio
                cameraDistance = 12f * densitat
            }
            .clickable(onClick = onClick),
    ) {
        if (rotacio > 90f) {
            // Dors de la carta (es contra-gira perquè no es vegi en mirall)
            Box(
                Modifier
                    .graphicsLayer { rotationY = 180f }
                    .aspectRatio(0.68f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(4.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("?", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        } else {
            CartaView(carta, compacta = true)
            Text(
                if (nova) stringResource(R.string.nova) else stringResource(R.string.repetida),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 26.dp)
                    .background(
                        if (nova) Color(0xFF69F0AE) else Color(0xFFCFCBEA),
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}

private class Particula(
    val x: Float, val y0: Float, val velocitat: Float, val color: Color, val radi: Float,
)

@Composable
private fun Confeti() {
    val progres = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progres.animateTo(1f, tween(2800, easing = LinearEasing)) }
    val colors = remember {
        listOf(
            Color(0xFFFFB300), Color(0xFF7C4DFF), Color(0xFF69F0AE),
            Color(0xFFFF5252), Color(0xFF40C4FF),
        )
    }
    val particules = remember {
        List(80) {
            Particula(
                x = Random.nextFloat(),
                y0 = -Random.nextFloat() * 0.4f,
                velocitat = 0.7f + Random.nextFloat() * 0.6f,
                color = colors[it % colors.size],
                radi = 4f + Random.nextFloat() * 6f,
            )
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val p = progres.value
        if (p >= 1f) return@Canvas
        particules.forEach { part ->
            val y = (part.y0 + p * part.velocitat * 1.5f) * size.height
            val x = part.x * size.width + sin(p * 8f + part.x * 12f) * 24f
            drawCircle(part.color, part.radi, Offset(x, y))
        }
    }
}
