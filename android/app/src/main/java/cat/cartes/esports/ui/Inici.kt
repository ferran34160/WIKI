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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun PantallaInici(vm: JocViewModel, onObreCarta: (String) -> Unit) {
    var resultat by remember { mutableStateOf<ResultatSobre?>(null) }
    val res = resultat
    if (res != null) {
        ObertureSobre(res, vm, onTanca = { resultat = null }, onObreCarta = onObreCarta)
        return
    }

    val guardats = vm.sobres.guardats
    val ms = vm.msFinsProperSobre()
    val minuts = ms / 60_000
    val segons = (ms / 1000) % 60
    val temps = "%02d:%02d".format(minuts, segons)

    val angle = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var obrint by remember { mutableStateOf(false) }
    val obre = {
        if (guardats > 0 && !obrint) {
            obrint = true
            scope.launch {
                repeat(3) {
                    angle.animateTo(7f, tween(60))
                    angle.animateTo(-7f, tween(60))
                }
                angle.animateTo(0f, tween(60))
                resultat = vm.obreSobre()
                obrint = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.obre_sobre),
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.sobre_subtitol, Sobres.CARTES_PER_SOBRE),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        SobreView(
            Modifier
                .fillMaxWidth(0.6f)
                .graphicsLayer { rotationZ = angle.value }
                .clickable(enabled = guardats > 0, onClick = obre),
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = obre, enabled = guardats > 0 && !obrint) {
            Text(
                if (guardats > 0) stringResource(R.string.obre) else stringResource(R.string.sense_sobres),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(12.dp))
        Column(
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 32.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "$guardats / ${Sobres.MAX_GUARDATS}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                if (guardats >= Sobres.MAX_GUARDATS) {
                    stringResource(R.string.sobres_plens)
                } else {
                    stringResource(R.string.proper_sobre, temps)
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val TEMPS_GIR_LLEGENDARIA = 1800

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ObertureSobre(
    res: ResultatSobre,
    vm: JocViewModel,
    onTanca: () -> Unit,
    onObreCarta: (String) -> Unit,
) {
    val total = res.cartes.size
    val pager = rememberPagerState { total }
    val scope = rememberCoroutineScope()
    val vistes = remember(res) { mutableStateListOf(*Array(total) { false }) }
    val extractes = remember(res, vm.idioma) { res.cartes.map { vm.repo.text(it.id, vm.idioma)?.text } }
    val actual = pager.currentPage
    // Les llegendàries giren més a poc a poc; el confeti surt quan ja s'han revelat.
    var llegendariaRevelada by remember(res) { mutableStateOf(false) }
    LaunchedEffect(actual) {
        llegendariaRevelada = false
        vistes[actual] = true
        if (res.cartes[actual].raresa == 5) {
            delay(TEMPS_GIR_LLEGENDARIA.toLong())
            llegendariaRevelada = true
        }
    }
    val queden = vistes.count { !it }
    val resources = LocalContext.current.resources
    val vibracio = LocalHapticFeedback.current
    val flaix = remember { Animatable(0f) }
    LaunchedEffect(llegendariaRevelada) {
        if (llegendariaRevelada) {
            vibracio.performHapticFeedback(HapticFeedbackType.LongPress)
            flaix.snapTo(0.85f)
            flaix.animateTo(0f, tween(800))
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Darrere la carta: raigs daurats quan s'ha revelat una llegendària.
        if (llegendariaRevelada) RaigsDaurats()
        Column(
            Modifier.fillMaxSize().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.carta_n, actual + 1, total),
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 32.dp),
                pageSpacing = 16.dp,
            ) { i ->
                BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val ample = minOf(maxWidth, maxHeight * 0.68f)
                    CartaGirable(
                        carta = res.cartes[i],
                        extracte = extractes[i],
                        nova = res.cartes[i].id in res.noves,
                        girada = vistes[i],
                        modifier = Modifier.width(ample),
                        onClick = { onObreCarta(res.cartes[i].id) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconButton(
                    onClick = { scope.launch { pager.animateScrollToPage(actual - 1) } },
                    enabled = actual > 0,
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(total) { i ->
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (i == actual) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                                ),
                        )
                    }
                }
                IconButton(
                    onClick = { scope.launch { pager.animateScrollToPage(actual + 1) } },
                    enabled = actual < total - 1,
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    if (queden > 0) {
                        val proxima = vistes.indexOfFirst { !it }
                        scope.launch { pager.animateScrollToPage(proxima) }
                    } else {
                        onTanca()
                    }
                },
            ) {
                Text(
                    if (queden > 0) resources.getQuantityString(R.plurals.encara_cartes, queden, queden)
                    else stringResource(R.string.continua),
                    fontSize = 18.sp,
                )
            }
        }
        if (llegendariaRevelada) {
            Confeti()
        }
        if (flaix.value > 0.01f) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flaix.value)))
        }
    }
}

@Composable
private fun CartaGirable(
    carta: Carta,
    extracte: String?,
    nova: Boolean,
    girada: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val rotacio by animateFloatAsState(
        targetValue = if (girada) 0f else 180f,
        animationSpec = tween(if (carta.raresa == 5) TEMPS_GIR_LLEGENDARIA else 550),
        label = "gira",
    )
    val densitat = LocalDensity.current.density
    Box(
        modifier
            .graphicsLayer {
                rotationY = rotacio
                cameraDistance = 12f * densitat
            },
    ) {
        if (rotacio > 90f) {
            // Dors de la carta (es contra-gira perquè no es vegi en mirall)
            Box(
                Modifier
                    .graphicsLayer { rotationY = 180f }
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(5.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("?", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        } else {
            CartaView(carta, extracte = extracte, nova = nova, onClick = onClick)
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
