package cat.cartes.esports.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.cartes.esports.R
import cat.cartes.esports.data.Coleccio
import cat.cartes.esports.game.JocViewModel

@Composable
fun nomColeccio(c: Coleccio, vm: JocViewModel): String {
    val esport = nomEsport(c.esport)
    val etiqueta = vm.repo.etiqueta(c.clau, vm.idioma) ?: "?"
    return when (c.tipus) {
        "club" -> stringResource(R.string.col_club, etiqueta)
        "pais" -> stringResource(R.string.col_pais, esport, etiqueta)
        "premi" -> stringResource(R.string.col_premi, etiqueta)
        "decada" -> stringResource(R.string.col_decada, esport, c.clau.orEmpty())
        "esport_raresa" -> {
            val parts = c.clau.orEmpty().split(":")
            val indexRaresa = CLAUS_RARESA.indexOf(parts.getOrNull(1)) + 1
            stringResource(
                R.string.col_esport_raresa,
                nomEsport(parts.getOrNull(0)),
                nomRaresa(if (indexRaresa > 0) indexRaresa else 1),
            )
        }
        else -> c.clau.orEmpty()
    }
}

@Composable
fun PantallaColleccions(vm: JocViewModel, onObre: (Long) -> Unit) {
    if (vm.coleccions.isEmpty()) {
        Text(stringResource(R.string.colleccions_buit), modifier = Modifier.padding(16.dp))
        return
    }
    // Les que tens a mitges primer, després les altres.
    val ordenades = vm.coleccions.sortedByDescending { c ->
        val (tens, total) = vm.progresColeccio(c.id)
        when {
            total > 0 && tens >= total -> 0.5f      // completes: al mig
            tens > 0 -> 1f + tens.toFloat() / total // en curs: primer
            else -> 0f
        }
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(ordenades, key = { it.id }) { c ->
            val (tens, total) = vm.progresColeccio(c.id)
            Card(
                Modifier.fillMaxWidth().clickable { onObre(c.id) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(nomColeccio(c, vm), fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { if (total == 0) 0f else tens.toFloat() / total },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.progres, tens, total),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun DetallColeccio(
    id: Long,
    vm: JocViewModel,
    onObreCarta: (String) -> Unit,
) {
    val coleccio = vm.coleccions.firstOrNull { it.id == id } ?: return
    val ids = remember(id) { vm.idsColeccio(id) }
    val cartes = remember(id) {
        vm.repo.cartes(ids).sortedWith(compareByDescending<cat.cartes.esports.data.Carta> { it.raresa }
            .thenByDescending { it.visites })
    }
    val (tens, total) = vm.progresColeccio(id)
    val completa = total > 0 && tens >= total
    val reclamada = vm.coleccioReclamada(id)

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Text(nomColeccio(coleccio, vm), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else tens.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.progres, tens, total))
            if (completa) {
                if (reclamada) {
                    Text(stringResource(R.string.reclamada), color = MaterialTheme.colorScheme.primary)
                } else {
                    Button(onClick = { vm.reclamaColeccio(id) }) {
                        Text(stringResource(R.string.reclama, vm.recompensaColeccio(id)))
                    }
                }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(100.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(cartes, key = { it.id }) { c ->
                if ((vm.inventari[c.id] ?: 0) > 0) {
                    CartaView(c, compacta = true, onClick = { onObreCarta(c.id) })
                } else {
                    CartaSilueta(c.raresa)
                }
            }
        }
    }
}
