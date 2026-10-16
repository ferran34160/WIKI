"""
Millora les fotos de les cartes d'una base de dades de wikidata_cartes.py:
  1. Torna a llegir de Commons la mida de cada foto i en demana una miniatura més gran (--ample).
  2. Per a les fotos petites (costat curt < --min), busca a Wikidata altres fotos de la mateixa persona
     (propietat P18 amb diversos valors) i es queda la més gran, si és prou millor.

Ús:
    python millora_fotos.py --contacte <correu o web> --db cartes.sqlite
"""
import argparse
import sqlite3
import sys

import wikidata_cartes as w


def info_fitxers(fitxers, ample):
    """Torna {nom de fitxer: info d'imageinfo} per a una llista de noms de fitxer de Commons."""
    out = {}
    fitxers = list(dict.fromkeys(fitxers))
    for i in range(0, len(fitxers), 20):
        lot = fitxers[i:i + 20]
        titols = {"File:" + f.replace("_", " "): f for f in lot}
        dades = w.api(w.COMMONS_API, {
            "action": "query", "prop": "imageinfo", "iiprop": "url|size|extmetadata",
            "iiurlwidth": ample, "redirects": 1, "formatversion": 2, "titles": "|".join(titols)})
        q = dades.get("query") or {}
        norm = {n["from"]: n["to"] for n in q.get("normalized", [])}
        redir = {r["from"]: r["to"] for r in q.get("redirects", [])}
        pagines = {p["title"]: p for p in q.get("pages", [])}
        for titol, f in titols.items():
            t = norm.get(titol, titol)
            t = redir.get(t, t)
            ii = ((pagines.get(t) or {}).get("imageinfo") or [None])[0]
            if ii:
                out[f] = ii
        print("   imageinfo %d/%d" % (min(i + 20, len(fitxers)), len(fitxers)), flush=True)
    return out


def camps(ii):
    em = ii.get("extmetadata") or {}

    def meta(k):
        return (em.get(k) or {}).get("value")

    return (ii.get("thumburl") or ii.get("url"), ii.get("url"), ii.get("descriptionurl"),
            w.neteja(meta("Artist")), w.neteja(meta("LicenseShortName")), meta("LicenseUrl"),
            ii.get("width"), ii.get("height"))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--contacte", required=True)
    ap.add_argument("--db", required=True)
    ap.add_argument("--min", type=int, default=500, help="costat curt mínim acceptable (px)")
    ap.add_argument("--factor", type=float, default=1.5, help="la nova foto ha de ser com a mínim tants cops més gran")
    ap.add_argument("--ample", type=int, default=960, help="ample de la miniatura que es guarda")
    args = ap.parse_args()
    w.UA = "CartesEsports/0.1 (%s) python-urllib" % args.contacte

    con = w.obre(args.db)
    cartes = con.execute(
        "SELECT qid, nom, foto_fitxer FROM jugadors WHERE foto_estat='ok' AND foto_fitxer IS NOT NULL "
        "AND qid IN (SELECT qid FROM ids)").fetchall()
    print("== mides i miniatures: %d fotos" % len(cartes), flush=True)
    actuals = info_fitxers([f for _, _, f in cartes], args.ample)
    for qid, nom, f in cartes:
        ii = actuals.get(f)
        if ii:
            con.execute("UPDATE jugadors SET foto_url=?, foto_original=?, foto_pagina=?, foto_autor=?, "
                        "foto_llicencia=?, foto_llicencia_url=?, foto_ample=?, foto_alt=? WHERE qid=?",
                        camps(ii) + (qid,))
    con.commit()

    febles = [(q, n, f) for q, n, f in cartes
              if actuals.get(f) and min(actuals[f]["width"], actuals[f]["height"]) < args.min]
    print("== fotos petites (< %d px): %d" % (args.min, len(febles)), flush=True)
    if not febles:
        return
    # Totes les fotos (P18) de cada persona, de qualsevol rang, llegides de la fitxa JSON de Wikidata.
    candidats = {}
    for q, _, _ in febles:
        fitxa = w.http_json("https://www.wikidata.org/wiki/Special:EntityData/%s.json" % q)
        reclams = (((fitxa.get("entities") or {}).get(q) or {}).get("claims") or {}).get("P18", [])
        for rec in reclams:
            valor = ((rec.get("mainsnak") or {}).get("datavalue") or {}).get("value")
            if isinstance(valor, str):
                candidats.setdefault(q, []).append(valor.replace("_", " "))
    print("   candidates trobades per %d persones" % len(candidats), flush=True)
    noves = info_fitxers([f for fs in candidats.values() for f in fs], args.ample)

    canvis = 0
    for q, nom, f in febles:
        act = actuals[f]
        mida_act = min(act["width"], act["height"])
        millor = None
        for cf in candidats.get(q, []):
            ii = noves.get(cf)
            if not ii or cf.replace("_", " ") == f.replace("_", " "):
                continue
            mida = min(ii["width"], ii["height"])
            if mida >= max(args.min, args.factor * mida_act) and (millor is None or mida > millor[0]):
                millor = (mida, cf, ii)
        if millor:
            mida, cf, ii = millor
            con.execute("UPDATE jugadors SET foto_fitxer=?, foto_url=?, foto_original=?, foto_pagina=?, "
                        "foto_autor=?, foto_llicencia=?, foto_llicencia_url=?, foto_ample=?, foto_alt=? WHERE qid=?",
                        (cf,) + camps(ii) + (q,))
            canvis += 1
            print("   %-28s %4d px -> %4d px  (%s)" % (nom, mida_act, mida, cf), flush=True)
        else:
            print("   %-28s %4d px: cap alternativa millor" % (nom, mida_act), flush=True)
    con.commit()
    print("== FI: %d fotos substituïdes de %d" % (canvis, len(febles)), flush=True)


if __name__ == "__main__":
    main()
