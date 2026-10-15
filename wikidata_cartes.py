#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
wikidata_cartes.py - Baixa esportistes de Wikidata (i les seves fotos de
Wikimedia Commons) cap a una base de dades SQLite pensada per a un joc de cartes.

Només fa servir la biblioteca estàndard de Python (3.8 o superior).

Ús (a la carpeta on hagis desat l'script):

    python wikidata_cartes.py --contacte elteu@correu.cat --prova 30 --db prova.sqlite tot
        -> prova ràpida: 30 esportistes per esport (nascuts el 1995)

    python wikidata_cartes.py --contacte elteu@correu.cat tot
        -> la descàrrega completa (pot trigar una o dues hores)

o per fases, en aquest ordre:

    ids         llista d'identificadors per esport i per anys de naixement
    detalls     dades de cada esportista (nom, naixement, clubs, premis...)
    etiquetes   noms d'equips, països, premis...
    fotos       autor i llicència de cada foto (Wikimedia Commons)
    textos      extracte breu de la Viquipèdia (ca, si no es, si no en)
    visites     visites de la Viquipèdia dels últims 12 mesos (suma de llengües)
    informe     recomptes i distribució de popularitat

La fase "visites" és la lenta: fa una petició per esportista i llengua.

Es pot aturar amb Ctrl+C i tornar a executar: continua pel punt on era.
"""

import argparse
import datetime
import html
import json
import re
import sqlite3
import sys
import time
from concurrent.futures import ThreadPoolExecutor
import urllib.error
import urllib.parse
import urllib.request

WDQS = "https://query.wikidata.org/sparql"
WD_API = "https://www.wikidata.org/w/api.php"
COMMONS_API = "https://commons.wikimedia.org/w/api.php"

IDIOMES = ("ca", "es", "en", "fr")  # ordre de preferència per als noms
PAUSA = 1.0                         # segons entre peticions
ANY_INICI, ANY_FI, PAS = 1850, 2016, 10
TEXT_LLENGUES = ("ca", "es", "en", "fr")  # llengües de l'extracte (i ordre de preferència)
# Edicions de la Viquipèdia on es comptaran les visites (per defecte)
VISITES_LLENGUES = ("en", "fr", "es")
VISITES_FILS = 6                    # peticions en paral·lel (fase visites)
VISITES_PAUSA = 0.15                # segons de pausa de cada fil entre peticions
MAX_EXTRACTE = 600                  # caràcters màxims de l'extracte
# Es guarden els enllaços d'aquestes llengües; --llengues-visites en tria un subconjunt.
VISITES_LLENGUES_TOTES = ("en", "es", "ca", "fr", "de", "it", "pt", "ru", "ja",
                          "ar", "zh", "pl", "nl", "tr", "ko", "id", "sv")

# esport -> (ocupació a Wikidata, paraula que ha de contenir el seu nom en anglès)
ESPORTS = {
    "futbol":    ("Q937857",   "football"),
    "basquet":   ("Q3665646",  "basketball"),
    "tennis":    ("Q10833314", "tennis"),
    "atletisme": ("Q11513337", "athlet"),
}

# Edicions de la Viquipèdia que no compten com a "llengua" per mesurar popularitat
NO_WIKIS = {"commonswiki", "wikidatawiki", "metawiki", "specieswiki",
            "mediawikiwiki", "sourceswiki", "foundationwiki", "outreachwiki",
            "wikimaniawiki"}

UA = "CartesEsports/0.1 (sense contacte)"


# --------------------------------------------------------------------------
# Xarxa
# --------------------------------------------------------------------------

class ErrorHTTP(Exception):
    def __init__(self, codi, cos):
        super().__init__("HTTP %s: %s" % (codi, cos[:200]))
        self.codi = codi
        self.cos = cos


class Timeout(Exception):
    pass


def http_json(url, params=None, accept="application/json"):
    """Fa una petició GET i retorna el JSON. Reintenta si cal."""
    if params:
        url = url + "?" + urllib.parse.urlencode(params)
    for intent in range(10):
        time.sleep(PAUSA)
        req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": accept})
        try:
            with urllib.request.urlopen(req, timeout=100) as r:
                return json.load(r)
        except urllib.error.HTTPError as e:
            cos = e.read().decode("utf-8", "replace")
            if e.code in (429, 503) and intent < 9:
                # Wikimedia limita de vegades a 1 petició/minut: si no diu quant, esperem 65 s.
                try:
                    espera = int(e.headers.get("Retry-After", "65"))
                except (TypeError, ValueError):
                    espera = 65
                time.sleep(min(max(espera, 5), 180))
                continue
            raise ErrorHTTP(e.code, cos)
        except (OSError, ValueError):
            if intent < 9:
                time.sleep(10 * (intent + 1))
                continue
            raise


def api(url, params):
    """Crida l'API de MediaWiki (Wikidata o Commons) respectant maxlag."""
    p = dict(params)
    p.setdefault("format", "json")
    # Sense "maxlag": només fem lectures, i aquest paràmetre també fa esperar per
    # l'endarreriment del servei de consultes (SPARQL), que no ens afecta.
    for _ in range(8):
        dades = http_json(url, p)
        err = dades.get("error") if isinstance(dades, dict) else None
        if err and err.get("code") == "maxlag":
            time.sleep(10)
            continue
        if err:
            raise RuntimeError("Error de l'API: %s" % (err,))
        return dades
    raise RuntimeError("L'API està massa carregada; torna-ho a provar més tard.")


# --------------------------------------------------------------------------
# Base de dades
# --------------------------------------------------------------------------

ESQUEMA = """
CREATE TABLE IF NOT EXISTS ids(qid TEXT, esport TEXT, PRIMARY KEY (qid, esport));
CREATE TABLE IF NOT EXISTS feina(clau TEXT PRIMARY KEY);
CREATE TABLE IF NOT EXISTS jugadors(
    qid TEXT PRIMARY KEY, valid INTEGER, nom TEXT, descripcio TEXT, naixement TEXT,
    pais_qid TEXT, genere_qid TEXT, alcada_m REAL, llengues INTEGER,
    article_ca TEXT, article_en TEXT, article_es TEXT,
    foto_fitxer TEXT, foto_estat TEXT, foto_url TEXT, foto_original TEXT,
    foto_pagina TEXT, foto_autor TEXT, foto_llicencia TEXT, foto_llicencia_url TEXT,
    nom_pila_qid TEXT, cognom_qid TEXT, defuncio TEXT,
    extracte TEXT, extracte_llengua TEXT, extracte_url TEXT,
    visites INTEGER, detall_v INTEGER);
CREATE TABLE IF NOT EXISTS enllacos(jugador_qid TEXT, llengua TEXT, titol TEXT);
CREATE INDEX IF NOT EXISTS i_enllacos ON enllacos(jugador_qid);
CREATE TABLE IF NOT EXISTS etiquetes(
    qid TEXT, llengua TEXT, nom TEXT, PRIMARY KEY (qid, llengua));
CREATE TABLE IF NOT EXISTS textos(
    jugador_qid TEXT, llengua TEXT, text TEXT, url TEXT, PRIMARY KEY (jugador_qid, llengua));
CREATE TABLE IF NOT EXISTS equips(
    jugador_qid TEXT, equip_qid TEXT, inici TEXT, fi TEXT, partits REAL, gols REAL);
CREATE TABLE IF NOT EXISTS premis(jugador_qid TEXT, premi_qid TEXT, any_premi TEXT);
CREATE TABLE IF NOT EXISTS marques(
    jugador_qid TEXT, disciplina_qid TEXT, valor REAL, unitat_qid TEXT);
CREATE TABLE IF NOT EXISTS entitats(qid TEXT PRIMARY KEY, nom TEXT);
CREATE INDEX IF NOT EXISTS i_equips ON equips(jugador_qid);
CREATE INDEX IF NOT EXISTS i_premis ON premis(jugador_qid);
CREATE INDEX IF NOT EXISTS i_marques ON marques(jugador_qid);
"""

VISTA = """
DROP VIEW IF EXISTS cartes;
CREATE VIEW cartes AS
SELECT j.qid, j.nom,
       CASE WHEN np.nom GLOB 'Q[0-9]*' THEN NULL ELSE np.nom END AS nom_pila,
       CASE WHEN cg.nom GLOB 'Q[0-9]*' THEN NULL ELSE cg.nom END AS cognom, j.descripcio,
       j.naixement, j.defuncio, p.nom AS pais, g.nom AS genere,
       j.alcada_m, j.llengues, j.visites,
       (SELECT group_concat(esport, ',') FROM ids WHERE ids.qid = j.qid) AS esports,
       (SELECT count(*) FROM premis WHERE jugador_qid = j.qid) AS n_premis,
       j.extracte, j.extracte_llengua, j.extracte_url,
       j.foto_url, j.foto_autor, j.foto_llicencia, j.foto_llicencia_url, j.foto_pagina,
       j.article_ca, j.article_en, j.article_es
FROM jugadors j
LEFT JOIN entitats p ON p.qid = j.pais_qid
LEFT JOIN entitats g ON g.qid = j.genere_qid
LEFT JOIN entitats np ON np.qid = j.nom_pila_qid
LEFT JOIN entitats cg ON cg.qid = j.cognom_qid
WHERE j.valid = 1 AND j.foto_estat = 'ok';
"""

NOVES_COLUMNES = {
    "nom_pila_qid": "TEXT", "cognom_qid": "TEXT", "defuncio": "TEXT",
    "extracte": "TEXT", "extracte_llengua": "TEXT", "extracte_url": "TEXT",
    "visites": "INTEGER", "detall_v": "INTEGER",
}


def obre(ruta):
    con = sqlite3.connect(ruta)
    con.executescript(ESQUEMA)
    # Bases de dades creades amb una versió anterior de l'script: afegeix les columnes noves.
    existents = {r[1] for r in con.execute("PRAGMA table_info(jugadors)")}
    for col, tipus in NOVES_COLUMNES.items():
        if col not in existents:
            con.execute("ALTER TABLE jugadors ADD COLUMN %s %s" % (col, tipus))
    con.executescript(VISTA)
    return con


# --------------------------------------------------------------------------
# Comprovació dels esports
# --------------------------------------------------------------------------

def comprova_esports(esports):
    ids = "|".join(ESPORTS[e][0] for e in esports)
    dades = api(WD_API, {"action": "wbgetentities", "ids": ids,
                         "props": "labels", "languages": "en"})
    errors = []
    print("== Comprovació dels identificadors d'ocupació")
    for e in esports:
        qid, paraula = ESPORTS[e]
        ent = (dades.get("entities") or {}).get(qid, {})
        nom = ((ent.get("labels") or {}).get("en") or {}).get("value", "?")
        print("   %s: %s = «%s»" % (e, qid, nom))
        if paraula not in nom.lower():
            errors.append("%s (%s) es diu «%s»" % (e, qid, nom))
    if errors:
        sys.exit("Aturat: aquests identificadors no quadren amb l'esport esperat:\n  - "
                 + "\n  - ".join(errors)
                 + "\nCorregeix ESPORTS a l'script (o digues-m'ho) abans de continuar.")


# --------------------------------------------------------------------------
# Fase 1: identificadors
# --------------------------------------------------------------------------

def sparql_ids(qocc, ini, fi):
    consulta = (
        "SELECT DISTINCT ?s WHERE {\n"
        "  ?s wdt:P106 wd:%s ; wdt:P18 ?img ; wdt:P569 ?n .\n"
        "  FILTER(?n >= \"%04d-01-01T00:00:00Z\"^^xsd:dateTime && "
        "?n < \"%04d-01-01T00:00:00Z\"^^xsd:dateTime)\n"
        "}" % (qocc, ini, fi))
    try:
        dades = http_json(WDQS, {"query": consulta, "format": "json"},
                          accept="application/sparql-results+json")
    except ErrorHTTP as e:
        if e.codi in (500, 502, 503, 504) or "imeout" in e.cos:
            raise Timeout(str(e))
        raise
    except (OSError, ValueError) as e:
        raise Timeout(str(e))
    return [b["s"]["value"].rsplit("/", 1)[-1] for b in dades["results"]["bindings"]]


def tros(con, esport, qocc, ini, fi):
    """Baixa els ids d'un interval d'anys; si dona timeout, el parteix per la meitat."""
    clau = "%s|%d|%d" % (esport, ini, fi)
    if con.execute("SELECT 1 FROM feina WHERE clau=?", (clau,)).fetchone():
        return True
    try:
        ids = sparql_ids(qocc, ini, fi)
    except Timeout:
        if fi - ini <= 1:
            print("   ! %s %d: temps esgotat fins i tot per a un sol any. "
                  "Es reintentarà la propera vegada." % (esport, ini))
            return False
        mig = (ini + fi) // 2
        ok1 = tros(con, esport, qocc, ini, mig)
        ok2 = tros(con, esport, qocc, mig, fi)
        if ok1 and ok2:
            con.execute("INSERT OR IGNORE INTO feina VALUES (?)", (clau,))
            con.commit()
        return ok1 and ok2
    con.executemany("INSERT OR IGNORE INTO ids VALUES (?, ?)", [(q, esport) for q in ids])
    con.execute("INSERT OR IGNORE INTO feina VALUES (?)", (clau,))
    con.commit()
    print("   %s %d-%d: %d" % (esport, ini, fi - 1, len(ids)))
    return True


def fase_ids(con, esports, prova=0, any_prova=1995, ids_de=None):
    print("== ids")
    if ids_de:
        # Conserva les cartes d'un joc_cartes.sqlite anterior: es tornen a baixar amb els nous.
        antic = sqlite3.connect(ids_de)
        files = antic.execute("SELECT id, esport FROM cartes").fetchall()
        con.executemany("INSERT OR IGNORE INTO ids VALUES (?, ?)", files)
        con.commit()
        print("   %d cartes conservades de %s" % (len(files), ids_de))
    for esport in esports:
        qocc = ESPORTS[esport][0]
        if prova:
            # Mode prova: un sol any, N esportistes, sense marcar la feina com a feta.
            ids = sparql_ids(qocc, any_prova, any_prova + 1)[:prova]
            con.executemany("INSERT OR IGNORE INTO ids VALUES (?, ?)", [(q, esport) for q in ids])
            con.commit()
            print("   %s (prova): %d" % (esport, len(ids)))
            continue
        for ini in range(ANY_INICI, ANY_FI, PAS):
            tros(con, esport, qocc, ini, min(ini + PAS, ANY_FI))
        n = con.execute("SELECT COUNT(*) FROM ids WHERE esport=?", (esport,)).fetchone()[0]
        print("   -> %s: %d identificadors en total" % (esport, n))


# --------------------------------------------------------------------------
# Fase 2: detalls
# --------------------------------------------------------------------------

def actius(claims, p):
    return [s for s in (claims.get(p) or []) if s.get("rank") != "deprecated"]


def primer(claims, p):
    llista = actius(claims, p)
    pref = [s for s in llista if s.get("rank") == "preferred"]
    llista = pref or llista
    return llista[0] if llista else None


def valor(x):
    """Valor d'una declaració (té 'mainsnak') o d'un qualificador (és un snak)."""
    if not isinstance(x, dict):
        return None
    snak = x.get("mainsnak", x)
    if snak.get("snaktype") != "value":
        return None
    dv = snak.get("datavalue")
    return dv.get("value") if dv else None


def qual(stmt, p):
    llista = (stmt.get("qualifiers") or {}).get(p) or []
    return valor(llista[0]) if llista else None


def qid_de(v):
    return v.get("id") if isinstance(v, dict) else None


def data_text(v):
    if not isinstance(v, dict) or "time" not in v:
        return None
    t = v["time"]
    if not t.startswith("+"):
        return None
    p = v.get("precision", 11)
    return t[1:5] if p <= 9 else (t[1:8] if p == 10 else t[1:11])


def quantitat(v):
    try:
        return float(v["amount"]), (v.get("unit") or "").rsplit("/", 1)[-1]
    except (KeyError, TypeError, ValueError, AttributeError):
        return None, None


def alcada_m(v):
    x, u = quantitat(v)
    if x is None:
        return None
    f = {"Q11573": 1.0, "Q174728": 0.01, "Q218593": 0.0254}.get(u)
    return round(x * f, 3) if f else None


def millor_text(dic, idiomes=IDIOMES):
    dic = dic or {}
    for l in idiomes:
        if l in dic and dic[l].get("value"):
            return dic[l]["value"]
    return None


def desa(con, ent):
    qid = ent["id"]
    claims = ent.get("claims") or {}
    sl = ent.get("sitelinks") or {}
    llengues = sum(1 for k in sl if re.fullmatch(r"[a-z_]+wiki", k) and k not in NO_WIKIS)

    def un(p):
        st = primer(claims, p)
        return valor(st) if st else None

    foto = un("P18")
    con.execute(
        "INSERT OR REPLACE INTO jugadors (qid, valid, nom, descripcio, naixement, pais_qid, "
        "genere_qid, alcada_m, llengues, article_ca, article_en, article_es, foto_fitxer, "
        "nom_pila_qid, cognom_qid, defuncio, detall_v) "
        "VALUES (?,1,?,?,?,?,?,?,?,?,?,?,?,?,?,?,2)",
        (qid, millor_text(ent.get("labels")) or qid, millor_text(ent.get("descriptions")),
         data_text(un("P569")), qid_de(un("P27")), qid_de(un("P21")), alcada_m(un("P2048")),
         llengues, (sl.get("cawiki") or {}).get("title"), (sl.get("enwiki") or {}).get("title"),
         (sl.get("eswiki") or {}).get("title"), foto if isinstance(foto, str) else None,
         qid_de(un("P735")), qid_de(un("P734")), data_text(un("P570"))))

    for taula in ("equips", "premis", "marques", "enllacos"):
        con.execute("DELETE FROM %s WHERE jugador_qid=?" % taula, (qid,))

    for ll in VISITES_LLENGUES_TOTES:  # enllaços per comptar visites
        tit = (sl.get("%swiki" % ll) or {}).get("title")
        if tit:
            con.execute("INSERT INTO enllacos VALUES (?,?,?)", (qid, ll, tit))

    for s in actius(claims, "P54"):  # membre d'un equip
        eq = qid_de(valor(s))
        if eq:
            con.execute("INSERT INTO equips VALUES (?,?,?,?,?,?)",
                        (qid, eq, data_text(qual(s, "P580")), data_text(qual(s, "P582")),
                         quantitat(qual(s, "P1350"))[0], quantitat(qual(s, "P1351"))[0]))
    for s in actius(claims, "P166"):  # premi rebut
        pr = qid_de(valor(s))
        if pr:
            con.execute("INSERT INTO premis VALUES (?,?,?)",
                        (qid, pr, data_text(qual(s, "P585"))))
    for s in actius(claims, "P2415"):  # marca personal
        x, u = quantitat(valor(s))
        if x is not None:
            con.execute("INSERT INTO marques VALUES (?,?,?,?)",
                        (qid, qid_de(qual(s, "P2416")), x, u))


def fase_detalls(con):
    pend = [r[0] for r in con.execute(
        "SELECT DISTINCT qid FROM ids WHERE qid NOT IN "
        "(SELECT qid FROM jugadors WHERE valid=0 OR detall_v >= 2) ORDER BY qid")]
    print("== detalls: %d esportistes pendents" % len(pend))
    for i in range(0, len(pend), 50):
        lot = pend[i:i + 50]
        dades = api(WD_API, {"action": "wbgetentities", "ids": "|".join(lot),
                             "props": "labels|descriptions|claims|sitelinks",
                             "languages": "|".join(IDIOMES)})
        trobats = set()
        for ent in (dades.get("entities") or {}).values():
            if "missing" in ent or "id" not in ent:
                continue
            desa(con, ent)
            trobats.add(ent["id"])
        for q in lot:
            if q not in trobats:
                con.execute("INSERT OR IGNORE INTO jugadors (qid, valid) VALUES (?, 0)", (q,))
        con.commit()
        if (i // 50) % 10 == 0:
            print("   %d/%d" % (min(i + 50, len(pend)), len(pend)))


# --------------------------------------------------------------------------
# Fase 3: etiquetes (noms d'equips, països, premis...)
# --------------------------------------------------------------------------

def fase_etiquetes(con):
    q = ("SELECT equip_qid FROM equips UNION SELECT premi_qid FROM premis "
         "UNION SELECT disciplina_qid FROM marques UNION SELECT unitat_qid FROM marques "
         "UNION SELECT pais_qid FROM jugadors UNION SELECT genere_qid FROM jugadors "
         "UNION SELECT nom_pila_qid FROM jugadors UNION SELECT cognom_qid FROM jugadors")
    tots = {r[0] for r in con.execute(q) if r[0] and re.fullmatch(r"Q\d+", r[0])}
    fets = {r[0] for r in con.execute("SELECT DISTINCT qid FROM etiquetes")}
    pend = sorted(tots - fets)
    print("== etiquetes: %d pendents" % len(pend))
    for i in range(0, len(pend), 50):
        lot = pend[i:i + 50]
        dades = api(WD_API, {"action": "wbgetentities", "ids": "|".join(lot),
                             "props": "labels", "languages": "|".join(IDIOMES)})
        noms = {}
        for ent in (dades.get("entities") or {}).values():
            if "id" in ent and "missing" not in ent:
                etiq = ent.get("labels") or {}
                noms[ent["id"]] = millor_text(etiq) or ent["id"]
                for ll, d in etiq.items():
                    if d.get("value"):
                        con.execute("INSERT OR REPLACE INTO etiquetes VALUES (?,?,?)",
                                    (ent["id"], ll, d["value"]))
        con.executemany("INSERT OR REPLACE INTO entitats VALUES (?, ?)",
                        [(qq, noms.get(qq, qq)) for qq in lot])
        # fila marcador (llengua buida) perquè no es torni a demanar si no té cap etiqueta
        con.executemany("INSERT OR IGNORE INTO etiquetes VALUES (?, '', ?)",
                        [(qq, noms.get(qq, qq)) for qq in lot])
        con.commit()
        if (i // 50) % 20 == 0:
            print("   %d/%d" % (min(i + 50, len(pend)), len(pend)))


# --------------------------------------------------------------------------
# Fase 4: fotos (Wikimedia Commons)
# --------------------------------------------------------------------------

def neteja(text):
    if not text:
        return None
    t = html.unescape(re.sub(r"<[^>]+>", "", text))
    return re.sub(r"\s+", " ", t).strip() or None


def fase_fotos(con):
    pend = con.execute(
        "SELECT qid, foto_fitxer FROM jugadors "
        "WHERE valid=1 AND foto_fitxer IS NOT NULL AND foto_estat IS NULL").fetchall()
    print("== fotos: %d pendents" % len(pend))
    for i in range(0, len(pend), 50):
        lot = pend[i:i + 50]
        titols = {"File:" + f.replace("_", " "): q for q, f in lot}
        dades = api(COMMONS_API, {
            "action": "query", "prop": "imageinfo", "iiprop": "url|extmetadata",
            "iiurlwidth": 500, "redirects": 1, "formatversion": 2,
            "titles": "|".join(titols)})
        consulta = dades.get("query") or {}
        norm = {n["from"]: n["to"] for n in consulta.get("normalized", [])}
        redir = {r["from"]: r["to"] for r in consulta.get("redirects", [])}
        pagines = {p["title"]: p for p in consulta.get("pages", [])}
        for titol, qid in titols.items():
            t = norm.get(titol, titol)
            t = redir.get(t, t)
            pg = pagines.get(t)
            info = ((pg or {}).get("imageinfo") or [None])[0]
            if not info:
                con.execute("UPDATE jugadors SET foto_estat='no' WHERE qid=?", (qid,))
                continue
            em = info.get("extmetadata") or {}

            def meta(k):
                return (em.get(k) or {}).get("value")

            con.execute(
                "UPDATE jugadors SET foto_estat='ok', foto_url=?, foto_original=?, foto_pagina=?, "
                "foto_autor=?, foto_llicencia=?, foto_llicencia_url=? WHERE qid=?",
                (info.get("thumburl") or info.get("url"), info.get("url"),
                 info.get("descriptionurl"), neteja(meta("Artist")),
                 neteja(meta("LicenseShortName")), meta("LicenseUrl"), qid))
        con.commit()
        if (i // 50) % 10 == 0:
            print("   %d/%d" % (min(i + 50, len(pend)), len(pend)))


# --------------------------------------------------------------------------
# Fase 5: textos (extracte de la Viquipèdia)
# --------------------------------------------------------------------------

def retalla(text, maxim=MAX_EXTRACTE):
    """Acorta l'extracte fins a l'última frase sencera que càpiga."""
    t = re.sub(r"\s+", " ", text or "").strip()
    if len(t) <= maxim:
        return t or None
    tall = t[:maxim]
    punt = max(tall.rfind(". "), tall.rfind("? "), tall.rfind("! "))
    return tall[:punt + 1] if punt > maxim // 3 else tall.rstrip() + "…"


def fase_textos(con):
    print("== textos")
    for ll in TEXT_LLENGUES:
        pend = [r for r in con.execute(
            "SELECT e.jugador_qid, e.titol FROM enllacos e JOIN jugadors j "
            "ON j.qid=e.jugador_qid WHERE e.llengua=? AND j.valid=1 AND j.foto_estat='ok' "
            "AND NOT EXISTS (SELECT 1 FROM textos t WHERE t.jugador_qid=e.jugador_qid "
            "AND t.llengua=e.llengua)", (ll,))
            if "|" not in r[1]]
        print("   %s: %d pendents" % (ll, len(pend)))
        url = "https://%s.wikipedia.org/w/api.php" % ll
        for i in range(0, len(pend), 20):
            lot = pend[i:i + 20]
            titols = {t: q for q, t in lot}
            dades = api(url, {"action": "query", "prop": "extracts", "exintro": 1,
                              "explaintext": 1, "exlimit": 20, "redirects": 1,
                              "formatversion": 2, "titles": "|".join(titols)})
            consulta = dades.get("query") or {}
            norm = {n["from"]: n["to"] for n in consulta.get("normalized", [])}
            redir = {r["from"]: r["to"] for r in consulta.get("redirects", [])}
            pagines = {p["title"]: p for p in consulta.get("pages", [])}
            for titol, qid in titols.items():
                t = norm.get(titol, titol)
                t = redir.get(t, t)
                text = retalla((pagines.get(t) or {}).get("extract"))
                if text:
                    con.execute(
                        "INSERT OR REPLACE INTO textos VALUES (?,?,?,?)",
                        (qid, ll, text, "https://%s.wikipedia.org/wiki/%s" % (
                            ll, urllib.parse.quote(t.replace(" ", "_"), safe=""))))
            con.commit()
            if (i // 20) % 10 == 0:
                print("   %s %d/%d" % (ll, min(i + 20, len(pend)), len(pend)))
    # extracte "per defecte" (ca, si no es, si no en, si no fr) a la taula jugadors
    for qid, ll, text, url in con.execute(
            "SELECT jugador_qid, llengua, text, url FROM textos ORDER BY "
            "CASE llengua WHEN 'ca' THEN 0 WHEN 'es' THEN 1 WHEN 'en' THEN 2 ELSE 3 END DESC"
    ).fetchall():
        con.execute("UPDATE jugadors SET extracte=?, extracte_llengua=?, extracte_url=? "
                    "WHERE qid=?", (text, ll, url, qid))
    con.commit()


# --------------------------------------------------------------------------
# Fase 6: visites de la Viquipèdia
# --------------------------------------------------------------------------

def mesos_visites():
    """Els últims 12 mesos complets, com a (inici, fi) en format AAAAMMDD."""
    avui = datetime.date.today()
    primer_mes_actual = avui.replace(day=1)
    fi = primer_mes_actual - datetime.timedelta(days=1)
    any_, mes = fi.year - 1, fi.month + 1
    if mes == 13:
        any_, mes = fi.year, 1
    return "%04d%02d01" % (any_, mes), fi.strftime("%Y%m%d")


def visites_article(llengua, titol, ini, fi):
    """Visites d'un article (accés d'usuaris, tots els dispositius) entre ini i fi."""
    t = urllib.parse.quote(titol.replace(" ", "_"), safe="")
    url = ("https://wikimedia.org/api/rest_v1/metrics/pageviews/per-article/"
           "%s.wikipedia/all-access/user/%s/monthly/%s/%s" % (llengua, t, ini, fi))
    for intent in range(5):
        time.sleep(VISITES_PAUSA)
        req = urllib.request.Request(url, headers={"User-Agent": UA})
        try:
            with urllib.request.urlopen(req, timeout=60) as r:
                return sum(x["views"] for x in json.load(r).get("items", []))
        except urllib.error.HTTPError as e:
            if e.code == 404:       # sense dades per a aquest article
                return 0
            if e.code in (429, 503) and intent < 4:
                time.sleep(10 * (intent + 1))
                continue
            return None
        except (OSError, ValueError):
            if intent < 4:
                time.sleep(5 * (intent + 1))
                continue
            return None
    return None


def fase_visites(con, llengues):
    ini, fi = mesos_visites()
    pend = [r[0] for r in con.execute(
        "SELECT qid FROM jugadors WHERE valid=1 AND foto_estat='ok' AND visites IS NULL")]
    print("== visites: %d esportistes pendents (%s-%s, llengües: %s)" %
          (len(pend), ini, fi, ",".join(llengues)))
    marques = ",".join("?" * len(llengues))
    with ThreadPoolExecutor(max_workers=VISITES_FILS) as pool:
        for i in range(0, len(pend), 50):
            lot = pend[i:i + 50]
            feines = []
            for qid in lot:
                for ll, tit in con.execute(
                        "SELECT llengua, titol FROM enllacos WHERE jugador_qid=? "
                        "AND llengua IN (%s)" % marques, (qid,) + tuple(llengues)):
                    feines.append((qid, ll, tit))
            resultats = list(pool.map(lambda f: visites_article(f[1], f[2], ini, fi), feines))
            total = {q: 0 for q in lot}
            fallats = set()
            for (qid, _, _), v in zip(feines, resultats):
                if v is None:
                    fallats.add(qid)
                else:
                    total[qid] += v
            for qid in lot:
                if qid not in fallats:     # els fallits es reintentaran la propera vegada
                    con.execute("UPDATE jugadors SET visites=? WHERE qid=?", (total[qid], qid))
            con.commit()
            if (i // 50) % 10 == 0:
                print("   %d/%d" % (min(i + 50, len(pend)), len(pend)))


# --------------------------------------------------------------------------
# Informe
# --------------------------------------------------------------------------

COMPLETA = ("j.valid=1 AND j.foto_estat='ok' AND j.naixement IS NOT NULL "
            "AND j.nom NOT GLOB 'Q[0-9]*'")


def percentil(llista, p):
    if not llista:
        return 0
    return llista[min(len(llista) - 1, int(len(llista) * p / 100))]


def fase_informe(con, esports):
    print("== informe")
    print("%-10s %8s %8s %9s %8s %8s %8s" %
          ("esport", "ids", "foto ok", "completes", "amb equip", "premis", "marques"))
    for e in esports:
        def n(extra=""):
            return con.execute(
                "SELECT COUNT(DISTINCT i.qid) FROM ids i JOIN jugadors j ON j.qid=i.qid "
                "WHERE i.esport=? " + extra, (e,)).fetchone()[0]
        print("%-10s %8d %8d %9d %8d %8d %8d" % (
            e,
            con.execute("SELECT COUNT(*) FROM ids WHERE esport=?", (e,)).fetchone()[0],
            n("AND j.valid=1 AND j.foto_estat='ok'"),
            n("AND " + COMPLETA),
            n("AND " + COMPLETA + " AND EXISTS (SELECT 1 FROM equips q WHERE q.jugador_qid=j.qid)"),
            n("AND " + COMPLETA + " AND EXISTS (SELECT 1 FROM premis q WHERE q.jugador_qid=j.qid)"),
            n("AND " + COMPLETA + " AND EXISTS (SELECT 1 FROM marques q WHERE q.jugador_qid=j.qid)")))
    total = con.execute(
        "SELECT COUNT(*) FROM jugadors j WHERE " + COMPLETA +
        " AND j.qid IN (SELECT qid FROM ids)").fetchone()[0]
    print("\nCartes completes úniques (foto + nom + naixement): %d" % total)
    ll = sorted(r[0] or 0 for r in con.execute(
        "SELECT j.llengues FROM jugadors j WHERE " + COMPLETA +
        " AND j.qid IN (SELECT qid FROM ids)"))
    if ll:
        print("Llengües de la Viquipèdia: mediana %d, p75 %d, p90 %d, "
              "p99 %d, màxim %d" % (percentil(ll, 50), percentil(ll, 75), percentil(ll, 90),
                                    percentil(ll, 99), ll[-1]))
    base = " FROM jugadors j WHERE " + COMPLETA + " AND j.qid IN (SELECT qid FROM ids)"
    vis = sorted(r[0] for r in con.execute("SELECT j.visites" + base) if r[0] is not None)
    if vis:
        print("Visites últims 12 mesos (%d amb dada): mediana %d, p75 %d, p90 %d, p99 %d, "
              "màxim %d" % (len(vis), percentil(vis, 50), percentil(vis, 75), percentil(vis, 90),
                            percentil(vis, 99), vis[-1]))
    print("\nCamps de la fitxa (sobre %d cartes completes):" % total)
    for etiqueta, cond in (("cognom", "j.cognom_qid IS NOT NULL"),
                           ("data de defunció", "j.defuncio IS NOT NULL"),
                           ("alçada", "j.alcada_m IS NOT NULL"),
                           ("text breu", "j.extracte IS NOT NULL"),
                           ("algun premi", "EXISTS (SELECT 1 FROM premis p WHERE "
                                           "p.jugador_qid=j.qid)"),
                           ("alguna marca", "EXISTS (SELECT 1 FROM marques p WHERE "
                                            "p.jugador_qid=j.qid)")):
        n = con.execute("SELECT COUNT(*)" + base + " AND " + cond).fetchone()[0]
        print("   %-18s %d" % (etiqueta, n))


# --------------------------------------------------------------------------
# Programa principal
# --------------------------------------------------------------------------

def main():
    global UA
    ap = argparse.ArgumentParser(description="Descarrega esportistes de Wikidata en una base SQLite.")
    ap.add_argument("fase", choices=["tot", "ids", "detalls", "etiquetes", "fotos",
                                     "textos", "visites", "informe"])
    ap.add_argument("--llengues-visites", default=",".join(VISITES_LLENGUES),
                    help="llengües de la Viquipèdia on comptar visites (per defecte: %s)"
                    % ",".join(VISITES_LLENGUES))
    ap.add_argument("--contacte", required=True,
                    help="el teu correu o web; Wikimedia demana identificar qui fa les peticions")
    ap.add_argument("--db", default="cartes.sqlite", help="fitxer de sortida (per defecte cartes.sqlite)")
    ap.add_argument("--esports", default=",".join(ESPORTS),
                    help="llista separada per comes (per defecte: %s)" % ",".join(ESPORTS))
    ap.add_argument("--prova", type=int, default=0,
                    help="mode prova: només N esportistes per esport (de l'any de --any-prova)")
    ap.add_argument("--any-prova", type=int, default=1995,
                    help="any de naixement del mode prova (per defecte 1995)")
    ap.add_argument("--ids-de", default=None, metavar="JOC_CARTES.SQLITE",
                    help="afegeix també els ids de les cartes d'aquest joc_cartes.sqlite")
    args = ap.parse_args()

    UA = "CartesEsports/0.1 (%s) python-urllib" % args.contacte
    esports = [e.strip() for e in args.esports.split(",") if e.strip()]
    for e in esports:
        if e not in ESPORTS:
            sys.exit("Esport desconegut: %s (opcions: %s)" % (e, ", ".join(ESPORTS)))

    con = obre(args.db)
    llengues_vis = [l.strip() for l in args.llengues_visites.split(",") if l.strip()]
    for l in llengues_vis:
        if l not in VISITES_LLENGUES_TOTES:
            sys.exit("Llengua no disponible per a visites: %s (opcions: %s)"
                     % (l, ", ".join(VISITES_LLENGUES_TOTES)))
    fases = (["ids", "detalls", "etiquetes", "fotos", "textos", "visites", "informe"]
             if args.fase == "tot" else [args.fase])
    try:
        if "ids" in fases:
            comprova_esports(esports)
            fase_ids(con, esports, args.prova, args.any_prova, args.ids_de)
        if "detalls" in fases:
            fase_detalls(con)
        if "etiquetes" in fases:
            fase_etiquetes(con)
        if "fotos" in fases:
            fase_fotos(con)
        if "textos" in fases:
            fase_textos(con)
        if "visites" in fases:
            fase_visites(con, llengues_vis)
        if "informe" in fases:
            fase_informe(con, esports)
    except KeyboardInterrupt:
        con.commit()
        print("\nAturat. Torna a executar la mateixa ordre per continuar.")


if __name__ == "__main__":
    main()
