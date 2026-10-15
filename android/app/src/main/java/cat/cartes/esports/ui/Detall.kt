package cat.cartes.esports.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

/** Fitxa completa d'una carta, amb les dades en l'ordre acordat: premis abans del text breu. */
@Composable
fun DetallCarta(cartaId: String, vm: JocViewModel) {
    val carta: Carta = remember(cartaId) { vm.repo.carta(cartaId) } ?: return
    val llengua = vm.idioma
    val premis = remember(cartaId, llengua) { vm.repo.premis(cartaId, llengua) }
    val marques = remember(cartaId, llengua) { vm.repo.marques(cartaId, llengua) }
    val text = remember(cartaId, llengua) { vm.repo.text(cartaId, llengua) }
    val pais = remember(carta.paisQid, llengua) { vm.repo.etiqueta(carta.paisQid, llengua) }
    val quantitat = vm.inventari[cartaId] ?: 0
    val uri = LocalUriHandler.current
    var confirma by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CartaView(carta, modifier = Modifier.fillMaxWidth(0.65f))
        Spacer(Modifier.height(12.dp))
        Text(
            if (quantitat > 0) stringResource(R.string.copies, quantitat)
            else stringResource(R.string.no_tens),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(carta.nom, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            carta.naixement?.let { Text(stringResource(R.string.nascut, it)) }
            carta.defuncio?.let { Text(stringResource(R.string.mort, it)) }
            carta.alcadaM?.let { Text(stringResource(R.string.alcada, "%.2f".format(it))) }
            pais?.takeIf { it.isNotBlank() }?.let { Text(it) }

            // Premis i rècords: només si n'hi ha
            if (premis.isNotEmpty() || marques.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.premis_titol),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                premis.forEach { p ->
                    Text(
                        if (p.any != null) stringResource(R.string.premi_any, p.nom, p.any.take(4))
                        else p.nom,
                    )
                }
                marques.forEach { m ->
                    val valor = "%.2f".format(m.valor).trimEnd('0').trimEnd('.') +
                        (m.unitat?.let { " $it" } ?: "")
                    Text(stringResource(R.string.marca_fmt, m.disciplina, valor))
                }
            }

            // Text breu
            if (text != null) {
                Spacer(Modifier.height(8.dp))
                Text(text.text)
                Text(
                    stringResource(R.string.text_font, text.llengua),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(enabled = text.url != null) {
                        text.url?.let { uri.openUri(it) }
                    },
                )
            }

            // Atribució de la foto (obligatòria per la llicència)
            if (carta.fotoAutor != null || carta.fotoLlicencia != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(
                        R.string.foto_credit,
                        carta.fotoAutor ?: "?",
                        carta.fotoLlicencia ?: "?",
                    ),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(enabled = carta.fotoPagina != null) {
                        carta.fotoPagina?.let { uri.openUri(it) }
                    },
                )
            }
        }

        if (quantitat > 0) {
            Spacer(Modifier.height(16.dp))
            Button(
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
