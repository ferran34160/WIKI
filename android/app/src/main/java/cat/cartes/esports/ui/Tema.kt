package cat.cartes.esports.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import cat.cartes.esports.R

private val Esquema = darkColorScheme(
    primary = Color(0xFF3DDC97),
    onPrimary = Color(0xFF03140C),
    secondary = Color(0xFF7C4DFF),
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF141616),
    surfaceVariant = Color(0xFF1D2120),
    outline = Color(0xFF3A4140),
    outlineVariant = Color(0xFF262B2A),
    surfaceContainerLowest = Color(0xFF060606),
    surfaceContainerLow = Color(0xFF0E0F0F),
    surfaceContainer = Color(0xFF111313),
    surfaceContainerHigh = Color(0xFF181B1A),
    surfaceContainerHighest = Color(0xFF1F2322),
    onBackground = Color.White,
    onSurface = Color(0xFFF2F4F3),
    onSurfaceVariant = Color(0xFF9AA3A0),
)

@Composable
fun TemaCartes(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Esquema, content = content)
}

/** Color de cada raresa (1 = comuna ... 5 = llegendària). */
fun colorRaresa(raresa: Int): Color = when (raresa) {
    1 -> Color(0xFF9E9E9E)
    2 -> Color(0xFF4CAF50)
    3 -> Color(0xFF2196F3)
    4 -> Color(0xFF9C27B0)
    else -> Color(0xFFFFB300)
}

@Composable
fun nomRaresa(raresa: Int): String = when (raresa) {
    1 -> stringResource(R.string.raresa_1)
    2 -> stringResource(R.string.raresa_2)
    3 -> stringResource(R.string.raresa_3)
    4 -> stringResource(R.string.raresa_4)
    else -> stringResource(R.string.raresa_5)
}

@Composable
fun nomEsport(esport: String?): String = when (esport) {
    "futbol" -> stringResource(R.string.esport_futbol)
    "basquet" -> stringResource(R.string.esport_basquet)
    "tennis" -> stringResource(R.string.esport_tennis)
    "atletisme" -> stringResource(R.string.esport_atletisme)
    else -> esport.orEmpty()
}

val ESPORTS = listOf("futbol", "basquet", "tennis", "atletisme")
val CLAUS_RARESA = listOf("comuna", "poc_comuna", "rara", "epica", "llegendaria")
