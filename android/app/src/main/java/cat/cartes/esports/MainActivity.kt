package cat.cartes.esports

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cat.cartes.esports.game.JocViewModel
import cat.cartes.esports.ui.DetallCarta
import cat.cartes.esports.ui.LocalEstilCarta
import cat.cartes.esports.ui.PantallaAlbum
import cat.cartes.esports.ui.PantallaInici
import cat.cartes.esports.ui.PantallaMissions
import cat.cartes.esports.ui.PantallaPerfil
import cat.cartes.esports.ui.PantallaTaller
import cat.cartes.esports.ui.PantallaTotes
import cat.cartes.esports.ui.TemaCartes
import java.util.Locale
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val vm: JocViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TemaCartes {
                // L'idioma de l'app es tria dins el joc, independent del del mòbil.
                val base = LocalContext.current
                val config = remember(vm.idioma, base) {
                    Configuration(base.resources.configuration).apply {
                        setLocale(Locale(vm.idioma))
                    }
                }
                val ctx = remember(config, base) { base.createConfigurationContext(config) }
                CompositionLocalProvider(
                    LocalContext provides ctx,
                    LocalConfiguration provides config,
                    LocalEstilCarta provides vm.estilCarta,
                ) {
                    ArrelJoc(vm)
                }
            }
        }
    }
}

private data class Pestanya(val titol: Int, val icona: ImageVector)

private val PESTANYES = listOf(
    Pestanya(R.string.tab_inici, Icons.Default.Home),
    Pestanya(R.string.tab_album, Icons.Default.Favorite),
    Pestanya(R.string.tab_totes, Icons.Default.Search),
    Pestanya(R.string.tab_missions, Icons.Default.CheckCircle),
    Pestanya(R.string.tab_taller, Icons.Default.Build),
)

@Composable
private fun ArrelJoc(vm: JocViewModel) {
    var pestanya by rememberSaveable { mutableIntStateOf(0) }
    var cartaOberta by rememberSaveable { mutableStateOf<String?>(null) }
    var perfilObert by rememberSaveable { mutableStateOf(false) }

    // Rellotge dels sobres: s'actualitza cada segon mentre l'app és visible.
    LaunchedEffect(Unit) {
        while (true) {
            vm.tick()
            delay(1000)
        }
    }

    BackHandler(enabled = cartaOberta != null || perfilObert) {
        when {
            cartaOberta != null -> cartaOberta = null
            else -> perfilObert = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.monedes_n, vm.monedes),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                IconButton(onClick = { perfilObert = !perfilObert }) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = stringResource(R.string.perfil_titol),
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar {
                PESTANYES.forEachIndexed { i, p ->
                    NavigationBarItem(
                        selected = pestanya == i && !perfilObert,
                        onClick = {
                            pestanya = i
                            cartaOberta = null
                            perfilObert = false
                        },
                        icon = { Icon(p.icona, contentDescription = null) },
                        label = { Text(stringResource(p.titol), fontSize = 11.sp, maxLines = 1) },
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val carta = cartaOberta
            when {
                carta != null -> DetallCarta(carta, vm, onTorna = { cartaOberta = null })
                perfilObert -> PantallaPerfil(vm)
                pestanya == 0 -> PantallaInici(vm, onObreCarta = { cartaOberta = it })
                pestanya == 1 -> PantallaAlbum(vm, onObreCarta = { cartaOberta = it })
                pestanya == 2 -> PantallaTotes(vm, onObreCarta = { cartaOberta = it })
                pestanya == 3 -> PantallaMissions(vm)
                else -> PantallaTaller(vm)
            }
        }
    }
}
