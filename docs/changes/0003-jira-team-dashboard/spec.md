# Spec: Jira csapat-dashboard – 1. fázis: sprint-áttekintés

- **ID:** 0003-jira-team-dashboard
- **Státusz:** Approved
- **Intent:** [intent.md](intent.md)
- **Dátum:** 2026-10-27

## Összefoglaló

A belépett felhasználók egy dashboardon látják a „HUGO - Board” aktív sprintjének
állapotát: KPI-csempék (összes, kész %, folyamatban, blokkolt, gazdátlan), a sprint
haladási sávja, és csempénként lefúrás a feladatlistáig és a Jira-feladatig. A backend
rendszeres időközönként lekérdezi a Jirából a változásokat (polling), elmenti őket, és
Server-Sent Events (SSE) kapcsolaton továbbküldi a megnyitott dashboardoknak. Ehhez
elég egy HUGO-s Jira-felhasználó API-tokenje; nem kell Jira-admin, és a backendnek nem
kell nyilvánosan elérhetőnek lennie. Ez a spec csak az intent 1. fázisát fedi.

## Viselkedés

### Adatok betöltése és élő frissítése

1. **Kezdeti betöltés:** a backend induláskor lekéri a „HUGO - Board” aktív sprintjét és
   annak összes feladatát a Jira REST API-ból, és elmenti az adatbázisba
   (feladat-pillanatkép).
2. **Időzített lekérdezés (polling):** a backend beállítható időközönként (alapérték:
   30 másodperc) lekérdezi a Jirát:
   - változott-e az aktív sprint (új sprint indult, a régi lezárult, módosult a dátuma);
   - mely feladatok módosultak a legutóbbi sikeres lekérdezés óta (csak ezek jönnek
     vissza, nem a teljes sprint).
3. Ha új sprint indult, a backend a kezdeti betöltéshez hasonlóan újratölti a
   pillanatképet az új sprintre.
4. A változott feladatokat a backend frissíti a pillanatképben; ha egy feladat kikerült
   az aktív sprintből, nem számít bele a dashboardba, de az adata megmarad.
5. A Jirában törölt feladatok nem jelennek meg a „módosult” lekérdezésben; ezeket a
   backend úgy veszi észre, hogy **minden lekérdezéskor** összeveti a pillanatkép
   kulcsait az aktív sprint feladatkulcsaival. A törölt feladat kikerül a pillanatképből.
6. A lezárt sprintek és a sprintből kikerült feladatok adatai **nem törlődnek**, hogy
   később, ha elég adat gyűlt össze, időszakra lehessen szűrni.
7. Ha a lekérdezés **változást** talál, a backend **SSE**-n elküldi a friss
   dashboard-adatot az összes megnyitott dashboardnak. Ha nincs változás, nem küld
   semmit.
8. Egy lekérdezés legfeljebb egyszerre fut; ha az előző még tart, a következő kimarad.

### Hibaesetek

- **Nincs aktív sprint:** a dashboard „Nincs aktív sprint” üzenetet mutat, csempék és sáv
  nélkül.
- **A Jira nem elérhető a kezdeti betöltéskor:** a backend naplózza (token nélkül), a
  dashboard „Az adatok nem érhetők el” üzenetet mutat; a következő sikeres lekérdezés
  után megjelennek az adatok.
- **Egy időzített lekérdezés hibát ad** (hálózat, `5xx`, `429` túl sok kérés): a
  pillanatkép változatlan marad, a backend naplózza, és a következő ütemben újra
  próbálja; `429`-nél a Jira által kért várakozási időt betartja. A dashboard az utolsó
  ismert állapotot mutatja az `updatedAt` idővel.
- **Hibás vagy lejárt token (`401`):** a lekérdezés nem frissít, a napló jelzi az okot
  (token nélkül).
- **Belépés nélkül** a dashboard és annak API-ja nem érhető el (`401`).

## Felület

- **Elrendezés:** új **Dashboard** oldal, az admin és a kolléga navigációjában is
  megjelenik (a `0004`-ben lefektetett oldalsáv/mobil alsó sáv mintázata szerint). Felül
  a KPI-csempék sora, alatta a sprint haladási sávja; egy csempére kattintva alatta
  megjelenik a hozzá tartozó, szűrt feladatlista.
- **Komponensek:**
  - **KPI-csempék** (Összes, Kész %, Folyamatban, Blokkolt, Gazdátlan) — kattinthatók,
    a kattintott csempe nyitja a lefúrási listát.
  - **Haladási sáv:** sprint neve, kezdete, vége, eltelt idő aránya, hátralévő napok.
  - **Feladatlista** (lefúrás): kulcs, összefoglaló, státusz, assignee neve vagy „nincs”;
    egy sorra kattintva a Jira-feladat új lapon nyílik meg.
  - **Üres/hiba állapotok:** „Nincs aktív sprint” (csempék és sáv nélkül), „Az adatok nem
    érhetők el” (sikertelen kezdeti betöltés), „Élő frissítés szünetel” (SSE-kapcsolat
    megszakadt, automatikus újrakapcsolódással).
- **Reszponzivitás:** a `0004`-ben lefektetett törésponthoz és navigációs mintához
  igazodik; kisebb képernyőn a csempék egy oszlopba rendeződnek, a feladatlista
  görgethető.
- **Animáció:** csak CSS-alapú, a projekt szabálya szerint; háttérből érkező
  frissítéskor a változott csempe/sáv értéke finoman kiemelve jelenik meg; az „Élő
  frissítés szünetel” jelzés be- és kiúszása animált.
- **Design tokenek:** nem vezet be új design rendszert — a `0004`-ben lefektetett
  PrimeNG/Aura témát, színeket és a Manrope betűtípust használja. Új elem: a Jira
  státuszkategóriákhoz (`TODO`/`IN_PROGRESS`/`DONE`) és a `Blocked` státuszhoz rendelt,
  színvak-barát színkódolás, amely minden nézetben (csempe, lista, címke) és mindkét
  témában (világos/sötét) azonos.

## API

| Metódus | Út | Kérés | Válasz | Hibák |
|---|---|---|---|---|
| `GET` | `/api/dashboard` | – | `200` + `Dashboard` | `401` |
| `GET` | `/api/dashboard/issues?tile={tile}` | `tile` ∈ `ALL`, `DONE`, `IN_PROGRESS`, `BLOCKED`, `UNASSIGNED` | `200` + `Issue[]` | `400`, `401` |
| `GET` | `/api/dashboard/stream` | – | `200`, `text/event-stream`: `dashboard` esemény `Dashboard` tartalommal; kapcsolódáskor azonnal, utána minden változáskor | `401` |

- Minden végpont admin és kolléga belépéssel is elérhető; a hívót az `0001` szerinti
  `Authorization: Bearer <access token>` fejléc azonosítja.
- A böngésző beépített `EventSource`-a nem tud fejlécet küldeni, ezért a frontend a
  streamet `fetch`-csel olvassa (Bearer fejléccel); a token nem kerül az URL-be.
- Ha a stream közben lejár az access token, a frontend frissíti a tokent, és
  újrakapcsolódik.
- A `DONE` lista a kész %-hoz tartozó feladatokat adja.

```jsonc
// Dashboard
{
  "sprint": {
    "name": "HUGO Sprint 42",
    "startDate": "2026-10-20T08:00:00Z",
    "endDate": "2026-11-03T16:00:00Z",
    "elapsedPercent": 50,
    "daysRemaining": 7
  } /* null, ha nincs aktív sprint */,
  "kpis": { "total": 40, "donePercent": 62, "inProgress": 9, "blocked": 2, "unassigned": 3 },
  "updatedAt": "2026-10-27T10:15:00Z"
}

// Issue
{
  "key": "HUGO-123",
  "summary": "...",
  "status": "Blocked",
  "statusCategory": "TODO" | "IN_PROGRESS" | "DONE",
  "assignee": { "name": "...", "avatarUrl": "..." } /* null, ha nincs */,
  "url": "https://<cég>.atlassian.net/browse/HUGO-123"
}
```

## Adatmodell

Új Flyway migráció. Időpontok ISO-8601 UTC szövegként.

| Tábla | Oszlop | Típus | Kötelező | Megjegyzés |
|---|---|---|---|---|
| `jira_sprint` | `id` | INTEGER | igen | Jira sprint-azonosító (elsődleges kulcs) |
| | `name` | TEXT | igen | |
| | `state` | TEXT | igen | `active` / `closed` / `future` |
| | `start_date` | TEXT | nem | |
| | `end_date` | TEXT | nem | |
| `jira_issue` | `id` | TEXT | igen | Jira feladat-azonosító (elsődleges kulcs) |
| | `issue_key` | TEXT | igen | pl. `HUGO-123`, egyedi |
| | `sprint_id` | INTEGER | nem | → `jira_sprint.id`; üres, ha kikerült a sprintből |
| | `summary` | TEXT | igen | |
| | `status_name` | TEXT | igen | pl. `Blocked` |
| | `status_category` | TEXT | igen | `TODO` / `IN_PROGRESS` / `DONE` |
| | `assignee_account_id` | TEXT | nem | Jira `accountId`; üres = gazdátlan |
| | `assignee_name` | TEXT | nem | |
| | `assignee_avatar_url` | TEXT | nem | a Jira feladat assignee-jének avatar URL-je; üres, ha gazdátlan |
| | `jira_updated_at` | TEXT | igen | a feladat utolsó módosítása a Jirában; régebbi adat nem írja felül az újabbat |
| `jira_sync_state` | `id` | INTEGER | igen | egyetlen sor |
| | `last_synced_at` | TEXT | nem | a legutóbbi sikeres lekérdezés ideje; ettől kezdve kérdezi a változásokat |

## Beállítás

- A Jira-kapcsolat ugyanazokból a környezeti változókból jön, mint a `0002`-ben
  (HUGO-s Jira-felhasználó e-mail címe + API tokenje). Soha nem a repóban.
- Új beállítás: a lekérdezés időköze (alapérték 30 másodperc).

## Elfogadási kritériumok

A Jira REST-hívásait a tesztek helyettesítő (mock) Jira-szerverrel váltják ki; valódi
Jira-hívás teszt közben nem történik. Az időzített lekérdezést a tesztek közvetlenül
indítják, nem várnak az időzítőre.

**KPI-k**

1. **Adott** egy aktív sprint 10 feladattal: 4 „Done”, 3 „In Progress” kategóriájú (ebből
   1 `Blocked` státuszú), 3 „To Do” kategóriájú (ebből 2 assignee nélkül), **amikor**
   lekérem a dashboardot, **akkor** `total = 10`, `donePercent = 40`, `inProgress = 3`,
   `blocked = 1`, `unassigned = 2`.
2. **Adott** egy aktív sprint feladatok nélkül, **amikor** lekérem a dashboardot,
   **akkor** minden KPI 0, és a `donePercent` 0 (nem hiba).
3. **Adott** 3 feladatból 1 kész, **amikor** lekérem a dashboardot, **akkor** a
   `donePercent` 33.
4. **Adott** nincs aktív sprint, **amikor** lekérem a dashboardot, **akkor** a `sprint`
   `null`, és a felület „Nincs aktív sprint” üzenetet mutat.

**Haladási sáv**

5. **Adott** egy 14 napos sprint, amelynek 7 napja telt el, **amikor** lekérem a
   dashboardot, **akkor** `elapsedPercent = 50` és `daysRemaining = 7`.
6. **Adott** egy sprint, amelynek a vége már elmúlt, de még aktív, **amikor** lekérem,
   **akkor** `elapsedPercent = 100` és `daysRemaining = 0`.

**Lefúrás**

7. **Adott** az 1. kritérium sprintje, **amikor** a `BLOCKED` listát kérem, **akkor**
   pontosan az 1 `Blocked` feladatot kapom kulccsal, összefoglalóval, státusszal,
   assignee-vel és Jira URL-lel.
8. **Adott** az 1. kritérium sprintje, **amikor** az `UNASSIGNED` listát kérem, **akkor**
   pontosan a 2 assignee nélküli feladatot kapom, `assignee = null` értékkel.
9. **Adott** ismeretlen `tile` érték, **amikor** a listát kérem, **akkor** `400`-at kapok.
10. **Adott** a dashboard a felületen, **amikor** egy csempére, majd egy feladatra
    kattintok, **akkor** megjelenik a szűrt lista, majd új lapon a feladat Jira-oldala.

**Időzített lekérdezés és élő frissítés**

11. **Adott** egy pillanatkép, és a Jirában egy feladat `Blocked` státuszba került,
    **amikor** lefut a lekérdezés, **akkor** a következő dashboard-lekérésben a
    `blocked` eggyel nő.
12. **Adott** egy új feladat az aktív sprintben, **amikor** lefut a lekérdezés, **akkor**
    a `total` eggyel nő.
13. **Adott** egy feladat, amelyet a Jirában kivettek az aktív sprintből, **amikor**
    lefut a lekérdezés, **akkor** a feladat nem számít bele a KPI-kba.
14. **Adott** egy feladat, amelyet a Jirában töröltek, **amikor** lefut a
    törlésellenőrzés, **akkor** a feladat kikerül a pillanatképből.
15. **Adott** egy lekérdezés, **amikor** a Jirát hívja, **akkor** csak a legutóbbi sikeres
    lekérdezés óta módosult feladatokat kéri le, és sikeres futás után frissül a
    `last_synced_at`.
16. **Adott** egy lekérdezés, amelyben a Jira hibát ad (`500`), **amikor** lefut,
    **akkor** a pillanatkép és a `last_synced_at` változatlan marad, és a következő
    lekérdezés ugyanattól az időponttól kérdez.
17. **Adott** egy lekérdezés, amelyre a Jira `429`-et ad várakozási idővel, **amikor**
    lefut, **akkor** a következő lekérdezés nem indul a várakozási idő letelte előtt.
18. **Adott** egy régebbi `updated` idejű adat egy már frissebb feladatról, **amikor**
    feldolgozásra kerül, **akkor** nem írja felül a frissebb állapotot.
19. **Adott** egy megnyitott SSE-kapcsolat, **amikor** egy lekérdezés változást talál,
    **akkor** a kliens megkapja a friss `Dashboard` adatot; ha nincs változás, nem kap
    eseményt.
20. **Adott** egy belépés nélküli kliens, **amikor** a `/api/dashboard/stream`-re
    kapcsolódik, **akkor** `401`-et kap.
21. **Adott** egy lezárult sprint és egy újonnan elindult, **amikor** lefut a lekérdezés,
    **akkor** a dashboard az új sprint adatait mutatja.

**Kezdeti betöltés**

22. **Adott** üres pillanatkép és elérhető Jira, **amikor** a backend elindul, **akkor**
    az aktív sprint és minden feladata elmentődik.
23. **Adott** a Jira nem elérhető induláskor, **amikor** a backend elindul, **akkor** az
    app elindul, a napló tartalmazza az okot token nélkül, és a dashboard „Az adatok nem
    érhetők el” üzenetet mutat.

**Jogosultság és felület**

24. **Adott** belépés nélküli állapot, **amikor** a `/api/dashboard` végpontot hívom,
    **akkor** `401`-et kapok.
25. **Adott** adminként és kollégaként belépett felhasználó, **amikor** lekéri a
    dashboardot, **akkor** mindkettő `200`-at kap.
26. **Adott** a dashboard a felületen, **amikor** egy háttérbeli változás érkezik,
    **akkor** a csempék oldalújratöltés nélkül frissülnek.
27. **Adott** a dashboard a felületen, **amikor** megszakad az SSE-kapcsolat,
    **akkor** megjelenik az „Élő frissítés szünetel” jelzés, és újrakapcsolódás után
    eltűnik.
28. **Adott** a dashboard világos és sötét témában, **amikor** megjelenik egy státusz a
    csempén és a listában, **akkor** mindkét helyen ugyanazt a színt kapja.

## Nincs benne (out of scope)

- Az intent 2. fázisa (csapattag-kártyák, hőtérkép, Tempo-órák) és 3. fázisa
  (flow-diagramok, eseményfolyam, nézetek, heti összefoglaló).
- Trendgörbék.
- **Időszakváltó** (előző sprint, utolsó 30 nap): később, ha elég adat gyűlt össze; az
  ehhez szükséges adatok már most megmaradnak.
- AI-funkciók.
- Bármilyen írás a Jirába.
- Jira webhook: későbbi, saját intent. A webhook akkor csak jelez, és azonnal lefuttatja
  ugyanezt a lekérdezést; a polling biztonsági háló marad.
- Automation szabály, felhős közvetítő.
- WebSocket (az SSE elég, mert az adat csak a szervertől megy a böngésző felé).
- Tempo-adatok.

## Nyitott kérdések

- Nincs. Eldöntve: törlésellenőrzés minden lekérdezéskor; 30 másodperces időköz; a
  státusz neve `Blocked`; a dashboardot minden belépett felhasználó látja; az
  időszakváltó később jön; Jira-oldali értesítés (webhook, Automation) nincs, mert a
  HUGO projektben nincs projektadmin.
