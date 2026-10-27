---
name: plan
description: Egy jóváhagyott spec.md-ből elkészíti a plan.md-t (sorrendbe tett lépések, érintett fájlok, tesztek) a docs/changes/<id>-<slug>/ mappában — az SDLC lánc harmadik lépése és az implementáció előtti emberi jóváhagyási kapu. Akkor használd, ha a felhasználó implementációs tervet kér egy spechez, vagy a /plan parancsot adja ki.
---

# Plan

A plan azt rögzíti, **hogyan valósítjuk meg** a spec-et: milyen sorrendben, mely
fájlokban, milyen tesztekkel. A plan az implementáló agent egyetlen utasítása —
ami nincs benne, az nem készül el.

## Előfeltétel

- Keresd meg a változás mappáját: `docs/changes/<id>-<slug>/` (ha a felhasználó nem
  adta meg, kérdezd meg, melyik).
- A `spec.md` státusza legyen `Approved`. Ha nem az, **állj meg**, és szólj.

## Lépések

1. **Felderítés (csak olvasás):** olvasd el az `intent.md`-t, a `spec.md`-t, a
   `CLAUDE.md`-t (és a beágyazott `apps/*/CLAUDE.md`-ket), majd nézd meg a kódban,
   hol és hogyan illeszkedik a változás. Ebben a fázisban semmit ne módosíts.

2. **Töltsd ki a [template.md](template.md) sablont**, és mentsd ide:
   `docs/changes/<id>-<slug>/plan.md`.
   - **Lépések:** kicsik és sorrendbe tettek; mindegyik után zöldnek kell maradnia a
     `npm run check`-nek. Lépésenként: mit, melyik fájlban, és melyik elfogadási
     kritériumot (`AC1`, `AC2` …) szolgálja.
   - **Tesztek:** minden elfogadási kritériumhoz legalább egy teszt, megnevezve a
     tesztfájlt. A teszt a hozzá tartozó kód előtt vagy vele együtt készül.
   - **Sorrend:** adatbázis-migráció → API → `contracts/openapi.json` újragenerálása →
     generált kliens → frontend.
   - **Kockázatok:** ami elromolhat, és mit teszünk ellene.
   - **Párhuzamosíthatóság:** jelöld, mely lépések függetlenek egymástól (pl. két
     külön feature-mappa) — ezeket külön agent, külön worktree-ben végezheti.
   - A státusz: `Draft`.

3. **Mutasd meg** a felhasználónak a kész fájlt, és **állj meg**. Ez a jóváhagyási
   kapu: implementáció csak akkor kezdődhet, ha a felhasználó kifejezetten jóváhagyta,
   és a státuszt `Approved`-ra állítottad.

## Ne csináld

- Ne írj kódot, és ne módosíts semmit a `plan.md`-n kívül.
- Ne tervezz olyat, ami nincs a spec-ben. Ha valami hiányzik a spec-ből, szólj, és a
  felhasználó dönt.
- Ne kezdd el az implementációt jóváhagyás nélkül.
