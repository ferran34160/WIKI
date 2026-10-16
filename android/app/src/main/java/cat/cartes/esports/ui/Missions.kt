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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.cartes.esports.R
import cat.cartes.esports.game.Exit
import cat.cartes.esports.game.Exits
import cat.cartes.esports.game.JocViewModel
import cat.cartes.esports.game.TipusExit
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

/** Targeta de progrés comuna a missions i èxits; quan ja s'ha cobrat, queda atenuada. */
@Composable
private fun TargetaProgres(
    text: String,
    nota: String?,
    progres: Int,
    objectiu: Int,
    recompensa: Int,
    reclamada: Boolean,
    alReclamar: () -> Unit,
) {
    val acabada = progres >= objectiu
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
                    Text(text, fontSize = 15.sp)
                    if (nota != null) {
                        Text(nota, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
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
                    acabada -> Button(onClick = alReclamar) {
                        Text(stringResource(R.string.reclama, recompensa))
                    }
                    else -> Text("+$recompensa", color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progres.toFloat() / objectiu },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.progres, progres, objectiu),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun textExit(e: Exit): String = when (e.tipus) {
    TipusExit.CARTES -> stringResource(R.string.exit_cartes, e.objectiu)
    TipusExit.LLEGENDARIES -> stringResource(R.string.exit_llegendaries, e.objectiu)
    TipusExit.EPIQUES_O_MES -> stringResource(R.string.exit_epiques, e.objectiu)
    TipusExit.ESPORT -> stringResource(R.string.exit_esport, e.objectiu, nomEsport(e.esport))
    TipusExit.SOBRES -> stringResource(R.string.missio_sobres_oberts, e.objectiu)
    TipusExit.DESTRUIDES -> stringResource(R.string.missio_destruir, e.objectiu)
    TipusExit.FABRICADES -> stringResource(R.string.missio_fabricar, e.objectiu)
}

@Composable
fun PantallaMissions(vm: JocViewModel) {
    // Llegir l'inventari i les monedes fa que la pantalla es refresqui quan canvien.
    @Suppress("UNUSED_VARIABLE") val dependencia = vm.inventari.size + vm.monedes + vm.sobres.guardats
    var veuExits by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        TitolPantalla(titol = stringResource(if (veuExits) R.string.tab_exits else R.string.tab_missions))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipFiltre(stringResource(R.string.tab_missions), !veuExits, { veuExits = false })
            ChipFiltre(stringResource(R.string.tab_exits), veuExits, { veuExits = true })
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!veuExits) {
                items(Missions.totes, key = { "m_" + it.id }) { m ->
                    TargetaProgres(
                        text = textMissio(m),
                        nota = if (m.diaria) stringResource(R.string.diaria) else null,
                        progres = vm.progres(m).coerceAtMost(m.objectiu),
                        objectiu = m.objectiu,
                        recompensa = m.recompensa,
                        reclamada = vm.missioReclamada(m),
                        alReclamar = { vm.reclamaMissio(m) },
                    )
                }
            } else {
                // Els èxits pendents primer (els que ja es poden cobrar, a dalt de tot) i els cobrats al final.
                val ordenats = Exits.tots.sortedBy { e ->
                    when {
                        vm.exitReclamat(e) -> 2
                        vm.progresExit(e) >= e.objectiu -> 0
                        else -> 1
                    }
                }
                items(ordenats, key = { "e_" + it.id }) { e ->
                    TargetaProgres(
                        text = textExit(e),
                        nota = null,
                        progres = vm.progresExit(e).coerceAtMost(e.objectiu),
                        objectiu = e.objectiu,
                        recompensa = e.recompensa,
                        reclamada = vm.exitReclamat(e),
                        alReclamar = { vm.reclamaExit(e) },
                    )
                }
            }
        }
    }
}
