package cat.cartes.esports.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.cartes.esports.R
import cat.cartes.esports.data.Carta
import cat.cartes.esports.game.Economia
import cat.cartes.esports.game.JocViewModel

@Composable
fun PantallaTaller(vm: JocViewModel) {
    var pestanya by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = pestanya == 0,
                onClick = { pestanya = 0 },
                label = { Text(stringResource(R.string.taller_destruir)) },
            )
            FilterChip(
                selected = pestanya == 1,
                onClick = { pestanya = 1 },
                label = { Text(stringResource(R.string.taller_fabricar)) },
            )
        }
        Spacer(Modifier.height(8.dp))
        if (pestanya == 0) TallerDestruir(vm) else TallerFabricar(vm)
    }
}

@Composable
private fun TallerDestruir(vm: JocViewModel) {
    val repetides = vm.possedides.filter { vm.repetides(it.id) > 0 }
        .sortedByDescending { it.raresa }
    if (repetides.isEmpty()) {
        Text(stringResource(R.string.sense_repetides), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Button(
        onClick = { vm.destrueixRepetides() },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.destrueix_repetides, vm.monedesPerRepetides()))
    }
    Spacer(Modifier.height(8.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(repetides, key = { it.id }) { c ->
            val n = vm.repetides(c.id)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(c.nom, fontSize = 15.sp)
                    Text(
                        "${nomRaresa(c.raresa)} · ×$n",
                        color = colorRaresa(c.raresa),
                        fontSize = 12.sp,
                    )
                }
                Button(onClick = { vm.destrueix(c, 1) }) {
                    Text("+${Economia.valorDestruccio(c.raresa)}")
                }
            }
        }
    }
}

@Composable
private fun TallerFabricar(vm: JocViewModel) {
    var text by remember { mutableStateOf("") }
    val resultats: List<Carta> = remember(text) {
        if (text.trim().length < 2) emptyList() else vm.repo.cerca(text, 30)
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(stringResource(R.string.cerca_fabricar)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(resultats, key = { it.id }) { c ->
            val cost = Economia.costFabricar(c.raresa)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(c.nom, fontSize = 15.sp)
                    Text(nomRaresa(c.raresa), color = colorRaresa(c.raresa), fontSize = 12.sp)
                }
                Button(onClick = { vm.fabrica(c) }, enabled = vm.monedes >= cost) {
                    Text(stringResource(R.string.fabrica_btn, cost))
                }
            }
        }
    }
}
