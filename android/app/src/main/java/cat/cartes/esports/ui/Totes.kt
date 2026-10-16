package cat.cartes.esports.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cat.cartes.esports.R
import cat.cartes.esports.data.Carta
import cat.cartes.esports.game.JocViewModel

private const val PAGINA = 60

/**
 * Catàleg de totes les cartes del joc, amb cerca i filtres. Les que encara no tens es veuen atenuades.
 * Es llegeix de la base de dades per pàgines, perquè amb molts milers de cartes no cap tot a memòria.
 */
@Composable
fun PantallaTotes(vm: JocViewModel, onObreCarta: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var esport by remember { mutableStateOf<String?>(null) }
    var raresa by remember { mutableStateOf<Int?>(null) }
    var nomesFalten by remember { mutableStateOf(false) }

    val excloses = if (nomesFalten) vm.inventari.keys else null
    val total = remember(text, esport, raresa, nomesFalten) {
        vm.repo.catalegTotal(text, esport, raresa, excloses)
    }
    val cartes = remember(text, esport, raresa, nomesFalten) {
        mutableStateListOf<Carta>().also { it.addAll(vm.repo.cataleg(text, esport, raresa, excloses, PAGINA, 0)) }
    }
    val graella = rememberLazyGridState()
    LaunchedEffect(cartes) {
        snapshotFlow { graella.layoutInfo.visibleItemsInfo.lastOrNull()?.index }.collect { ultima ->
            if (ultima != null && ultima >= cartes.size - 12 && cartes.size < total) {
                cartes.addAll(vm.repo.cataleg(text, esport, raresa, excloses, PAGINA, cartes.size))
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        TitolPantalla(
            titol = stringResource(R.string.titol_totes),
            subtitol = stringResource(R.string.cartes_total_n, total),
        )
        CampCerca(text, { text = it }, stringResource(R.string.cerca_carta))
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { ChipFiltre(stringResource(R.string.tots), esport == null, { esport = null }) }
            items(ESPORTS) { e ->
                ChipFiltre(nomEsport(e), esport == e, { esport = if (esport == e) null else e })
            }
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { ChipFiltre(stringResource(R.string.em_falten), nomesFalten, { nomesFalten = !nomesFalten }) }
            items((1..5).toList()) { r ->
                ChipFiltre(nomRaresa(r), raresa == r, { raresa = if (raresa == r) null else r }, punt = colorRaresa(r))
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyVerticalGrid(
            state = graella,
            columns = GridCells.Adaptive(110.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(cartes, key = { it.id }) { c ->
                val quantitat = vm.inventari[c.id] ?: 0
                CartaView(
                    c,
                    modifier = Modifier.alpha(if (quantitat > 0) 1f else 0.4f),
                    compacta = true,
                    quantitat = quantitat,
                    onClick = { onObreCarta(c.id) },
                )
            }
        }
    }
}
