# Feature Request Board — AI-native SDLC sandbox

Tanuló sandbox, amely az Anthropic AI-Native SDLC Playbookját követi. A termék egy kis
Feature Request Board (kérések beküldése, szavazás, státusz követése); a valódi téma a
körülötte lévő agent-munkafolyamat: intent → spec → plan → build → review.

## Állapot

Az app még NINCS létrehozva. Egyelőre csak az agent-konfiguráció létezik. A lenti
parancsok a célállapotot írják le — ha egy parancs még nem létezik, mondd meg, ne találj
ki helyette másikat.

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

## Parancsok (cél)

- `pnpm check` — az egyetlen ellenőrző parancs: web lint + test + build, utána
  `./mvnw verify`. Ez a feedback loop.
- `pnpm dev` — web és API helyi futtatása.
- Egy teszt: `pnpm --filter web test -- <minta>` / `./mvnw -pl apps/api test -Dtest=<Osztály>`.

## Definition of done

Soha ne jelents egy feladatot késznek, amíg a `pnpm check` nem zöld. Másold be az
eredmény összegző sorát. Ha piros, jelentsd a hibát — ne rejtsd el, ne hagyd ki, és ne
gyengíts meg tesztet.

## SDLC munkafolyamat

Minden nem triviális változás ezt a láncot követi. Minden lépés egy fájlt ír, és emberi
jóváhagyásig megáll, mielőtt a következő jönne.

1. `/intent` → `docs/changes/<id>-<slug>/intent.md` — probléma, cél, nyitott kérdések
2. `/spec`   → `spec.md` — viselkedés, API, adatmodell, elfogadási kritériumok
3. `/plan`   → `plan.md` — sorrendbe tett lépések, érintett fájlok, tesztek. **Emberi jóváhagyási kapu.**
4. Build — csak a `plan.md`-t valósítsd meg; futtasd a `pnpm check`-et; a verifier subagent megerősíti
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
- Az API csomagolása feature szerint (`request/`, `vote/`), nem rétegenként.

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
