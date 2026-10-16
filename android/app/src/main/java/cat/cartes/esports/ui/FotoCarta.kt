package cat.cartes.esports.ui

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.media.FaceDetector
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Proporció (ample/alt) d'una foto i, si s'hi ha trobat una cara, on és (0..1 en cada eix). */
private data class InfoFoto(val proporcio: Float, val cara: Offset?)

/** Resultat per URL, perquè no es torni a detectar cada vegada que la carta es dibuixa. */
private val cache = ConcurrentHashMap<String, InfoFoto>()

/**
 * Busca la cara més clara de la foto amb el detector del sistema (sense biblioteques ni xarxa).
 * Retorna el centre del cap, o null si no hi ha cap cara fiable.
 */
private fun detectaCara(foto: Bitmap): Offset? {
    val escala = minOf(1f, 240f / max(foto.width, foto.height))
    val w = (foto.width * escala).toInt() and 1.inv() // el detector necessita un ample parell
    val h = (foto.height * escala).toInt()
    if (w < 16 || h < 16) return null
    val reduida = Bitmap.createScaledBitmap(foto, w, h, true).copy(Bitmap.Config.RGB_565, false) ?: return null
    return try {
        val trobades = arrayOfNulls<FaceDetector.Face>(1)
        if (FaceDetector(w, h, 1).findFaces(reduida, trobades) == 0) return null
        val cara = trobades[0] ?: return null
        if (cara.confidence() < 0.4f) return null
        val ulls = PointF().also { cara.getMidPoint(it) }
        // El centre del cap és una mica per sota de la línia dels ulls.
        Offset(ulls.x / w, (ulls.y + 0.3f * cara.eyesDistance()) / h)
    } finally {
        reduida.recycle()
    }
}

/** Alineació que centra [focus] a la finestra visible quan la foto es retalla (Crop) dins l'espai. */
private fun alineacio(proporcio: Float, focus: Offset?, ample: Float, alt: Float): BiasAlignment {
    if (ample <= 0f || alt <= 0f) return BiasAlignment(0f, -0.5f)
    // La foto escalada fins a cobrir l'espai, amb alçada «escala» i amplada proporcio * escala.
    val escala = max(ample / proporcio, alt)
    val fotoAmple = proporcio * escala
    val fotoAlt = escala
    fun eix(foto: Float, finestra: Float, punt: Float?, perDefecte: Float): Float {
        val sobra = foto - finestra
        if (sobra < 1f) return 0f
        if (punt == null) return perDefecte
        val fraccio = ((punt * foto - finestra / 2f) / sobra).coerceIn(0f, 1f)
        return 2f * fraccio - 1f
    }
    // Sense cara: la majoria de retrats tenen el cap al terç superior.
    return BiasAlignment(eix(fotoAmple, ample, focus?.x, 0f), eix(fotoAlt, alt, focus?.y, -0.5f))
}

/** Foto de la carta: es retalla per omplir l'espai, però centrada en la cara si se'n troba una. */
@Composable
fun FotoCarta(url: String, descripcio: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var info by remember(url) { mutableStateOf(cache[url]) }
    // Sense maquinari: el detector necessita poder llegir els píxels.
    val peticio = remember(url) { ImageRequest.Builder(context).data(url).allowHardware(false).build() }

    BoxWithConstraints(modifier) {
        val ample = constraints.maxWidth.toFloat()
        val alt = constraints.maxHeight.toFloat()
        val actual = info
        val alinia = if (actual == null) BiasAlignment(0f, -0.5f)
        else alineacio(actual.proporcio, actual.cara, ample, alt)
        AsyncImage(
            model = peticio,
            contentDescription = descripcio,
            contentScale = ContentScale.Crop,
            alignment = alinia,
            modifier = Modifier.fillMaxSize(),
            onSuccess = { estat ->
                val foto = (estat.result.drawable as? BitmapDrawable)?.bitmap
                if (foto != null && cache[url] == null) {
                    val proporcio = foto.width.toFloat() / foto.height
                    scope.launch {
                        val cara = withContext(Dispatchers.Default) { runCatching { detectaCara(foto) }.getOrNull() }
                        val nova = InfoFoto(proporcio, cara)
                        cache[url] = nova
                        info = nova
                    }
                }
            },
        )
    }
}
