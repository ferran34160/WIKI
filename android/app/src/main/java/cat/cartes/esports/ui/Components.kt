package cat.cartes.esports.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Títol gran de pantalla amb subtítol gris i, a la dreta, una acció opcional (p. ex. «Selecciona»). */
@Composable
fun TitolPantalla(titol: String, subtitol: String? = null, accio: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(titol, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 32.sp)
            if (subtitol != null) {
                Text(subtitol, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        accio?.invoke()
    }
}

/** Camp de cerca fosc, amb cantonades arrodonides i una lupa. */
@Composable
fun CampCerca(valor: String, alCanviar: (String) -> Unit, pista: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = valor,
        onValueChange = alCanviar,
        placeholder = { Text(pista, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Filtre en forma de píndola; quan està triat, es marca amb el color d'accent. */
@Composable
fun ChipFiltre(text: String, triat: Boolean, alTocar: () -> Unit, punt: Color? = null) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        onClick = alTocar,
        shape = RoundedCornerShape(50),
        color = if (triat) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (triat) accent else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (punt != null) {
                Box(Modifier.size(8.dp).background(punt, CircleShape))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text,
                fontSize = 13.sp,
                fontWeight = if (triat) FontWeight.SemiBold else FontWeight.Normal,
                color = if (triat) accent else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** Botó secundari amb vora, per a accions com «Selecciona». */
@Composable
fun BotoVora(text: String, alTocar: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = alTocar,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier,
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Moneda daurada del joc, dibuixada. */
@Composable
fun Moneda(mida: Dp = 20.dp) {
    Canvas(Modifier.size(mida)) {
        val r = size.minDimension / 2
        drawCircle(
            Brush.linearGradient(listOf(Color(0xFFFFE082), Color(0xFFFFB300)), Offset.Zero, Offset(size.width, size.height)),
            r,
        )
        drawCircle(Color(0xFF7A4A00).copy(alpha = 0.55f), r * 0.72f, style = Stroke(r * 0.12f))
        drawCircle(Color(0xFF7A4A00).copy(alpha = 0.55f), r * 0.2f)
    }
}

/** Píndola de dalt amb les monedes, com a WikiMasters. */
@Composable
fun MonedesPill(monedes: Int) {
    Row(
        Modifier
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Moneda(20.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            "$monedes",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
