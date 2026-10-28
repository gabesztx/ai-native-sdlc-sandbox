# Intent: Dashboard lista child route navigációval

- **ID:** 0006-dashboard-list-navigation
- **Státusz:** Approved
- **Dátum:** 2026-10-28

## Probléma

A dashboard oldalon egy KPI-kártyára kattintva a megfelelő szűrt lista query
paraméterrel (`dashboard?tile=ALL`) töltődik be, és ugyanazon az oldalon, a kártyák és
a sprint progress card alatt jelenik meg. Ez nem hat önálló navigációnak, nincs
átmenet/animáció a megjelenéskor, és a lista elemek vizuális megjelenése nagyon
egyszerű (puszta szöveges sorok), nem illeszkedik a `0004-ui-design`-ban bevezetett
modernebb felülethez.

## Kívánt eredmény

- Egy KPI-kártyára kattintva a szűrt lista külön aloldalként (Angular child route)
  töltődik be, animált átmenettel.
- Az aloldal teljesen átveszi a nézetet: amíg a lista aloldal aktív, a KPI-kártyák sora
  és a sprint progress card nem látszik.
- Az aloldalról explicit "vissza" interakcióval (nem a kártyára való újbóli
  kattintással) lehet visszajutni a dashboard főnézetéhez.
- Minden szűrt nézetnek saját, megosztható/bookmarkolható URL-je van (pl.
  `/dashboard/blocked`), és a böngésző vissza/előre gombja natívan lépked a dashboard
  főnézet és az egyes szűrt aloldalak között.
- A lista elemek megjelenése modernebb, letisztultabb (nem puszta szöveges sorok).

## Érintettek

- **Felhasználók:** a HUGO csapat tagjai, akik a Jira csapat-dashboardot nézik.
- **Rendszerrészek:** csak web (`apps/web`, `features/dashboard`); API és adatmodell
  nem érintett.

## Korlátok

- A meglévő élő frissítés (`DashboardStreamService`, SSE) és a KPI-kártyák
  adatlogikája funkcionálisan nem változhat — csak az, hogy a szűrt lista hogyan és
  hol jelenik meg.
- A `0004-ui-design` PrimeNG-alapú vizuális nyelvét kell követni, nem egy új
  design-rendszert bevezetni.

## Nincs benne (out of scope)

- API- vagy adatmodell-változás; a backend (`apps/api`) nem érintett.
- A sprint progress card vagy a KPI-kártyák adatlogikájának módosítása — csak a
  láthatóságuk változik attól függően, hogy a dashboard főnézet vagy egy lista aloldal
  aktív.
- Új függőség (pl. animációs könyvtár) bevezetése.

## Nyitott kérdések

- Az animáció pontos stílusa (pl. csúszás, fade, egyéb) nincs meghatározva — ezt a
  `/spec` vagy a `/plan` határozza meg a projekt natív CSS-animáció szabályai szerint.
- A "vissza" interakció pontos formája (pl. gomb, fejléc visszanyíl, breadcrumb) nincs
  meghatározva.
- A lista elemek pontos új vizuális megjelenése (milyen adatok, milyen elrendezés)
  nincs részletezve — ezt a `/spec` dolgozza ki.
