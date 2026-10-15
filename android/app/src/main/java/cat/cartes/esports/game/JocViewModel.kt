package cat.cartes.esports.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import cat.cartes.esports.data.Carta
import cat.cartes.esports.data.CartaRepository
import cat.cartes.esports.data.Coleccio
import cat.cartes.esports.data.EstatJugador
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

data class ResultatSobre(val cartes: List<Carta>, val noves: Set<String>)

class JocViewModel(app: Application) : AndroidViewModel(app) {

    val repo = CartaRepository(app)
    private val estat = EstatJugador(app)
    private val motor = MotorSobres()
    private val rnd = Random.Default

    var monedes by mutableIntStateOf(0)
        private set
    var sobres by mutableStateOf(EstatSobres(Sobres.MAX_GUARDATS, System.currentTimeMillis()))
        private set
    var ara by mutableLongStateOf(System.currentTimeMillis())
        private set
    var idioma by mutableStateOf("ca")
        private set

    /** id de carta -> quantitat que en tens. */
    var inventari by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    /** Cartes que tens (una per id), per a l'àlbum i les missions. */
    var possedides by mutableStateOf<List<Carta>>(emptyList())
        private set

    val coleccions: List<Coleccio> by lazy { repo.coleccions() }
    private val membres: Map<Long, List<String>> by lazy { repo.membresColeccions() }
    private val raresesDisponibles: Set<Int> by lazy {
        (Raresa.MIN..Raresa.MAX).filter { repo.nombreRaresa(it) > 0 }.toSet()
    }

    init {
        monedes = estat.llegeixLong("monedes", MONEDES_INICIALS.toLong()).toInt()
        val guardats = estat.llegeixLong("sobres_guardats", Sobres.MAX_GUARDATS.toLong()).toInt()
        val tick = estat.llegeixLong("sobres_tick", System.currentTimeMillis())
        sobres = EstatSobres(guardats, tick)
        idioma = estat.llegeixText("idioma") ?: "ca"
        recarregaInventari()
        tick()
    }

    // ---- Rellotge dels sobres ---------------------------------------------

    /** Cal cridar-ho de tant en tant (cada segon, des de la UI). */
    fun tick() {
        ara = System.currentTimeMillis()
        val nou = Sobres.actualitza(sobres, ara)
        if (nou != sobres) {
            sobres = nou
            desaSobres()
        }
    }

    fun msFinsProperSobre(): Long = Sobres.msFinsProper(sobres, ara)

    private fun desaSobres() {
        estat.escriuLong("sobres_guardats", sobres.guardats.toLong())
        estat.escriuLong("sobres_tick", sobres.ultimTick)
    }

    // ---- Sobres -------------------------------------------------------------

    fun obreSobre(): ResultatSobre? {
        tick()
        if (sobres.guardats <= 0 || raresesDisponibles.isEmpty()) return null
        sobres = Sobres.gasta(sobres, ara)
        desaSobres()

        val ids = motor.triaCartes(Sobres.CARTES_PER_SOBRE, raresesDisponibles, rnd) { raresa, excloses ->
            repo.cartaAleatoria(raresa, rnd, excloses)
        }
        val perId = repo.cartes(ids).associateBy { it.id }
        val cartes = ids.mapNotNull { perId[it] }
        val noves = cartes.map { it.id }.filter { (inventari[it] ?: 0) == 0 }.toSet()
        cartes.forEach { estat.canvia(it.id, +1) }

        augmenta("st_sobres")
        val avui = avui()
        if (estat.llegeixText("dia_data") != avui) {
            estat.escriuText("dia_data", avui)
            estat.escriuLong("dia_sobres", 0)
        }
        augmenta("dia_sobres")
        recarregaInventari()
        return ResultatSobre(cartes, noves)
    }

    // ---- Taller ---------------------------------------------------------------

    /** Quantes en pots destruir sense perdre l'última còpia. */
    fun repetides(cartaId: String): Int = ((inventari[cartaId] ?: 0) - 1).coerceAtLeast(0)

    /** Destrueix [quantitat] còpies i retorna les monedes guanyades (0 si no es pot). */
    fun destrueix(carta: Carta, quantitat: Int = 1): Int {
        val tens = inventari[carta.id] ?: 0
        if (quantitat <= 0 || tens < quantitat) return 0
        val guany = Economia.valorDestruccio(carta.raresa) * quantitat
        estat.canvia(carta.id, -quantitat)
        canviaMonedes(+guany)
        augmenta("st_destruides", quantitat)
        recarregaInventari()
        return guany
    }

    /** Destrueix totes les repetides (deixant-ne una de cada). Retorna les monedes guanyades. */
    fun destrueixRepetides(): Int {
        var guany = 0
        var quantes = 0
        for (c in possedides) {
            val n = repetides(c.id)
            if (n > 0) {
                estat.canvia(c.id, -n)
                guany += Economia.valorDestruccio(c.raresa) * n
                quantes += n
            }
        }
        if (guany > 0) {
            canviaMonedes(+guany)
            augmenta("st_destruides", quantes)
            recarregaInventari()
        }
        return guany
    }

    fun monedesPerRepetides(): Int =
        possedides.sumOf { Economia.valorDestruccio(it.raresa) * repetides(it.id) }

    /** Col·leccions on la carta és i en què ja tens alguna altra carta (avís en destruir). */
    fun coleccionsEnCurs(cartaId: String): List<Coleccio> =
        coleccions.filter { col ->
            val ids = membres[col.id].orEmpty()
            cartaId in ids && ids.any { it != cartaId && (inventari[it] ?: 0) > 0 }
        }

    fun fabrica(carta: Carta): Boolean {
        val cost = Economia.costFabricar(carta.raresa)
        if (monedes < cost) return false
        canviaMonedes(-cost)
        estat.canvia(carta.id, +1)
        augmenta("st_fabricades")
        recarregaInventari()
        return true
    }

    // ---- Col·leccions -------------------------------------------------------------

    fun idsColeccio(id: Long): List<String> = membres[id].orEmpty()

    fun progresColeccio(id: Long): Pair<Int, Int> {
        val ids = membres[id].orEmpty()
        return ids.count { (inventari[it] ?: 0) > 0 } to ids.size
    }

    fun recompensaColeccio(id: Long): Int {
        val ids = membres[id].orEmpty()
        val rareses = repo.cartes(ids).map { it.raresa }
        return Economia.recompensaColeccio(rareses)
    }

    fun coleccioReclamada(id: Long): Boolean = estat.reclamada("col:$id")

    fun reclamaColeccio(id: Long): Int {
        val (tens, total) = progresColeccio(id)
        if (total == 0 || tens < total || coleccioReclamada(id)) return 0
        val guany = recompensaColeccio(id)
        estat.marcaReclamada("col:$id")
        canviaMonedes(+guany)
        augmenta("st_colleccions")
        return guany
    }

    private fun coleccionsCompletades(): Int =
        coleccions.count { col ->
            val ids = membres[col.id].orEmpty()
            ids.isNotEmpty() && ids.all { (inventari[it] ?: 0) > 0 }
        }

    // ---- Missions -----------------------------------------------------------------

    fun progres(m: Missio): Int = when (m.tipus) {
        TipusMissio.SOBRES_AVUI ->
            if (estat.llegeixText("dia_data") == avui()) estat.llegeixLong("dia_sobres").toInt() else 0
        TipusMissio.SOBRES_OBERTS -> estat.llegeixLong("st_sobres").toInt()
        TipusMissio.CARTES_DIFERENTS -> inventari.size
        TipusMissio.RARA_O_MES -> possedides.count { it.raresa >= 3 }
        TipusMissio.CARTES_DESTRUIDES -> estat.llegeixLong("st_destruides").toInt()
        TipusMissio.CARTES_FABRICADES -> estat.llegeixLong("st_fabricades").toInt()
        TipusMissio.COLECCIONS_COMPLETADES -> coleccionsCompletades()
    }

    private fun claveMissio(m: Missio) = if (m.diaria) "${m.id}:${avui()}" else m.id

    fun missioReclamada(m: Missio): Boolean = estat.reclamada(claveMissio(m))

    fun reclamaMissio(m: Missio): Int {
        if (missioReclamada(m) || progres(m) < m.objectiu) return 0
        estat.marcaReclamada(claveMissio(m))
        canviaMonedes(+m.recompensa)
        return m.recompensa
    }

    // ---- Estadístiques i perfil ------------------------------------------------

    fun estadistica(clau: String): Int = estat.llegeixLong(clau).toInt()

    fun canviaIdioma(codi: String) {
        idioma = codi
        estat.escriuText("idioma", codi)
    }

    // ---- Interns ---------------------------------------------------------------------

    private fun canviaMonedes(delta: Int) {
        monedes = (monedes + delta).coerceAtLeast(0)
        estat.escriuLong("monedes", monedes.toLong())
    }

    private fun augmenta(clau: String, quantitat: Int = 1) {
        estat.escriuLong(clau, estat.llegeixLong(clau) + quantitat)
    }

    private fun recarregaInventari() {
        inventari = estat.inventari()
        possedides = repo.cartes(inventari.keys)
    }

    private fun avui(): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    companion object {
        const val MONEDES_INICIALS = 100
    }
}
