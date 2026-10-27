# Spec: Felhasználólista lekérése a Jirából

- **ID:** 0002-jira-user-import
- **Státusz:** Approved
- **Intent:** [intent.md](intent.md)
- **Dátum:** 2026-10-27

## Összefoglaló

Az adatbázis létrehozásakor a backend egyszer lekéri a „HUGO - Board” (scrum) aktív
sprintjének résztvevőit a Jira Cloud REST API-jából, és elmenti őket csapattagként. A
belépési kolléga-választóban, az admin csapatoldalán és az értékelések címzettjei között
onnantól a valódi HUGO csapat szerepel. A csapat utána nem változik. A felület és az API
nem változik.

## Viselkedés

### Fő folyamat

1. A backend indul. Ha a `team_member` tábla **üres**, és a Jira-kapcsolat be van
   állítva, elindul az import. Ez profiltól függetlenül így van, `dev` profilban is.
2. A backend lekéri a beállított tábla (`HUGO - Board`) **aktív sprintjét**.
3. Lekéri az aktív sprint összes feladatát, lapozva, és összegyűjti a feladatok
   **assignee-it** (lásd Nyitott kérdések 1.). Minden felhasználó egyszer szerepel.
4. Minden felhasználót elment csapattagként. Mentett adatok: Jira-azonosító,
   megjelenített név, e-mail (ha a Jira visszaadja), avatar URL, aktív-e, importálás ideje.
5. Az import egyetlen tranzakció: vagy minden felhasználó bekerül, vagy egy sem.
6. Ha a `team_member` tábla **nem üres**, az import nem fut le újra. A csapat így
   változatlan marad, a meglévő űrlapok és kitöltések érintetlenek.
7. Csak az **aktív** csapattagok látszanak és választhatók bárhol. A belépési
   kolléga-választó (`GET /api/auth/team-members`), az admin csapatoldala és űrlap-szerkesztője
   (`GET /api/admin/team-members`), valamint a kiküldéskori címzettlista is csak őket
   tartalmazza. Inaktív tag nem léphet be, és nem lehet értékelt személy.

### Fejlesztői és tesztkörnyezet

- **`dev` profil, Jira-beállítás nélkül:** a meglévő `DevTeamSeeder` tölti be a kitalált
  csapattagokat (ahogy most, `npm run dev:api`). A Jira nem kap hívást.
- **`dev` profil nélkül, Jira-beállítás nélkül:** nincs import, nincs kitalált csapat, a
  backend figyelmeztetést naplóz, és üres csapattal indul.
- Kitalált és valódi csapattag soha nem kerül ugyanabba az adatbázisba. Az import csak
  üres táblába ír, a kitalált lista pedig csak Jira-beállítás nélkül töltődik be.
- Ha valaki a kitalált csapattal létrejött adatbázis után állítja be a Jirát, az import
  nem fut, mert a tábla nem üres. Az adatbázisfájl (`apps/api/data/board.db`) törlése után
  újraindul minden.

### Hibaesetek

- **A Jira nem elérhető, lassú, vagy hibás a hitelesítés:** az import megszakad, semmi nem
  mentődik. A backend hibát naplóz (a tokent nem írja a naplóba), és üres csapattal
  elindul. A Jira-hívásoknak időkorlátja van (kapcsolódás és olvasás egyaránt), így a
  backend indulása nem akad el. A következő induláskor az import újra megpróbálkozik,
  mert a tábla üres.
- **A tábla nem található:** mint fent, a napló megnevezi az okot.
- **Nincs aktív sprint:** mint fent.
- **Az aktív sprintben nincs assignee-vel rendelkező feladat:** az import nem ment
  semmit, a napló jelzi.
- **Üres csapattal** a kolléga-választó üres listát mutat, és űrlap nem küldhető ki
  (`0001`: „Nincs címzett”).

## Beállítás

A Jira-kapcsolat adatai **környezeti változókból** jönnek, soha nem a repóból. Az
`application.yml` csak hivatkozik rájuk, ugyanúgy, mint a `HUGO_ADMIN_*` változókra.

| Környezeti változó | Példa | Kötelező |
|---|---|---|
| `HUGO_JIRA_BASE_URL` | `https://<cég>.atlassian.net` | igen |
| `HUGO_JIRA_EMAIL` | a token tulajdonosa | igen |
| `HUGO_JIRA_API_TOKEN` | – | igen |
| `HUGO_JIRA_BOARD_ID` | `535` | igen |

A kapcsolat „be van állítva”, ha mind a négy változó meg van adva. Ha csak egy része van
megadva, a backend figyelmeztetést naplóz, és úgy viselkedik, mintha a kapcsolat nem
lenne beállítva.

## API

Nincs új végpont és nincs sémaváltozás. A `contracts/openapi.json` és a generált Angular
kliens változatlan. A meglévő végpontok viselkedése annyiban változik, hogy csak aktív
csapattagot adnak vissza (lásd Fő folyamat 7.):

- `GET /api/auth/team-members` — belépés nélkül, a kolléga-választóhoz
- `GET /api/admin/team-members` — adminnak

```jsonc
// TeamMember — változatlan
{ "id": 3, "name": "Teszt Elek" }
```

Az e-mail, a Jira-azonosító és az avatar URL nem jelenik meg az API-ban. Az avatar URL
csak mentődik, a `0003` 2. fázisa teszi ki és jeleníti meg.

## Adatmodell

Új Flyway migráció (`V2__…`), amely a `team_member` táblát bővíti:

| Tábla | Oszlop | Típus | Kötelező | Megjegyzés |
|---|---|---|---|---|
| `team_member` | `jira_account_id` | TEXT | nem | Jira Cloud `accountId`; egyedi, ha meg van adva; kitalált tagnál üres |
| | `email` | TEXT | nem | a Jira adatvédelmi beállításai miatt hiányozhat |
| | `avatar_url` | TEXT | nem | a Jira 48×48-as avatarjának URL-je |
| | `active` | INTEGER | igen | 0 / 1, alapérték 1; a Jira `active` mezője |
| | `imported_at` | TEXT | nem | az import ideje (ISO-8601 UTC); kitalált tagnál üres |

A meglévő sorok (kitalált tagok) `active = 1` értéket kapnak, a többi új oszlopuk üres.

## Elfogadási kritériumok

A Jira-hívásokat a tesztek egy helyettesítő (mock) Jira-szerverrel váltják ki
(`MockRestServiceServer`). Valódi Jira-hívás teszt közben nem történik.

1. **Adott** üres `team_member` tábla és beállított Jira-kapcsolat, az aktív sprintben
   három különböző assignee-vel, **amikor** a backend elindul, **akkor** pontosan három
   csapattag jön létre a nevükkel, Jira-azonosítójukkal, avatar URL-jükkel és az
   importálás idejével.
2. **Adott** egy aktív sprint, amelyben ugyanannak a felhasználónak több feladata van,
   **amikor** az import lefut, **akkor** a felhasználó egyszer szerepel.
3. **Adott** egy aktív sprint, amelyben assignee nélküli feladatok is vannak, **amikor**
   az import lefut, **akkor** ezek a feladatok nem hoznak létre csapattagot.
4. **Adott** egy aktív sprint, amelynek feladatai több oldalon jönnek, **amikor** az
   import lefut, **akkor** minden oldal assignee-i bekerülnek.
5. **Adott** egy felhasználó, akinek az e-mailjét a Jira nem adja vissza, **amikor** az
   import lefut, **akkor** a csapattag létrejön üres e-maillel.
6. **Adott** egy assignee, akinek a Jira-fiókja inaktív, **amikor** az import lefut,
   **akkor** a csapattag létrejön `active = 0` értékkel, de egyik csapattag-végpont sem
   adja vissza, nem léphet be vele senki, nem lehet értékelt személy, és kiküldéskor nem
   kap kitöltést.
7. **Adott** nem üres `team_member` tábla, **amikor** a backend elindul, **akkor** a
   Jira nem kap hívást, és a csapattagok változatlanok.
8. **Adott** üres tábla, és a Jira hibát ad (pl. `401` vagy `500`), **amikor** a backend
   elindul, **akkor** az app elindul, a tábla üres marad, és a napló tartalmazza a hiba
   okát, de nem tartalmazza a tokent.
9. **Adott** üres tábla, és a Jira a sprint-feladatok lekérése közben (pl. a második
   oldalnál) hibát ad, **amikor** az import fut, **akkor** egyetlen csapattag sem
   mentődik (nincs félkész import).
10. **Adott** üres tábla, és a beállított tábla nem létezik, vagy nincs aktív sprintje,
    **amikor** a backend elindul, **akkor** a tábla üres marad, és a napló megnevezi az
    okot.
11. **Adott** egy Jira, amely nem válaszol, **amikor** az import fut, **akkor** a hívás az
    időkorlát után hibával megszakad, és onnantól a 8. pont szerint viselkedik (az app
    elindul, a tábla üres).
12. **Adott** üres tábla, `dev` profil és **nem** beállított Jira-kapcsolat, **amikor** a
    backend elindul, **akkor** a kitalált csapattagok töltődnek be, és a Jira nem kap
    hívást.
13. **Adott** üres tábla, `dev` profil és beállított Jira-kapcsolat, **amikor** a backend
    elindul, **akkor** az import fut, és kitalált csapattag nem jön létre.
14. **Adott** csak részben megadott Jira-beállítás (pl. token nélkül), **amikor** a
    backend elindul, **akkor** a Jira nem kap hívást, és a napló figyelmeztet a hiányzó
    beállításra (a megadott értékeket nem írja ki).
15. **Adott** egy sikeres import, **amikor** belépés nélkül lekérem a
    `GET /api/auth/team-members` végpontot, **akkor** az importált aktív tagok
    szerepelnek a listában, csak `id` és `name` mezővel.
16. **Adott** egy sikeres import, **amikor** az admin űrlapot küld ki egy importált tag
    értékelésére, **akkor** a többi aktív importált tag megkapja kitöltésre (`0001`
    szerint).

## Nincs benne (out of scope)

- A csapat későbbi változásának követése (új tag, kilépő tag), újraimportálás.
- Jira webhook a felhasználókhoz (a csapat nem változik).
- Bejelentkezés Jira-fiókkal.
- Az avatar megjelenítése és kitétele az API-ban (a `0003` 2. fázisa).
- Bármilyen frontend-változás.
- Bármilyen írás a Jirába.

## Nyitott kérdések

A felhasználó 2026-10-27-én mind a négy feltételezést jóváhagyta.

1. **Kik az „aktív sprint résztvevői”?** A Jirában nincs külön sprinttagság.
   (Feltételezés: az aktív sprint feladatainak assignee-i. Alternatíva: a reporterek is.)
2. **Inaktív Jira-felhasználó:** ha egy assignee fiókja a Jirában inaktív, bekerüljön-e?
   (Feltételezés: bekerül `active = 0` jelöléssel, de sehol nem látszik és nem választható.)
3. **A token típusa:** személyes Jira API token (e-mail és token, Basic auth), vagy egy
   technikai felhasználó? (Feltételezés: személyes API token, csak helyi környezeti
   változóban.)
4. **Táblaazonosító vagy táblanév?** (Feltételezés: táblaazonosító (`535`), mert a név
   szerinti keresés több táblát is visszaadhat, és átnevezéskor eltörik.)
