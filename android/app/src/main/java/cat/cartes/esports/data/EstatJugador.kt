package cat.cartes.esports.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Tot el que és del jugador: cartes que té, monedes, comptadors i recompenses
 * ja reclamades. Viu en una base de dades pròpia (estat.db), separada de la de
 * les cartes, de manera que es pot substituir el catàleg sense perdre la partida.
 */
class EstatJugador(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "estat.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE inventari(carta_id TEXT PRIMARY KEY, quantitat INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE kv(clau TEXT PRIMARY KEY, valor TEXT NOT NULL)")
        db.execSQL("CREATE TABLE reclamades(clau TEXT PRIMARY KEY)")
    }

    override fun onUpgrade(db: SQLiteDatabase, antiga: Int, nova: Int) = Unit

    // ---- Inventari ------------------------------------------------------

    fun inventari(): Map<String, Int> {
        val mapa = HashMap<String, Int>()
        readableDatabase.rawQuery("SELECT carta_id, quantitat FROM inventari", null).use { c ->
            while (c.moveToNext()) mapa[c.getString(0)] = c.getInt(1)
        }
        return mapa
    }

    fun quantitat(cartaId: String): Int =
        readableDatabase.rawQuery(
            "SELECT quantitat FROM inventari WHERE carta_id=?",
            arrayOf(cartaId),
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    /** Canvia la quantitat d'una carta (pot ser negatiu). Retorna la quantitat resultant. */
    fun canvia(cartaId: String, delta: Int): Int {
        val nova = (quantitat(cartaId) + delta).coerceAtLeast(0)
        val db = writableDatabase
        if (nova == 0) {
            db.delete("inventari", "carta_id=?", arrayOf(cartaId))
        } else {
            val v = ContentValues().apply {
                put("carta_id", cartaId)
                put("quantitat", nova)
            }
            db.insertWithOnConflict("inventari", null, v, SQLiteDatabase.CONFLICT_REPLACE)
        }
        return nova
    }

    // ---- Valors simples -------------------------------------------------

    fun llegeixLong(clau: String, defecte: Long = 0L): Long =
        llegeixText(clau)?.toLongOrNull() ?: defecte

    fun escriuLong(clau: String, valor: Long) = escriuText(clau, valor.toString())

    fun llegeixText(clau: String): String? =
        readableDatabase.rawQuery("SELECT valor FROM kv WHERE clau=?", arrayOf(clau)).use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }

    fun escriuText(clau: String, valor: String) {
        val v = ContentValues().apply {
            put("clau", clau)
            put("valor", valor)
        }
        writableDatabase.insertWithOnConflict("kv", null, v, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // ---- Recompenses reclamades -------------------------------------------

    fun reclamada(clau: String): Boolean =
        readableDatabase.rawQuery("SELECT 1 FROM reclamades WHERE clau=?", arrayOf(clau))
            .use { it.moveToFirst() }

    fun marcaReclamada(clau: String) {
        val v = ContentValues().apply { put("clau", clau) }
        writableDatabase.insertWithOnConflict(
            "reclamades", null, v, SQLiteDatabase.CONFLICT_IGNORE,
        )
    }
}
