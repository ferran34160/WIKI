package cat.cartes.esports.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.cartes.esports.R
import cat.cartes.esports.data.Carta
import cat.cartes.esports.game.Economia
import cat.cartes.esports.game.JocViewModel

/** Fitxa d'una carta: tota la informació va dins la carta, ampliada; fora només hi ha els botons. */
@Composable
fun DetallCarta(cartaId: String, vm: JocViewModel, onTorna: () -> Unit) {
    val carta: Carta = remember(cartaId) { vm.repo.carta(cartaId) } ?: return
    val llengua = vm.idioma
    val ctx = LocalContext.current
    val premis = remember(cartaId, llengua) { vm.repo.premis(cartaId, llengua) }
    val marques = remember(cartaId, llengua) { vm.repo.marques(cartaId, llengua) }
    val text = remember(cartaId, llengua) { vm.repo.text(cartaId, llengua) }
    val pais = remember(carta.paisQid, llengua) { vm.repo.etiqueta(carta.paisQid, llengua) }
    val quantitat = vm.inventari[cartaId] ?: 0
    val uri = LocalUriHandler.current
    var confirma by remember { mutableStateOf(false) }

    // Premis i rècords (només si n'hi ha) abans del text breu, tot dins la carta.
    val cos = remember(premis, marques, text, ctx) {
        buildString {
            if (premis.isNotEmpty() || marques.isNotEmpty()) {
                append(ctx.getString(R.string.premis_titol)).append('\n')
                premis.forEach { p ->
                    append("• ")
                    append(if (p.any != null) ctx.getString(R.string.premi_any, p.nom, p.any.take(4)) else p.nom)
                    append('\n')
                }
                marques.forEach { m ->
                    val valor = "%.2f".format(m.valor).trimEnd('0').trimEnd('.') +
                        (m.unitat?.let { " $it" } ?: "")
                    append("• ").append(ctx.getString(R.string.marca_fmt, m.disciplina, valor)).append('\n')
                }
                append('\n')
            }
            text?.let { append(it.text) }
        }.trim().ifEmpty { null }
    }
    val credit = if (carta.fotoAutor != null || carta.fotoLlicencia != null) {
        ctx.getString(R.string.foto_credit, carta.fotoAutor ?: "?", carta.fotoLlicencia ?: "?")
    } else null

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onTorna) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.torna))
            }
            Text(
                if (quantitat > 0) stringResource(R.string.copies, quantitat)
                else stringResource(R.string.no_tens),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val ample = minOf(maxWidth, maxHeight * 0.68f)
            CartaView(
                carta,
                modifier = Modifier.width(ample),
                extracte = cos,
                pais = pais,
                credit = credit,
                onCredit = carta.fotoPagina?.let { pagina -> { uri.openUri(pagina) } },
            )
        }
        if (quantitat > 0) {
            Button(
                modifier = Modifier.padding(vertical = 12.dp),
                onClick = {
                    if (quantitat > 1 && vm.coleccionsEnCurs(cartaId).isEmpty()) {
                        vm.destrueix(carta, 1)
                    } else {
                        confirma = true
                    }
                },
            ) {
                Text(stringResource(R.string.destrueix_btn, Economia.valorDestruccio(carta.raresa)))
            }
        } else {
            Spacer(Modifier.height(12.dp))
        }
    }

    if (confirma) {
        val enCurs = vm.coleccionsEnCurs(cartaId)
        val noms = enCurs.joinToString(", ") { c ->
            // Només necessitem el text; es calcula fora del composable amb etiquetes simples.
            vm.repo.etiqueta(c.clau, llengua) ?: c.clau.orEmpty()
        }
        AlertDialog(
            onDismissRequest = { confirma = false },
            title = { Text(stringResource(R.string.confirma_titol)) },
            text = {
                Column {
                    if (quantitat <= 1) Text(stringResource(R.string.confirma_ultima))
                    if (enCurs.isNotEmpty()) {
                        Text(stringResource(R.string.confirma_coleccions, noms))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.destrueix(carta, 1)
                    confirma = false
                }) { Text(stringResource(R.string.confirma_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { confirma = false }) {
                    Text(stringResource(R.string.cancela))
                }
            },
        )
    }
}
