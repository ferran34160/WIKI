package cat.cartes.esports.data

data class Carta(
    val id: String,
    val nom: String,
    val nomPila: String?,
    val cognom: String?,
    val esport: String?,
    val naixement: String?,
    val defuncio: String?,
    val alcadaM: Double?,
    val paisQid: String?,
    val visites: Int,
    val raresa: Int,
    val fotoUrl: String?,
    val fotoAutor: String?,
    val fotoLlicencia: String?,
    val fotoLlicenciaUrl: String?,
    val fotoPagina: String?,
) {
    val anyNaixement: String? get() = naixement?.take(4)
}

data class TextCarta(val text: String, val url: String?, val llengua: String)

data class PremiCarta(val nom: String, val any: String?)

data class MarcaCarta(val disciplina: String, val valor: Double, val unitat: String?)

/** Estil visual de les cartes: el clàssic (amb text) o el de foto a pantalla completa. */
enum class EstilCarta { CLASSIC, FOTO }
