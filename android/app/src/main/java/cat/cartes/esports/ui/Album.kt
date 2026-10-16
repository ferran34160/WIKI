package cat.cartes.esports.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.cartes.esports.R
import cat.cartes.esports.data.Carta
import cat.cartes.esports.game.JocViewModel

@Composable
fun PantallaAlbum(vm: JocViewModel, onObreCarta: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var esport by remember { mutableStateOf<String?>(null) }
    var raresa by remember { mutableStateOf<Int?>(null) }
    var seleccionant by remember { mutableStateOf(false) }
    var seleccio by remember { mutableStateOf(setOf<String>()) }
    var confirma by remember { mutableStateOf(false) }

    val cartes = vm.possedides
        .filter { c ->
            (esport == null || c.esport == esport) &&
                (raresa == null || c.raresa == raresa) &&
                (text.isBlank() || c.nom.contains(text.trim(), ignoreCase = true))
        }
        .sortedWith(compareByDescending<Carta> { it.raresa }.thenByDescending { it.visites })

    val totesTriades = cartes.isNotEmpty() && cartes.all { it.id in seleccio }

    fun surtDeSeleccio() {
        seleccionant = false
        seleccio = emptySet()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        TitolPantalla(
            titol = stringResource(R.string.tab_album),
            subtitol = if (seleccionant) stringResource(R.string.n_seleccionades, seleccio.size)
            else stringResource(R.string.cartes_diferents, vm.possedides.size),
            accio = {
                if (vm.possedides.isNotEmpty()) {
                    BotoVora(
                        if (seleccionant) stringResource(R.string.cancela) else stringResource(R.string.selecciona),
                        alTocar = { if (seleccionant) surtDeSeleccio() else seleccionant = true },
                    )
                }
            },
        )
        CampCerca(text, { text = it }, stringResource(R.string.cerca_carta))
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                ChipFiltre(stringResource(R.string.tots), esport == null, { esport = null })
            }
            items(ESPORTS) { e ->
                ChipFiltre(nomEsport(e), esport == e, { esport = if (esport == e) null else e })
            }
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (seleccionant) {
                item {
                    ChipFiltre(
                        if (totesTriades) stringResource(R.string.deselecciona_totes)
                        else stringResource(R.string.selecciona_totes),
                        triat = totesTriades,
                        alTocar = {
                            val mostrades = cartes.map { it.id }.toSet()
                            seleccio = if (totesTriades) seleccio - mostrades else seleccio + mostrades
                        },
                    )
                }
                item {
                    ChipFiltre(
                        stringResource(R.string.selecciona_repetides),
                        triat = false,
                        alTocar = { seleccio = vm.possedides.filter { vm.repetides(it.id) > 0 }.map { it.id }.toSet() },
                    )
                }
            }
            items((1..5).toList()) { r ->
                ChipFiltre(
                    nomRaresa(r), raresa == r, { raresa = if (raresa == r) null else r },
                    punt = colorRaresa(r),
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        if (vm.possedides.isEmpty()) {
            Text(stringResource(R.string.album_buit), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyVerticalGrid(
                modifier = Modifier.weight(1f),
                columns = GridCells.Adaptive(110.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(cartes, key = { it.id }) { c ->
                    val triada = c.id in seleccio
                    Box(
                        Modifier.clickable(enabled = seleccionant) {
                            seleccio = if (triada) seleccio - c.id else seleccio + c.id
                        },
                    ) {
                        CartaView(
                            c,
                            modifier = Modifier.alpha(if (seleccionant && !triada) 0.55f else 1f),
                            compacta = true,
                            quantitat = vm.inventari[c.id] ?: 0,
                            onClick = if (seleccionant) null else ({ onObreCarta(c.id) }),
                        )
                        if (seleccionant) {
                            Box(
                                Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                                    .size(24.dp)
                                    .background(
                                        if (triada) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.55f),
                                        CircleShape,
                                    )
                                    .border(
                                        2.dp,
                                        if (triada) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.8f),
                                        CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (triada) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (seleccionant && seleccio.isNotEmpty()) {
            Button(
                onClick = { confirma = true },
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE5484D),
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    stringResource(R.string.destruir_n, seleccio.size, vm.monedesSeleccio(seleccio)),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }

    if (confirma) {
        val perdudes = seleccio.count { (vm.inventari[it] ?: 0) <= 1 }
        AlertDialog(
            onDismissRequest = { confirma = false },
            title = { Text(stringResource(R.string.confirma_titol)) },
            text = {
                Column {
                    Text(stringResource(R.string.confirma_multi, seleccio.size, vm.monedesSeleccio(seleccio)))
                    if (perdudes > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.confirma_multi_perdudes, perdudes),
                            color = Color(0xFFE5484D),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.destrueixSeleccio(seleccio)
                    confirma = false
                    surtDeSeleccio()
                }) { Text(stringResource(R.string.confirma_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { confirma = false }) { Text(stringResource(R.string.cancela)) }
            },
        )
    }
}
