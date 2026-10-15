package cat.cartes.esports.data

/**
 * Codi ISO 3166-1 alfa-2 dels països (clau: QID de Wikidata). Només els països i territoris
 * d'esport habituals; un QID que no hi sigui no mostra bandera.
 * Cal revisar-la quan hi hagi la descàrrega completa (idealment amb la propietat P297 de Wikidata).
 */
private val ISO_PER_QID: Map<String, String> = mapOf(
    // Europa
    "Q183" to "DE", "Q142" to "FR", "Q38" to "IT", "Q29" to "ES", "Q45" to "PT", "Q145" to "GB",
    "Q27" to "IE", "Q55" to "NL", "Q29999" to "NL", "Q31" to "BE", "Q32" to "LU", "Q39" to "CH",
    "Q40" to "AT", "Q35" to "DK", "Q34" to "SE", "Q20" to "NO", "Q33" to "FI", "Q189" to "IS",
    "Q36" to "PL", "Q213" to "CZ", "Q214" to "SK", "Q28" to "HU", "Q218" to "RO", "Q219" to "BG",
    "Q41" to "GR", "Q403" to "RS", "Q224" to "HR", "Q225" to "BA", "Q215" to "SI", "Q236" to "ME",
    "Q221" to "MK", "Q222" to "AL", "Q1246" to "XK", "Q212" to "UA", "Q159" to "RU", "Q184" to "BY",
    "Q217" to "MD", "Q37" to "LT", "Q211" to "LV", "Q191" to "EE", "Q233" to "MT", "Q229" to "CY",
    "Q228" to "AD", "Q238" to "SM", "Q235" to "MC", "Q347" to "LI",
    // Amèrica
    "Q30" to "US", "Q16" to "CA", "Q96" to "MX", "Q241" to "CU", "Q786" to "DO", "Q790" to "HT",
    "Q766" to "JM", "Q242" to "BZ", "Q800" to "CR", "Q804" to "PA", "Q811" to "NI", "Q792" to "SV",
    "Q774" to "GT", "Q783" to "HN", "Q155" to "BR", "Q414" to "AR", "Q77" to "UY", "Q733" to "PY",
    "Q750" to "BO", "Q736" to "EC", "Q739" to "CO", "Q717" to "VE", "Q419" to "PE", "Q298" to "CL",
    "Q734" to "GY", "Q730" to "SR", "Q754" to "TT", "Q778" to "BS", "Q781" to "AG", "Q244" to "BB",
    "Q760" to "LC", "Q757" to "VC", "Q769" to "GD", "Q784" to "DM", "Q763" to "KN",
    // Àsia i Oceania
    "Q668" to "IN", "Q843" to "PK", "Q902" to "BD", "Q854" to "LK", "Q837" to "NP", "Q889" to "AF",
    "Q252" to "ID", "Q17" to "JP", "Q884" to "KR", "Q423" to "KP", "Q148" to "CN", "Q865" to "TW",
    "Q711" to "MN", "Q230" to "GE", "Q399" to "AM", "Q227" to "AZ", "Q43" to "TR", "Q813" to "KG",
    "Q863" to "TJ", "Q874" to "TM", "Q232" to "KZ", "Q265" to "UZ", "Q836" to "MM", "Q424" to "KH",
    "Q869" to "TH", "Q881" to "VN", "Q833" to "MY", "Q334" to "SG", "Q928" to "PH", "Q794" to "IR",
    "Q796" to "IQ", "Q801" to "IL", "Q851" to "SA", "Q846" to "QA", "Q878" to "AE", "Q858" to "SY",
    "Q822" to "LB", "Q810" to "JO", "Q408" to "AU", "Q664" to "NZ", "Q712" to "FJ", "Q691" to "PG",
    "Q683" to "WS", "Q678" to "TO",
    // Àfrica
    "Q79" to "EG", "Q1028" to "MA", "Q262" to "DZ", "Q948" to "TN", "Q1016" to "LY", "Q1033" to "NG",
    "Q1041" to "SN", "Q1008" to "CI", "Q117" to "GH", "Q1009" to "CM", "Q114" to "KE", "Q115" to "ET",
    "Q924" to "TZ", "Q1044" to "SL", "Q1049" to "SD", "Q258" to "ZA", "Q1006" to "GN", "Q912" to "ML",
    "Q965" to "BF", "Q971" to "CG", "Q974" to "CD", "Q916" to "AO", "Q1025" to "MR", "Q1032" to "NE",
    "Q657" to "TD", "Q1019" to "MG", "Q954" to "ZW", "Q953" to "ZM", "Q1036" to "UG", "Q1045" to "SO",
    "Q986" to "ER", "Q1050" to "SZ", "Q1013" to "LS", "Q1005" to "GM", "Q1007" to "GW", "Q1000" to "GA",
    "Q1042" to "SC", "Q1029" to "MZ", "Q1030" to "NA", "Q963" to "BW", "Q1020" to "MW", "Q1011" to "CV",
)

/** Emoji de la bandera del país (a partir dels indicadors regionals de Unicode), o null si no el coneixem. */
fun banderaPais(paisQid: String?): String? {
    val iso = ISO_PER_QID[paisQid] ?: return null
    return iso.map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
}
