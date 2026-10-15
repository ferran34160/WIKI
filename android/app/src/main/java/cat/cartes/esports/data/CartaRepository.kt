package cat.cartes.esports.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.File
import kotlin.random.Random

/**
 * Accés de només lectura a joc_cartes.sqlite (generat per exporta_joc.py).
 * El fitxer ve dins els assets i es copia a la carpeta de bases de dades
 * la primera vegada i cada cop que canvia la versió de l'app.
 */
class CartaRepository(context: Context) {

    private val db: SQLiteDatabase = obre(context.applicationContext)

    private fun obre(context: Context): SQLiteDatabase {
        val fitxer: File = context.getDatabasePath(NOM_BD)
        val prefs = context.getSharedPreferences("repo", Context.MODE_PRIVATE)
        @Suppress("DEPRECATION")
        val versioApp = context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        if (!fitxer.exists() || prefs.getInt("versio_app", -1) != versioApp) {
            fitxer.parentFile?.mkdirs()
            context.assets.open(NOM_BD).use { entrada ->
                fitxer.outputStream().use { sortida -> entrada.copyTo(sortida) }
            }
            prefs.edit().putInt("versio_app", versioApp).apply()
        }
        return SQLiteDatabase.openDatabase(fitxer.path, null, SQLiteDatabase.OPEN_READONLY)
    }

    private fun <T> consulta(
        sql: String,
        args: Array<String> = emptyArray(),
        mapa: (Cursor) -> T,
    ): List<T> {
        db.rawQuery(sql, args).use { c ->
            val sortida = ArrayList<T>()
            while (c.moveToNext()) sortida.add(mapa(c))
            return sortida
        }
    }

    private fun Cursor.text(i: Int): String? = if (isNull(i)) null else getString(i)

    private fun mapaCarta(c: Cursor) = Carta(
        id = c.getString(0),
        nom = c.getString(1),
        nomPila = c.text(2),
        cognom = c.text(3),
        esport = c.text(4),
        naixement = c.text(5),
        defuncio = c.text(6),
        alcadaM = if (c.isNull(7)) null else c.getDouble(7),
        paisQid = c.text(8),
        visites = c.getInt(9),
        raresa = c.getInt(10),
        fotoUrl = c.text(11),
        fotoAutor = c.text(12),
        fotoLlicencia = c.text(13),
        fotoLlicenciaUrl = c.text(14),
        fotoPagina = c.text(15),
    )

    // ---- Cartes ---------------------------------------------------------

    fun carta(id: String): Carta? =
        consulta("SELECT $COLS_CARTA FROM cartes WHERE id=?", arrayOf(id), ::mapaCarta).firstOrNull()

    /** Cartes pels seus ids (es consulta per trossos per respectar el límit de SQLite). */
    fun cartes(ids: Collection<String>): List<Carta> {
        if (ids.isEmpty()) return emptyList()
        val sortida = ArrayList<Carta>(ids.size)
        for (tros in ids.chunked(400)) {
            val marques = tros.joinToString(",") { "?" }
            sortida += consulta(
                "SELECT $COLS_CARTA FROM cartes WHERE id IN ($marques)",
                tros.toTypedArray(),
                ::mapaCarta,
            )
        }
        return sortida
    }

    fun cerca(text: String, limit: Int = 30): List<Carta> =
        consulta(
            "SELECT $COLS_CARTA FROM cartes WHERE nom LIKE ? ORDER BY visites DESC LIMIT ?",
            arrayOf("%" + text.trim() + "%", limit.toString()),
            ::mapaCarta,
        )

    /** Condicions SQL (i arguments) del catàleg segons els filtres; [excloses] són les cartes que ja es tenen. */
    private fun filtreCataleg(
        text: String, esport: String?, raresa: Int?, excloses: Collection<String>?,
    ): Pair<String, Array<String>> {
        val condicions = ArrayList<String>()
        val args = ArrayList<String>()
        if (text.isNotBlank()) {
            condicions += "nom LIKE ?"
            args += "%" + text.trim() + "%"
        }
        if (esport != null) {
            condicions += "esport=?"
            args += esport
        }
        if (raresa != null) {
            condicions += "raresa=?"
            args += raresa.toString()
        }
        if (!excloses.isNullOrEmpty()) {
            // Els ids són QID de Wikidata: s'incrusten com a literals (només lletres i xifres) per no
            // topar amb el límit de variables d'SQLite.
            val segurs = excloses.filter { id -> id.all { it.isLetterOrDigit() || it == '_' } }
            condicions += "id NOT IN (" + segurs.joinToString(",") { "'$it'" } + ")"
        }
        val onClau = if (condicions.isEmpty()) "" else " WHERE " + condicions.joinToString(" AND ")
        return onClau to args.toTypedArray()
    }

    /** Pàgina del catàleg de totes les cartes, de les més rares i famoses a les més comunes. */
    fun cataleg(
        text: String, esport: String?, raresa: Int?, excloses: Collection<String>?, limit: Int, offset: Int,
    ): List<Carta> {
        val (onClau, args) = filtreCataleg(text, esport, raresa, excloses)
        return consulta(
            "SELECT $COLS_CARTA FROM cartes$onClau ORDER BY raresa DESC, visites DESC, id LIMIT ? OFFSET ?",
            args + limit.toString() + offset.toString(),
            ::mapaCarta,
        )
    }

    fun catalegTotal(text: String, esport: String?, raresa: Int?, excloses: Collection<String>?): Int {
        val (onClau, args) = filtreCataleg(text, esport, raresa, excloses)
        return consulta("SELECT COUNT(*) FROM cartes$onClau", args) { it.getInt(0) }.first()
    }

    fun totalCartes(): Int = consulta("SELECT COUNT(*) FROM cartes") { it.getInt(0) }.first()

    /** Nombre de cartes que hi ha d'una raresa (1..5). */
    fun nombreRaresa(raresa: Int): Int =
        consulta("SELECT COUNT(*) FROM cartes WHERE raresa=?", arrayOf(raresa.toString())) {
            it.getInt(0)
        }.first()

    /** Id d'una carta qualsevol d'una raresa que no sigui a [excloses], o null si no n'hi ha. */
    fun cartaAleatoria(raresa: Int, rnd: Random, excloses: Set<String> = emptySet()): String? {
        val filtre = if (excloses.isEmpty()) "" else
            " AND id NOT IN (" + excloses.joinToString(",") { "?" } + ")"
        val args = arrayOf(raresa.toString()) + excloses.toTypedArray()
        val n = consulta("SELECT COUNT(*) FROM cartes WHERE raresa=?$filtre", args) { it.getInt(0) }.first()
        if (n == 0) return null
        return consulta(
            "SELECT id FROM cartes WHERE raresa=?$filtre LIMIT 1 OFFSET ?",
            args + rnd.nextInt(n).toString(),
        ) { it.getString(0) }.firstOrNull()
    }

    // ---- Fitxa ----------------------------------------------------------

    /** Text breu en la llengua demanada o, si no n'hi ha, en ca, es, en, fr. */
    fun text(cartaId: String, llengua: String): TextCarta? {
        val ordre = (listOf(llengua) + listOf("ca", "es", "en", "fr")).distinct()
        val trobats = consulta(
            "SELECT llengua, text, url FROM textos WHERE carta_id=?",
            arrayOf(cartaId),
        ) { TextCarta(text = it.getString(1), url = it.text(2), llengua = it.getString(0)) }
        return ordre.firstNotNullOfOrNull { l -> trobats.firstOrNull { it.llengua == l } }
    }

    fun etiqueta(qid: String?, llengua: String): String? {
        if (qid == null) return null
        val trobats = consulta(
            "SELECT llengua, nom FROM etiquetes WHERE qid=?",
            arrayOf(qid),
        ) { it.getString(0) to it.getString(1) }.toMap()
        return (listOf(llengua, "ca", "en", "es", "fr")).firstNotNullOfOrNull { trobats[it] }
    }

    fun premis(cartaId: String, llengua: String): List<PremiCarta> =
        consulta(
            "SELECT premi_qid, any_premi FROM carta_premi WHERE carta_id=? ORDER BY any_premi",
            arrayOf(cartaId),
        ) { it.getString(0) to it.text(1) }
            .mapNotNull { (qid, any) ->
                etiqueta(qid, llengua)?.takeIf { !esQid(it) }?.let { PremiCarta(it, any) }
            }
            .distinctBy { it.nom + "|" + it.any }

    fun marques(cartaId: String, llengua: String): List<MarcaCarta> =
        consulta(
            "SELECT disciplina_qid, valor, unitat_qid FROM carta_marca WHERE carta_id=?",
            arrayOf(cartaId),
        ) { Triple(it.text(0), it.getDouble(1), it.text(2)) }
            .mapNotNull { (disc, valor, unitat) ->
                val nom = etiqueta(disc, llengua)?.takeIf { !esQid(it) } ?: return@mapNotNull null
                Triple(MarcaCarta(nom, valor, etiqueta(unitat, llengua)?.takeIf { !esQid(it) }), unitat, nom)
            }
            // Wikidata porta tot l'historial de marques: a la carta només la millor de cada disciplina
            // (el temps més baix si la unitat és el segon; si no, el valor més alt).
            .groupBy { it.third }
            .map { (_, marques) ->
                if (marques.first().second == QID_SEGON) marques.minBy { it.first.valor }.first
                else marques.maxBy { it.first.valor }.first
            }


    // ---- Configuració -----------------------------------------------------

    fun config(clau: String): String? =
        consulta("SELECT valor FROM config WHERE clau=?", arrayOf(clau)) { it.getString(0) }
            .firstOrNull()

    private fun esQid(text: String) = text.length > 1 && text[0] == 'Q' && text.drop(1).all { it.isDigit() }

    companion object {
        const val NOM_BD = "joc_cartes.sqlite"
        private const val COLS_CARTA =
            "id, nom, nom_pila, cognom, esport, naixement, defuncio, alcada_m, pais_qid, " +
                "visites, raresa, foto_url, foto_autor, foto_llicencia, foto_llicencia_url, foto_pagina"
    }
}

/** Unitat «segon» a Wikidata: les marques en aquesta unitat són millors com més baixes. */
private const val QID_SEGON = "Q11574"
