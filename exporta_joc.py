#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
exporta_joc.py - Converteix la base de dades de wikidata_cartes.py en el fitxer
que llegirà l'app Android (joc_cartes.sqlite): cartes amb raresa, textos en
diverses llengües, noms d'entitats i col·leccions automàtiques.

Ús:
    python exporta_joc.py --origen prova100b.sqlite --sortida joc_cartes.sqlite

Només fa servir la biblioteca estàndard.
"""

import argparse
import os
import sqlite3
import sys

# Raresa: (clau, fracció de cartes). Ordenades de més comuna a més rara.
RARESES = [("comuna", 0.50), ("poc_comuna", 0.30), ("rara", 0.15),
           ("epica", 0.04), ("llegendaria", 0.01)]

LLENGUES = ("ca", "es", "en", "fr")
MIDA_MIN_COLECCIO = 3      # a la descàrrega completa pujar-ho (p. ex. 10)
MIDA_MAX_COLECCIO = 100    # col·leccions més grans es parteixen per esport/dècada

SCHEMA = """
CREATE TABLE cartes(
    id TEXT PRIMARY KEY, nom TEXT, nom_pila TEXT, cognom TEXT, esport TEXT,
    naixement TEXT, defuncio TEXT, alcada_m REAL, pais_qid TEXT, genere_qid TEXT,
    visites INTEGER, llengues INTEGER, raresa INTEGER, raresa_clau TEXT,
    foto_url TEXT, foto_autor TEXT, foto_llicencia TEXT, foto_llicencia_url TEXT,
    foto_pagina TEXT);
CREATE TABLE textos(carta_id TEXT, llengua TEXT, text TEXT, url TEXT,
    PRIMARY KEY (carta_id, llengua));
CREATE TABLE etiquetes(qid TEXT, llengua TEXT, nom TEXT, PRIMARY KEY (qid, llengua));
CREATE TABLE carta_premi(carta_id TEXT, premi_qid TEXT, any_premi TEXT);
CREATE TABLE carta_equip(carta_id TEXT, equip_qid TEXT, inici TEXT, fi TEXT);
CREATE TABLE carta_marca(carta_id TEXT, disciplina_qid TEXT, valor REAL, unitat_qid TEXT);
CREATE TABLE coleccions(id INTEGER PRIMARY KEY, tipus TEXT, clau TEXT, esport TEXT,
    mida INTEGER);
CREATE TABLE coleccio_carta(coleccio_id INTEGER, carta_id TEXT,
    PRIMARY KEY (coleccio_id, carta_id));
CREATE TABLE config(clau TEXT PRIMARY KEY, valor TEXT);
CREATE INDEX i_cartes_raresa ON cartes(raresa);
CREATE INDEX i_cartes_esport ON cartes(esport);
CREATE INDEX i_col_carta ON coleccio_carta(carta_id);
"""


def assigna_rareses(files):
    """files: llista de (id, visites, llengues). Retorna {id: índex_raresa 1..5}.

    Es ordena per visites (desempat: llengües, després id) i es tallen
    els percentils de RARESES. Les cartes sense visites compten com a 0.
    """
    ordre = sorted(files, key=lambda f: (f[1] or 0, f[2] or 0, f[0]))
    n = len(ordre)
    talls, acum = [], 0.0
    for _, fraccio in RARESES:
        acum += fraccio
        talls.append(round(n * acum))
    resultat = {}
    for pos, f in enumerate(ordre):
        idx = next(i for i, t in enumerate(talls) if pos < t) if pos < talls[-1] else len(talls) - 1
        resultat[f[0]] = idx + 1
    llindars = []
    for i in range(len(RARESES)):
        ini = talls[i - 1] if i else 0
        fi = talls[i]
        if fi > ini:
            llindars.append((RARESES[i][0], ordre[ini][1] or 0, ordre[fi - 1][1] or 0, fi - ini))
        else:
            llindars.append((RARESES[i][0], None, None, 0))
    return resultat, llindars


def main():
    ap = argparse.ArgumentParser(description="Exporta la base de dades per a l'app.")
    ap.add_argument("--origen", default="prova100b.sqlite")
    ap.add_argument("--sortida", default="joc_cartes.sqlite")
    args = ap.parse_args()

    if not os.path.exists(args.origen):
        sys.exit("No trobo %s" % args.origen)
    if os.path.exists(args.sortida):
        os.remove(args.sortida)

    src = sqlite3.connect(args.origen)
    out = sqlite3.connect(args.sortida)
    out.executescript(SCHEMA)

    # --- cartes: només les que tenen foto, nom, naixement i alguna visita
    files = src.execute(
        "SELECT j.qid, j.visites, j.llengues FROM jugadors j "
        "WHERE j.valid=1 AND j.foto_estat='ok' AND j.naixement IS NOT NULL "
        "AND j.nom NOT GLOB 'Q[0-9]*' AND j.qid IN (SELECT qid FROM ids)").fetchall()
    if not files:
        sys.exit("No hi ha cap carta completa a l'origen.")
    rareses, llindars = assigna_rareses(files)
    ids = {f[0] for f in files}

    for r in src.execute(
            "SELECT j.qid, j.nom, "
            " (SELECT nom FROM entitats WHERE qid=j.nom_pila_qid), "
            " (SELECT nom FROM entitats WHERE qid=j.cognom_qid), "
            " (SELECT esport FROM ids WHERE qid=j.qid ORDER BY esport LIMIT 1), "
            " j.naixement, j.defuncio, j.alcada_m, j.pais_qid, j.genere_qid, j.visites, "
            " j.llengues, j.foto_url, j.foto_autor, j.foto_llicencia, "
            " j.foto_llicencia_url, j.foto_pagina FROM jugadors j"):
        if r[0] not in ids:
            continue
        nom_pila = None if (r[2] or "").startswith("Q") and r[2][1:].isdigit() else r[2]
        cognom = None if (r[3] or "").startswith("Q") and r[3][1:].isdigit() else r[3]
        rar = rareses[r[0]]
        out.execute("INSERT INTO cartes VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    (r[0], r[1], nom_pila, cognom, r[4], r[5], r[6], r[7], r[8], r[9],
                     r[10] or 0, r[11] or 0, rar, RARESES[rar - 1][0],
                     r[12], r[13], r[14], r[15], r[16]))

    # --- textos (només llengües de l'app)
    for r in src.execute("SELECT jugador_qid, llengua, text, url FROM textos"):
        if r[0] in ids and r[1] in LLENGUES:
            out.execute("INSERT OR REPLACE INTO textos VALUES (?,?,?,?)", r)

    # --- premis, equips, marques
    for r in src.execute("SELECT jugador_qid, premi_qid, any_premi FROM premis"):
        if r[0] in ids:
            out.execute("INSERT INTO carta_premi VALUES (?,?,?)", r)
    for r in src.execute("SELECT jugador_qid, equip_qid, inici, fi FROM equips"):
        if r[0] in ids:
            out.execute("INSERT INTO carta_equip VALUES (?,?,?,?)", r)
    for r in src.execute("SELECT jugador_qid, disciplina_qid, valor, unitat_qid FROM marques"):
        if r[0] in ids:
            out.execute("INSERT INTO carta_marca VALUES (?,?,?,?)", r)

    # --- etiquetes: només les entitats que fa servir alguna carta
    usats = set()
    for q in ("SELECT pais_qid FROM cartes", "SELECT genere_qid FROM cartes",
              "SELECT premi_qid FROM carta_premi", "SELECT equip_qid FROM carta_equip",
              "SELECT disciplina_qid FROM carta_marca", "SELECT unitat_qid FROM carta_marca"):
        usats.update(r[0] for r in out.execute(q) if r[0])
    for qid, ll, nom in src.execute("SELECT qid, llengua, nom FROM etiquetes"):
        if qid in usats and ll in LLENGUES:
            out.execute("INSERT OR REPLACE INTO etiquetes VALUES (?,?,?)", (qid, ll, nom))

    # --- col·leccions automàtiques
    def crea(tipus, clau, esport, cartes_ids):
        if len(cartes_ids) < MIDA_MIN_COLECCIO:
            return 0
        cur = out.execute("INSERT INTO coleccions (tipus, clau, esport, mida) VALUES (?,?,?,?)",
                          (tipus, clau, esport, len(cartes_ids)))
        out.executemany("INSERT INTO coleccio_carta VALUES (?,?)",
                        [(cur.lastrowid, c) for c in cartes_ids])
        return 1

    def agrupa(sql, tipus):
        grups = {}
        for carta, clau, esport in out.execute(sql):
            if clau:
                grups.setdefault((clau, esport), set()).add(carta)
        n = 0
        for (clau, esport), cs in sorted(grups.items()):
            n += crea(tipus, clau, esport, sorted(cs))
        return n

    resum = {}
    resum["club"] = agrupa(
        "SELECT e.carta_id, e.equip_qid, NULL FROM carta_equip e", "club")
    resum["pais"] = agrupa(
        "SELECT id, pais_qid, esport FROM cartes", "pais")          # país + esport
    resum["decada"] = agrupa(
        "SELECT id, substr(naixement,1,3) || '0', esport FROM cartes", "decada")  # dècada + esport
    resum["premi"] = agrupa(
        "SELECT p.carta_id, p.premi_qid, NULL FROM carta_premi p", "premi")
    resum["esport_raresa"] = agrupa(
        "SELECT id, esport || ':' || raresa_clau, esport FROM cartes", "esport_raresa")

    # --- configuració
    out.executemany("INSERT INTO config VALUES (?,?)", [
        ("versio", "1"),
        ("sobre_cartes", "5"),
        ("sobre_minuts", "10"),
        ("sobre_max_guardats", "3"),
        ("raresa_fraccions", ",".join("%s=%s" % (c, f) for c, f in RARESES)),
    ])
    out.commit()

    # --- informe
    n_cartes = out.execute("SELECT COUNT(*) FROM cartes").fetchone()[0]
    print("Cartes exportades: %d" % n_cartes)
    print("\nRaresa (visites mínima-màxima, nombre de cartes):")
    for clau, mn, mx, n in llindars:
        print("  %-12s %9s - %-9s %4d" % (clau, mn, mx, n))
    print("\nCol·leccions generades (mida mínima %d):" % MIDA_MIN_COLECCIO)
    for tipus, n in resum.items():
        mides = [r[0] for r in out.execute(
            "SELECT mida FROM coleccions WHERE tipus=? ORDER BY mida", (tipus,))]
        print("  %-14s %3d   mides: %s" % (tipus, n, mides if len(mides) <= 12 else
                                          "%d-%d, mediana %d" % (mides[0], mides[-1],
                                                                  mides[len(mides) // 2])))
    print("\nTextos per llengua:",
          out.execute("SELECT llengua, COUNT(*) FROM textos GROUP BY llengua").fetchall())
    print("Cartes amb premi: %d, amb marca: %d" % (
        out.execute("SELECT COUNT(DISTINCT carta_id) FROM carta_premi").fetchone()[0],
        out.execute("SELECT COUNT(DISTINCT carta_id) FROM carta_marca").fetchone()[0]))
    out.close()


if __name__ == "__main__":
    main()
