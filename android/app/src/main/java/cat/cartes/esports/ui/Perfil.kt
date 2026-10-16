package cat.cartes.esports.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.cartes.esports.R
import cat.cartes.esports.data.EstilCarta
import cat.cartes.esports.game.JocViewModel

private val IDIOMES = listOf(
    "ca" to "Català",
    "es" to "Castellano",
    "en" to "English",
    "fr" to "Français",
)

@Composable
fun PantallaPerfil(vm: JocViewModel) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
    ) {
        TitolPantalla(titol = stringResource(R.string.perfil_titol))
        Text(stringResource(R.string.idioma), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IDIOMES.forEach { (codi, nom) ->
                ChipFiltre(nom, vm.idioma == codi, { vm.canviaIdioma(codi) })
            }
        }
        Spacer(Modifier.height(24.dp))

        Text(stringResource(R.string.estil_carta), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChipFiltre(stringResource(R.string.estil_classic), vm.estilCarta == EstilCarta.CLASSIC,
                { vm.canviaEstilCarta(EstilCarta.CLASSIC) })
            ChipFiltre(stringResource(R.string.estil_foto), vm.estilCarta == EstilCarta.FOTO,
                { vm.canviaEstilCarta(EstilCarta.FOTO) })
        }
        Spacer(Modifier.height(24.dp))

        Text(stringResource(R.string.estadistiques), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.est_sobres, vm.estadistica("st_sobres")))
        Text(stringResource(R.string.est_cartes, vm.inventari.size, vm.repo.totalCartes()))
        Text(stringResource(R.string.est_destruides, vm.estadistica("st_destruides")))
        Text(stringResource(R.string.est_fabricades, vm.estadistica("st_fabricades")))
        Spacer(Modifier.height(24.dp))

        Text(stringResource(R.string.credits), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.credits_text),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
