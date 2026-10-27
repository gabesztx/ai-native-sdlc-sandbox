# HUGO Feature Request Board — AI-native SDLC sandbox

Tanuló sandbox, amely az Anthropic AI-Native SDLC Playbookját követi. A termék a
HUGO Feature Request Board: a HUGO csapat tagjai egymást értékelik az admin által
összeállított űrlapokon, és egy Jira csapat-dashboardon látják a fejlesztés állapotát
(részletek: `docs/changes/`). A valódi téma a körülötte lévő agent-munkafolyamat:
intent → spec → plan → build → review.

## Állapot

Az app váza létezik (`apps/web`, `apps/api`, `contracts/openapi.json`). Kész és a teljes
láncon átment változások:

- `0001-peer-review-forms` — kollégák értékelése űrlapokkal
- `0004-ui-design` — PrimeNG-alapú, reszponzív felület
- `0005-angular-refactor` — feature-first Angular-struktúra, service-réteg, Signal Forms

A `0002-jira-user-import` és a `0003-jira-team-dashboard` specje még Draft. A lenti
parancsok működnek; ha egy parancs mégsem létezik, mondd meg, ne találj ki helyette másikat.

## Stack

- **Web** (`apps/web`): Angular 22 — standalone komponensek, signals, zoneless,
  beépített control flow (`@if`, `@for`). Teszt: Vitest.
- **API** (`apps/api`): Java 25 + Spring Boot 4.1, Maven wrapper (`./mvnw`).
- **Adatbázis**: SQLite (egyetlen fájl) + Flyway migrációk. Adatelérés Spring
  `JdbcClient`-tel, sima SQL-lel — JPA nélkül.
- **Szerződés**: a springdoc által generált OpenAPI, kiírva a `contracts/openapi.json`
  fájlba (commitolva). Az Angular kliens ebből generálódik, soha nem kézzel írt.

## Repo térkép

```
CLAUDE.md                 közös szabályok (ez a fájl)
REVIEW.md                 review súlyossági szabályok
apps/web/                 Angular app        (saját CLAUDE.md-vel)
apps/api/                 Spring Boot API    (saját CLAUDE.md-vel)
contracts/openapi.json    API-szerződés (generált, commitolt)
docs/changes/<id>-<slug>/ változásonként intent.md, spec.md, plan.md
.claude/skills/           SDLC eljárások: intent, spec, plan, review
.claude/agents/           specializált subagentek (verifier, reviewerek)
```

## Parancsok

- `npm run check` — az egyetlen ellenőrző parancs: API-kliens generálás, web lint + test
  + build, utána `./mvnw verify` (benne az OpenAPI-szerződés teszt). Ez a feedback loop.
- `npm run dev:web` és `npm run dev:api` — web és API helyi futtatása, két külön terminálban.
- Egy teszt: `npm -w apps/web test -- --filter <minta>` / `cd apps/api && ./mvnw test -Dtest=<Osztály>`.
- `npm run contract:update` — a `contracts/openapi.json` újragenerálása API-változás után.
- `npm run api:client -w apps/web` — az Angular kliens generálása a szerződésből.

## Definition of done

Soha ne jelents egy feladatot késznek, amíg az `npm run check` nem zöld. Másold be az
eredmény összegző sorát. Ha piros, jelentsd a hibát — ne rejtsd el, ne hagyd ki, és ne
gyengíts meg tesztet.

## SDLC munkafolyamat

Minden nem triviális változás ezt a láncot követi. Minden lépés egy fájlt ír, és emberi
jóváhagyásig megáll, mielőtt a következő jönne.

1. `/intent` → `docs/changes/<id>-<slug>/intent.md` — probléma, cél, nyitott kérdések
2. `/spec`   → `spec.md` — viselkedés, API, adatmodell, elfogadási kritériumok
3. `/plan`   → `plan.md` — sorrendbe tett lépések, érintett fájlok, tesztek. **Emberi jóváhagyási kapu.**
4. Build — csak a `plan.md`-t valósítsd meg; futtasd az `npm run check`-et; a verifier subagent megerősíti
5. `/review` → párhuzamos reviewer subagentek, összefésülve a `REVIEW.md` szerint

Triviális változás (elírás, konfig-módosítás) mehet egyből a 4. lépésre, de mondd ki,
hogy kihagyod a korábbi lépéseket.

## Munka agentekkel

- A fő session az **orchestrator**. Delegál a `.claude/agents/` subagentjeinek, és csak
  az összefoglalójukat kapja vissza.
- Az agentek a `docs/changes/` fájljain keresztül adják át egymásnak a munkát, nem
  chatben. Ami nincs benne az `intent.md` / `spec.md` / `plan.md` fájlban, arról nem
  született megállapodás.
- Aki a kódot írta, nem reviewzhatja. A review-t reviewer subagentek végzik.
- A reviewerek és a verifier csak olvasnak: jelentenek, nem szerkesztenek.
- Párhuzamos munka: egy feladat = egy git worktree = egy branch = egy PR.

## Architektúra szabályok

- API-változás a `spec.md`-ben kezdődik, utána jön a kód, majd az újragenerált
  `contracts/openapi.json`.
- Sémaváltozás csak új Flyway migrációval (`V<n>__<leiras>.sql`).
- Az API csomagolása feature szerint (pl. `form/`, `user/`, `dashboard/`), nem rétegenként.

## Konvenciók

- A dokumentáció, az agent-fájlok (skillek, agentek) és a commit üzenetek egyelőre
  magyarul készülnek; a kód, az azonosítók és a kódkommentek angolul.

## Határok

- Új függőség hozzáadása előtt kérdezz.
- Soha ne írj át tesztet azért, hogy átmenjen; javítsd a kódot, vagy jelezd, miért hibás a teszt.
- Soha ne szerkessz meglévő Flyway migrációt; írj újat.
- Soha ne commitolj secretet vagy `.env` fájlt; `.env` fájlt ne olvass.
- Soha ne force-pusholj.

## Tipikus hibák

<!-- Soronként egy visszatérő agent-hiba és a javítása. Röviden. -->

- Friss worktree-ben a build elhasal (`Cannot find module './core/api/...'`) → előbb `npm ci`, majd `npm run api:client -w apps/web`; a generált kliens gitignore-olt.
- Lépés commitolása piros check mellett → commit csak zöld `npm run check` után.
- Spec vagy plan `Approved`-ra állítása, mielőtt az ember elolvasta → mutasd meg a fájlt, és várd meg az explicit OK-t.
- Build vagy scaffold indítása jóváhagyás nélkül → a „kezdheted” csak az épp megbeszélt lépésre vonatkozik.
- `rxResource`-os komponens tesztje: a kérés csak change detection után indul → `fixture.detectChanges()` az `expectOne` előtt.
- `springdoc.*` pretty-print beállítások az `application.yml`-ben hatástalanok → ne próbáld újra, az `openapi.json` egysoros marad.
