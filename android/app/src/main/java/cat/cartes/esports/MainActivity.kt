package cat.cartes.esports

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import cat.cartes.esports.ui.MonedesPill
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

/** Capa opaca que tapa la pantalla de sota i en bloqueja els tocs. */
@Composable
private fun CapaSobre(contingut: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) { detectTapGestures { } },
    ) { contingut() }
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
                MonedesPill(vm.monedes)
                IconButton(
                    onClick = { perfilObert = !perfilObert },
                    modifier = Modifier
                        .background(
                            if (perfilObert) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surface,
                            CircleShape,
                        ),
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = stringResource(R.string.perfil_titol),
                        tint = if (perfilObert) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.drawBehind {
                    drawLine(
                        Color(0xFF262B2A), Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx(),
                    )
                },
            ) {
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
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent,
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        // La pantalla de la pestanya es manté sempre; el perfil i la fitxa d'una carta es mostren a sobre.
        // Així, en tornar enrere, tot queda exactament com estava (el sobre que s'obria, els filtres, la selecció...).
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                when (pestanya) {
                    0 -> PantallaInici(vm, onObreCarta = { cartaOberta = it })
                    1 -> PantallaAlbum(vm, onObreCarta = { cartaOberta = it })
                    2 -> PantallaTotes(vm, onObreCarta = { cartaOberta = it })
                    3 -> PantallaMissions(vm)
                    else -> PantallaTaller(vm)
                }
            }
            if (perfilObert) CapaSobre { PantallaPerfil(vm) }
            val carta = cartaOberta
            if (carta != null) CapaSobre { DetallCarta(carta, vm, onTorna = { cartaOberta = null }) }
        }
    }
}
