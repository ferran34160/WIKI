# Projecte: joc de cartes col·leccionables d'esports

Document de context per continuar el projecte. Resumeix el que s'ha decidit i el que encara està obert. Les coses marcades com a **proposta** les va suggerir Claude i **l'usuari encara no les ha confirmades**.

L'usuari parla català: respon-li en català.

## 0. Actualització (decisions confirmades per l'usuari)

- **Plataforma: joc per a Android, Kotlin + Jetpack Compose**, sense servidor al principi (dades i estat del jugador al mòbil). Android Studio **encara no instal·lat en aquest PC**: l'usuari el posarà en un altre; per ara no es pot compilar des d'aquí.
- **Treballar de moment només amb la mostra** (`prova100b.sqlite`, 109 cartes). No es llança la descàrrega completa fins que l'usuari ho digui.
- **Fama = visites de la Viquipèdia dels últims 12 mesos**, sumades a **en, fr i es** (`VISITES_LLENGUES`). Les llengües de l'article (`llengues`) són només desempat.
- **Fitxa comuna de la carta** (sense estadístiques d'atac/defensa): nom, cognoms, naixement i defunció, alçada, **premis o rècords com a text (el nom del premi), abans del text breu, i només si n'hi ha**, text breu (extracte de la Viquipèdia, en ca/es/en/fr), foto amb autor i llicència.
- **Sobres: 5 cartes, un de nou cada 10 min, fins a 3 de guardats.** Raresa per percentils: 50/30/15/4/1 % (comuna, poc comuna, rara, èpica, llegendària). **Proposta**: encara no confirmats exactament.
- **Idioma de l'app: català per defecte**, preparat per canviar a castellà, francès i anglès.
- **Aspecte cridaner** (colors per raresa, brillantor/holo, animació en obrir sobres). Dissenyat un primer esquema de carta; l'usuari l'ha aprovat.
- **Àlbum jeràrquic, no pla**: cartes que tens (cercador i filtres) + **col·leccions** de 10-100 cartes amb silueta i progrés (per club, país, època, premi, esport i raresa). Mai "x de 100.000".
- **Seccions de l'app**: barra superior (monedes, perfil) i 5 pestanyes: Inici (sobres), Àlbum, Col·leccions, Missions, Taller (destruir i fabricar cartes).

### Fitxers
- `wikidata_cartes.py`: descàrrega. Fases `ids, detalls, etiquetes, fotos, textos, visites, informe`. Taules noves: `enllacos`, `etiquetes` (noms per llengua), `textos` (extracte per llengua).
- `exporta_joc.py`: genera `joc_cartes.sqlite` per a l'app (cartes amb raresa, textos, etiquetes, premis, equips, marques, col·leccions automàtiques, config).
- A la mostra, les col·leccions per premi surten buides i les de club són minúscules (3-4 cartes): és un efecte de la mida de la mostra. A la descàrrega completa, pujar `MIDA_MIN_COLECCIO`.
- Per a la API de Wikidata/Wikipedia no s'envia `maxlag` (només lectures): amb ell l'API retornava errors per l'endarreriment del servei SPARQL.

### Codi Android (`android/`)
- Escrit **sense compilar** (no hi havia Android Studio/JDK/Kotlin en aquest PC). Projecte Gradle (AGP 8.7.3, Kotlin 2.0.21, Compose BOM 2024.10.01), paquet `cat.cartes.esports`, sense Room: SQLite directe. Vegeu `android/README.md` (estructura, regles, limitacions).
- Fet: sobres (5 cartes, 10 min, màx. 3), raresa, economia (destruir/fabricar), àlbum amb filtres, col·leccions amb siluetes i recompensa, missions (diàries i permanents), perfil amb idioma ca/es/en/fr, fitxa amb premis abans del text breu, proves unitàries del motor.
- **Cal compilar-lo a un PC amb Android Studio i corregir les errades que surtin.**

### Pendent
- Compilar i provar el projecte `android/` (primera sincronització de Gradle).
- Mostrar a la carta noms de premis i marques (de moment només hi ha 11 cartes amb premi i 3 amb marca a la mostra).
- Decidir si es fa la descàrrega completa (la fase `visites` és la lenta: ~10 peticions/s).

## 1. Què és i d'on surt la idea

Joc de cartes col·leccionables inspirat en **WikiMasters** (https://wiki-masters.com, en francès). WikiMasters converteix articles de la Viquipèdia en cartes: la freqüentació de l'article en fixa la raresa (comuna, poc comuna, rara, súper rara, ultra rara, llegendària), la llargada l'atac i la qualitat la defensa. Té sobres, àlbum, duels entre amics, intercanvis, subhastes, gremis, amics, classificació i èxits. Comptes gratuïts; les monedes es poden comprar amb diners reals, guanyar venent cartes o "destruint" cartes.

**No es vol fer una còpia.** Es vol agafar la idea, però sobre un altre tema i amb una identitat pròpia.

## 2. Decisions preses per l'usuari

- **Tema: esports. Font de dades: Wikidata** (llicència CC0).
- **Esports de moment: futbol, bàsquet, tennis i atletisme.** Es poden afegir altres més endavant (ciclisme, natació...).
- **Nucli del joc: col·leccionar el que vulguis** (p. ex. una col·lecció del FC Barcelona). Els duels entre amics gairebé no es fan servir a WikiMasters, per tant **no són prioritaris**.
- **Menys cartes que WikiMasters, però totes ben fetes: amb foto, informació completa i estadístiques.** Moltes cartes de WikiMasters no tenen foto ni informació, i això fa impossible completar col·leccions.
- **Nombre de cartes: el màxim possible.** Cent mil està bé, més millor. (Abans s'havia parlat d'un milió com a mida orientativa; la decisió posterior és "el màxim que passi el filtre de qualitat".)
- **Missions o reptes amb premis** (insígnies i/o monedes del joc): confirmat com a bona idea.
- **Monedes: res de diners reals.** Es guanyen venent cartes o destruint les que no vols. (L'usuari ho va dir parlant de WikiMasters, i va afegir que la compra amb diners reals no l'interessa.)

## 3. Propostes de Claude (pendents de confirmar)

- **Raresa per percentils**, no pel valor brut de popularitat: p. ex. 1% llegendàries, la meitat comunes. Popularitat = nombre de llengües de la Viquipèdia on l'esportista té article (camp `llengues`). Sobres: primer es tria la raresa amb probabilitats i després una carta a l'atzar dins d'aquella raresa.
- **Economia:** destruir cartes dona monedes (taula de valors per raresa); **fabricar cartes concretes amb monedes** (per evitar col·leccions impossibles d'acabar); confirmació abans de destruir, sobretot si la carta forma part d'una col·lecció en curs; límit de monedes per missions diàries; que hi hagi més formes de gastar que de guanyar. **Mercat entre jugadors (vendre cartes) a la fase 2**, quan ja hi hagi jugadors.
- **Col·leccions:** decidir si són curades (p. ex. "el Dream Team") o dinàmiques (només amb cartes que passen el filtre).
- **Idees per diferenciar-se de WikiMasters** (l'usuari encara no ha triat): (1) química entre companys d'equip (clubs i anys coincidents), (2) estadístiques reals de Wikidata en lloc de popularitat/llargada, (3) missions de "viatge en el temps" (reconstruir l'equip campió del 1992), (4) calendari real (Mundial, Tour...), (5) prediccions amb cartes, mai amb diners reals. Claude va recomanar començar per 1+2.

## 4. Dades

### Estat
- **L'script `wikidata_cartes.py` existeix i la prova (`--prova 30`) ha funcionat** a l'ordinador de l'usuari (Windows, Python instal·lat).
- **La descàrrega completa encara no s'ha fet o no està confirmada.** Fitxer de sortida: `cartes.sqlite`. La prova va a `prova.sqlite`.
- Wikidata talla les consultes SPARQL llargues als 60 s. L'script baixa per esport i rangs d'anys de naixement, parteix els trossos que donen timeout i es pot aturar i reprendre.
- L'script només usa la biblioteca estàndard. Necessita `--contacte <correu o web>` (Wikimedia demana identificar qui fa les peticions): usa el que l'usuari et doni, no n'inventis cap.
- **Claude no pot accedir a Wikidata des del seu entorn al xat** (403 del proxy). Si t'executes al seu ordinador, sí que hauries de poder.

### Ordres
```
python wikidata_cartes.py --contacte <correu> --prova 30 --db prova.sqlite tot   # prova
python wikidata_cartes.py --contacte <correu> tot                                 # completa (1-2 h)
python wikidata_cartes.py --contacte <correu> informe                             # només l'informe
```
Fases: `ids`, `detalls`, `etiquetes`, `fotos`, `informe`.

### Ocupacions a Wikidata (comprovades a la prova)
futbol Q937857 («association football player»), bàsquet Q3665646 («basketball player»), tennis Q10833314 («tennis player»), atletisme Q11513337 («athletics competitor»).

### Recomptes ja obtinguts amb consultes manuals a query.wikidata.org
- Futbolistes amb foto (P18): **69.648**.
- Futbolistes amb foto + data de naixement + almenys un club: **59.793**.
- No s'han pogut comptar els altres esports (les consultes agrupades caduquen).

### Esquema SQLite
- `ids(qid, esport)`, `feina(clau)` (control de reprèn).
- `jugadors`: qid, valid, nom, descripcio, naixement, pais_qid, genere_qid, alcada_m, llengues, article_ca/en/es, foto_fitxer, foto_estat ('ok'/'no'), foto_url, foto_original, foto_pagina, foto_autor, foto_llicencia, foto_llicencia_url.
- `equips(jugador_qid, equip_qid, inici, fi, partits, gols)`, `premis(jugador_qid, premi_qid, any_premi)`, `marques(jugador_qid, disciplina_qid, valor, unitat_qid)`, `entitats(qid, nom)` (noms d'equips, països, premis...).
- Vista `cartes`: només jugadors vàlids amb foto OK, amb país i gènere ja resolts.

### Resultat de la prova (30 esportistes per esport, nascuts el 1995)
| esport | amb foto | completes | amb equip | amb premis | amb marques |
|---|---|---|---|---|---|
| futbol | 30 | 30 | 30 | 1 | 0 |
| bàsquet | 30 | 29 | 28 | 4 | 0 |
| tennis | 30 | 30 | 16 | 1 | 0 |
| atletisme | 30 | 30 | 4 | 7 | 4 |

Popularitat (llengües de la Viquipèdia): mediana 15, p75 25, p90 41, p99 64, màxim 66. És una mostra petita; cal tornar a mirar-ho amb les dades completes.

**Conclusions provisionals:** en esports individuals (tennis, atletisme) els clubs no serveixen per a les estadístiques; premis i marques personals són escassos a Wikidata.

## 5. Llicències i aspectes legals (no és assessorament legal)

- Wikidata és CC0. Les fotos de Wikimedia Commons són lliures, però **cada carta ha de mostrar autor i llicència** (ja es guarden a `foto_autor`, `foto_llicencia`, `foto_llicencia_url`, `foto_pagina`). El text de la Viquipèdia, si es fa servir, és CC BY-SA (atribució i compartir igual).
- **Evitar escuts de clubs i equipacions** (drets). El nom i la imatge d'una persona en un joc comercial poden tenir implicacions segons el país: consultar-ho abans de publicar.
- **Sense compra de monedes amb diners reals**, almenys al principi: les caixes de sorpresa estan regulades en alguns països.

## 6. Preguntes obertes

1. **Quines estadístiques tindrà cada esport?** (atac/defensa a partir de dades reals: gols, partits, títols, alçada, marques...). Per a esports individuals cal una altra base que els clubs.
2. **Els "rècords o marques destacables"** que l'usuari volia a les cartes no són un camp uniforme a Wikidata: calen premis, marques personals o una altra font. Mirar-ho amb les dades completes.
3. **Llindar de qualitat definitiu** (foto + nom + naixement són el mínim actual; falta decidir si exigir també club, premis...).
4. Quina idea de diferenciació es tria (secció 3), i si hi haurà duels o només col·leccionar i missions.
5. Esportistes de diversos esports, nascuts abans del 1850 (no s'inclouen) i cartes femenines (el camp `genere_qid` permet separar-les).
6. **Tecnologia del joc encara per decidir** (web, comptes d'usuari, sobres, col·lecció, missions).

## 7. Propers passos

1. Fer la descàrrega completa i executar `informe`.
2. Decidir el llindar de qualitat i el repartiment de rareses amb els recomptes reals.
3. Definir l'estructura de la carta i les estadístiques per esport.
4. Prototip mínim: sobres, col·lecció, destruir cartes per monedes i primeres missions. La part social (mercat, gremis) més endavant.
