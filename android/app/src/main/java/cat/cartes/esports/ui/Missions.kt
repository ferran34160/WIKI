package cat.cartes.esports.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.cartes.esports.R
import cat.cartes.esports.game.JocViewModel
import cat.cartes.esports.game.Missio
import cat.cartes.esports.game.Missions
import cat.cartes.esports.game.TipusMissio

@Composable
private fun textMissio(m: Missio): String = when (m.tipus) {
    TipusMissio.SOBRES_AVUI -> stringResource(R.string.missio_sobres_avui, m.objectiu)
    TipusMissio.SOBRES_OBERTS -> stringResource(R.string.missio_sobres_oberts, m.objectiu)
    TipusMissio.CARTES_DIFERENTS -> stringResource(R.string.missio_diferents, m.objectiu)
    TipusMissio.RARA_O_MES -> stringResource(R.string.missio_rara, m.objectiu)
    TipusMissio.CARTES_DESTRUIDES -> stringResource(R.string.missio_destruir, m.objectiu)
    TipusMissio.CARTES_FABRICADES -> stringResource(R.string.missio_fabricar, m.objectiu)
}

@Composable
fun PantallaMissions(vm: JocViewModel) {
    // Llegir l'inventari i les monedes fa que la pantalla es refresqui quan canvien.
    @Suppress("UNUSED_VARIABLE") val dependencia = vm.inventari.size + vm.monedes + vm.sobres.guardats

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
    TitolPantalla(titol = stringResource(R.string.tab_missions))
    LazyColumn(
        Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(Missions.totes, key = { it.id }) { m ->
            val progres = vm.progres(m).coerceAtMost(m.objectiu)
            val reclamada = vm.missioReclamada(m)
            val acabada = progres >= m.objectiu
            Card(
                Modifier.fillMaxWidth().alpha(if (reclamada) 0.45f else 1f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(textMissio(m), fontSize = 15.sp)
                            if (m.diaria) {
                                Text(
                                    stringResource(R.string.diaria),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                        when {
                            reclamada -> Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    stringResource(R.string.reclamada),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            acabada -> Button(onClick = { vm.reclamaMissio(m) }) {
                                Text(stringResource(R.string.reclama, m.recompensa))
                            }
                            else -> Text(
                                "+${m.recompensa}",
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progres.toFloat() / m.objectiu },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(R.string.progres, progres, m.objectiu),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
    }
}
