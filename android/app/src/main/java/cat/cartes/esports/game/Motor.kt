package cat.cartes.esports.game

import kotlin.random.Random

/** Rareses: 1 = comuna ... 5 = llegendària (mateix ordre que exporta_joc.py). */
object Raresa {
    const val MIN = 1
    const val MAX = 5
}

/** Preus i recompenses. Gastar ha de ser més car que guanyar. */
object Economia {
    private val valorDestruccio = intArrayOf(5, 15, 50, 200, 1000)

    /** Monedes que es reben per destruir una carta d'aquesta raresa. */
    fun valorDestruccio(raresa: Int): Int = valorDestruccio[raresa.coerceIn(Raresa.MIN, Raresa.MAX) - 1]

    /** Monedes que costa fabricar una carta concreta. */
    fun costFabricar(raresa: Int): Int = valorDestruccio(raresa) * 5

    /** Premi d'acabar una col·lecció, segons les rareses de les seves cartes. */
    fun recompensaColeccio(rareses: List<Int>): Int =
        rareses.sumOf { valorDestruccio(it) } / 2 + 10
}

/** Tria de les cartes d'un sobre. */
class MotorSobres(
    /** Probabilitat de cada raresa (índex 0 = comuna). Es renormalitza entre les disponibles. */
    private val probabilitats: DoubleArray = doubleArrayOf(0.60, 0.25, 0.10, 0.04, 0.01),
) {
    /**
     * Tria una raresa. [disponibles] són les rareses que tenen almenys una carta;
     * la resta no es poden treure.
     */
    fun triaRaresa(disponibles: Set<Int>, rnd: Random): Int {
        require(disponibles.isNotEmpty()) { "No hi ha cap raresa disponible" }
        val pesos = disponibles.sorted().map { it to probabilitats[it - Raresa.MIN] }
        val total = pesos.sumOf { it.second }
        var tirada = rnd.nextDouble() * total
        for ((raresa, pes) in pesos) {
            tirada -= pes
            if (tirada < 0) return raresa
        }
        return pesos.last().first
    }

    /**
     * Tria les cartes d'un sobre sense repetir-ne cap. [cartaAleatoria] dona una carta d'una raresa
     * que no sigui entre les ja triades (o null si ja no en queden). Si la raresa tirada s'ha
     * esgotat, es prova la més propera; si no en queda cap, el sobre porta menys cartes.
     */
    fun triaCartes(
        quantes: Int,
        disponibles: Set<Int>,
        rnd: Random,
        cartaAleatoria: (raresa: Int, excloses: Set<String>) -> String?,
    ): List<String> {
        val triades = LinkedHashSet<String>()
        repeat(quantes) {
            val tirada = triaRaresa(disponibles, rnd)
            val ordre = disponibles.sortedBy { kotlin.math.abs(it - tirada) }
            for (raresa in ordre) {
                val id = cartaAleatoria(raresa, triades) ?: continue
                triades.add(id)
                break
            }
        }
        return triades.toList()
    }
}

/** Sobres guardats i compte enrere. Tot en mil·lisegons de rellotge. */
data class EstatSobres(val guardats: Int, val ultimTick: Long)

object Sobres {
    const val MAX_GUARDATS = 3
    const val CARTES_PER_SOBRE = 5
    const val MS_PER_SOBRE = 10 * 60 * 1000L

    /** Afegeix els sobres generats des de l'últim tick. Plens, el rellotge no avança. */
    fun actualitza(e: EstatSobres, ara: Long): EstatSobres {
        if (e.guardats >= MAX_GUARDATS) return EstatSobres(MAX_GUARDATS, ara)
        val generats = ((ara - e.ultimTick) / MS_PER_SOBRE).toInt().coerceAtLeast(0)
        if (generats == 0) return e
        val nous = e.guardats + generats
        return if (nous >= MAX_GUARDATS) {
            EstatSobres(MAX_GUARDATS, ara)
        } else {
            EstatSobres(nous, e.ultimTick + generats * MS_PER_SOBRE)
        }
    }

    /** Si en gasta un i estaven plens, el compte enrere comença ara. */
    fun gasta(e: EstatSobres, ara: Long): EstatSobres {
        require(e.guardats > 0) { "No hi ha sobres" }
        val inici = if (e.guardats >= MAX_GUARDATS) ara else e.ultimTick
        return EstatSobres(e.guardats - 1, inici)
    }

    /** Mil·lisegons que falten pel proper sobre (0 si ja n'hi ha el màxim). */
    fun msFinsProper(e: EstatSobres, ara: Long): Long {
        if (e.guardats >= MAX_GUARDATS) return 0L
        val passat = (ara - e.ultimTick).coerceIn(0L, MS_PER_SOBRE)
        return MS_PER_SOBRE - passat
    }
}

enum class TipusMissio {
    SOBRES_AVUI, SOBRES_OBERTS, CARTES_DIFERENTS, RARA_O_MES, CARTES_DESTRUIDES,
    CARTES_FABRICADES, COLECCIONS_COMPLETADES,
}

data class Missio(
    val id: String,
    val tipus: TipusMissio,
    val objectiu: Int,
    val recompensa: Int,
    /** Les diàries es poden tornar a reclamar cada dia. */
    val diaria: Boolean = false,
)

object Missions {
    val totes: List<Missio> = listOf(
        Missio("d_sobres_3", TipusMissio.SOBRES_AVUI, 3, 40, diaria = true),
        Missio("sobres_10", TipusMissio.SOBRES_OBERTS, 10, 60),
        Missio("sobres_50", TipusMissio.SOBRES_OBERTS, 50, 250),
        Missio("diferents_10", TipusMissio.CARTES_DIFERENTS, 10, 50),
        Missio("diferents_50", TipusMissio.CARTES_DIFERENTS, 50, 300),
        Missio("rara_1", TipusMissio.RARA_O_MES, 1, 60),
        Missio("rara_5", TipusMissio.RARA_O_MES, 5, 250),
        Missio("destruir_5", TipusMissio.CARTES_DESTRUIDES, 5, 30),
        Missio("fabricar_1", TipusMissio.CARTES_FABRICADES, 1, 40),
        Missio("coleccio_1", TipusMissio.COLECCIONS_COMPLETADES, 1, 100),
        Missio("coleccio_5", TipusMissio.COLECCIONS_COMPLETADES, 5, 500),
    )
}
