"""
Afegeix cartes noves a joc_cartes.sqlite fins a arribar a un total.

Agafa d'una base de dades de wikidata_cartes.py (--origen) els esportistes complets que encara no són al joc,
en tria els que calguin repartits entre els esports (i amb una foto prou gran) i regenera la base de dades del joc
amb exporta_joc.py. Les cartes que ja hi ha es conserven totes.

Ús:
    python afegeix_cartes.py --origen nous300.sqlite --conserva joc_cartes.sqlite --total 300
"""
import argparse
import os
import random
import shutil
import sqlite3
import subprocess
import sys
import tempfile

COMPLETA = ("j.valid=1 AND j.foto_estat='ok' AND j.naixement IS NOT NULL "
            "AND j.nom NOT GLOB 'Q[0-9]*'")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--origen", required=True, help="base de dades de wikidata_cartes.py")
    ap.add_argument("--conserva", default="joc_cartes.sqlite", help="joc_cartes.sqlite actual (se'n conserven les cartes)")
    ap.add_argument("--sortida", default=None, help="fitxer de sortida (per defecte, sobreescriu --conserva)")
    ap.add_argument("--total", type=int, default=300, help="nombre total de cartes que ha de tenir el joc")
    ap.add_argument("--foto-min", type=int, default=400, help="costat curt mínim de la foto de les cartes noves (px)")
    ap.add_argument("--llavor", type=int, default=1991, help="llavor del sorteig (per poder repetir el resultat)")
    args = ap.parse_args()
    sortida = args.sortida or args.conserva
    aqui = os.path.dirname(os.path.abspath(__file__))

    antigues = {r[0] for r in sqlite3.connect(args.conserva).execute("SELECT id FROM cartes")}
    src = sqlite3.connect(args.origen)
    cols = {r[1] for r in src.execute("PRAGMA table_info(jugadors)")}
    complets = {r[0]: r[1] for r in src.execute(
        "SELECT j.qid, (SELECT esport FROM ids WHERE qid=j.qid ORDER BY esport LIMIT 1) FROM jugadors j "
        "WHERE %s AND j.qid IN (SELECT qid FROM ids)" % COMPLETA)}
    perdudes = antigues - set(complets)
    if perdudes:
        print("AVÍS: %d cartes actuals no són completes a l'origen i no es conservarien: %s"
              % (len(perdudes), ", ".join(sorted(perdudes)[:10])))
        sys.exit("Aturat: torna a baixar-les amb --ids-de joc_cartes.sqlite.")

    filtre = ""
    if "foto_ample" in cols:
        filtre = "AND (j.foto_ample IS NULL OR j.foto_alt IS NULL OR MIN(j.foto_ample, j.foto_alt) >= %d) " % args.foto_min
    bones = {r[0] for r in src.execute(
        "SELECT j.qid FROM jugadors j WHERE %s %s AND j.qid IN (SELECT qid FROM ids)" % (COMPLETA, filtre))}
    candidats = {}
    for q, esport in complets.items():
        if q not in antigues and q in bones:
            candidats.setdefault(esport, []).append(q)
    falten = args.total - len(antigues)
    print("Cartes actuals: %d | candidates noves amb foto >= %d px: %d | n'hi falten %d"
          % (len(antigues), args.foto_min, sum(len(v) for v in candidats.values()), falten))
    if falten <= 0:
        sys.exit("Ja hi ha %d cartes o més." % len(antigues))
    if sum(len(v) for v in candidats.values()) < falten:
        sys.exit("No hi ha prou candidates: baixa'n més (augmenta --prova) o abaixa --foto-min.")

    # Reparteix les noves entre els esports tan igualment com es pugui.
    rnd = random.Random(args.llavor)
    for v in candidats.values():
        v.sort()
        rnd.shuffle(v)
    triades = []
    esports = sorted(candidats)
    while len(triades) < falten:
        avanc = False
        # Cada ronda dona una carta a l'esport que, comptant les actuals, en té menys.
        tenen = {e: sum(1 for q in antigues | set(triades) if complets.get(q) == e) for e in esports}
        for e in sorted(esports, key=lambda e: (tenen[e], e)):
            if candidats[e]:
                triades.append(candidats[e].pop())
                avanc = True
                break
        if not avanc:
            break

    with tempfile.TemporaryDirectory() as tmp:
        copia = os.path.join(tmp, "origen.sqlite")
        shutil.copy(args.origen, copia)
        c = sqlite3.connect(copia)
        mantenir = antigues | set(triades)
        c.execute("CREATE TEMP TABLE k(qid TEXT)")
        c.executemany("INSERT INTO k VALUES (?)", [(q,) for q in mantenir])
        c.execute("DELETE FROM ids WHERE qid NOT IN (SELECT qid FROM k)")
        c.commit()
        c.close()
        nou = os.path.join(tmp, "joc.sqlite")
        r = subprocess.run([sys.executable, os.path.join(aqui, "exporta_joc.py"), "--origen", copia, "--sortida", nou])
        if r.returncode != 0:
            sys.exit("exporta_joc.py ha fallat.")
        ara = sqlite3.connect(nou)
        total = ara.execute("SELECT COUNT(*) FROM cartes").fetchone()[0]
        noves_ids = {x[0] for x in ara.execute("SELECT id FROM cartes")}
        assert antigues <= noves_ids, "s'ha perdut alguna carta actual"
        vell = {x[0]: x[1] for x in sqlite3.connect(args.conserva).execute("SELECT id, raresa FROM cartes")}
        nou_r = {x[0]: x[1] for x in ara.execute("SELECT id, raresa FROM cartes")}
        canvis = sum(1 for k in vell if nou_r[k] != vell[k])
        print("\nCartes al joc: %d (%d de noves) | repartiment: %s | cartes antigues que canvien de raresa: %d"
              % (total, len(triades), dict(ara.execute("SELECT esport, COUNT(*) FROM cartes GROUP BY esport").fetchall()), canvis))
        ara.close()
        shutil.copy(nou, sortida)
    print("Escrit %s" % sortida)
    assets = os.path.join(aqui, "android", "app", "src", "main", "assets", "joc_cartes.sqlite")
    if args.sortida is None and os.path.isdir(os.path.dirname(assets)):
        shutil.copy(sortida, assets)
        print("Copiat també a %s" % assets)


if __name__ == "__main__":
    main()
