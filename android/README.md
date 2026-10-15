# Cartes Esports (Android, Kotlin + Jetpack Compose)

Prototip del joc de cartes d'esportistes. Funciona sense servidor: les cartes
vénen dins l'app (`app/src/main/assets/joc_cartes.sqlite`) i la partida es
desa al mòbil (`estat.db`).

## Com obrir-lo

1. Instal·la Android Studio (versió recent) i obre la carpeta `android/`.
2. Deixa que faci la sincronització de Gradle. El primer cop descarrega l'SDK i
   Gradle 8.9. Si proposa actualitzar el plugin d'Android o Kotlin, accepta.
3. Executa l'app a l'emulador o en un mòbil amb la depuració USB activada.
4. Les proves unitàries del motor (sobres, economia) són a
   `app/src/test/.../MotorTest.kt` (`Run` o `./gradlew test`).

**Estat:** el codi s'ha escrit sense poder-lo compilar (no hi havia Android
Studio a l'ordinador de treball). Cal esperar alguna errada de compilació
menor a la primera sincronització; passa'm el missatge i es corregeix.

## Estructura

| Carpeta / fitxer | Què hi ha |
|---|---|
| `data/CartaRepository.kt` | Lectura de `joc_cartes.sqlite` (cartes, textos, premis, col·leccions) |
| `data/EstatJugador.kt` | Partida: inventari, monedes, comptadors, recompenses reclamades |
| `game/Motor.kt` | Raresa, economia, sobres guardats i compte enrere, missions |
| `game/JocViewModel.kt` | Estat de la partida i accions (obrir sobre, destruir, fabricar, reclamar) |
| `ui/` | Pantalles Compose: Inici, Àlbum, Col·leccions, Missions, Taller, Perfil, fitxa |
| `res/values*/strings.xml` | Textos en català (per defecte), castellà, anglès i francès |

## Regles del joc (valors a `game/Motor.kt`)

- Sobre de 5 cartes; un de nou cada 10 min; es guarden fins a 3.
- Probabilitat per carta: comuna 60%, poc comuna 25%, rara 10%, èpica 4%, llegendària 1%.
- Destruir dona 5 / 15 / 50 / 200 / 1000 monedes; fabricar costa 5 vegades això.
- Es comença amb 100 monedes i 3 sobres.

## Actualitzar les cartes

Després de regenerar la base de dades des de Python:

```
python exporta_joc.py --origen cartes.sqlite --sortida joc_cartes.sqlite
copy joc_cartes.sqlite android\app\src\main\assets\joc_cartes.sqlite
```

L'app torna a copiar el fitxer en cada versió nova (`versionCode`). La
partida del jugador és en una altra base de dades i no es perd.

## Limitacions conegudes

- Les consultes a la base de dades es fan al fil principal. Amb la mostra va
  bé; amb 100.000 cartes caldrà moure-les a corrutines (`Dispatchers.IO`) i
  paginar l'àlbum.
- Les fotos es carreguen d'internet (Wikimedia Commons): sense connexió es
  veu la silueta.
- No hi ha icona pròpia de l'app ni sons.
- Sense mercat, intercanvis ni comptes (fase 2).
