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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cat.cartes.esports.R
import cat.cartes.esports.game.JocViewModel

@Composable
fun PantallaAlbum(vm: JocViewModel, onObreCarta: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var esport by remember { mutableStateOf<String?>(null) }
    var raresa by remember { mutableStateOf<Int?>(null) }

    val cartes = vm.possedides
        .filter { c ->
            (esport == null || c.esport == esport) &&
                (raresa == null || c.raresa == raresa) &&
                (text.isBlank() || c.nom.contains(text.trim(), ignoreCase = true))
        }
        .sortedWith(compareByDescending<cat.cartes.esports.data.Carta> { it.raresa }
            .thenByDescending { it.visites })

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text(stringResource(R.string.cerca_carta)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = esport == null,
                    onClick = { esport = null },
                    label = { Text(stringResource(R.string.tots)) },
                )
            }
            items(ESPORTS) { e ->
                FilterChip(
                    selected = esport == e,
                    onClick = { esport = if (esport == e) null else e },
                    label = { Text(nomEsport(e)) },
                )
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items((1..5).toList()) { r ->
                FilterChip(
                    selected = raresa == r,
                    onClick = { raresa = if (raresa == r) null else r },
                    label = { Text(nomRaresa(r)) },
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.cartes_diferents, vm.possedides.size),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))

        if (vm.possedides.isEmpty()) {
            Text(stringResource(R.string.album_buit))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(110.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(cartes, key = { it.id }) { c ->
                    CartaView(
                        c,
                        compacta = true,
                        quantitat = vm.inventari[c.id] ?: 0,
                        onClick = { onObreCarta(c.id) },
                    )
                }
            }
        }
    }
}
