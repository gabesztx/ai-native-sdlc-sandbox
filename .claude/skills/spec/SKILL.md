---
name: spec
description: Egy jóváhagyott intent.md-ből elkészíti a spec.md-t (viselkedés, API, adatmodell, elfogadási kritériumok) a docs/changes/<id>-<slug>/ mappában — az SDLC lánc második lépése. Akkor használd, ha a felhasználó egy intent specifikálását kéri, vagy a /spec parancsot adja ki.
---

# Spec

A spec azt rögzíti, **mit csinál a rendszer** a változás után — kívülről nézve,
ellenőrizhetően. A belső megvalósítás (osztályok, fájlok, lépések) a `/plan` dolga.

## Előfeltétel

- Keresd meg a változás mappáját: `docs/changes/<id>-<slug>/` (ha a felhasználó nem
  adta meg, kérdezd meg, melyik).
- Az `intent.md` státusza legyen `Approved`. Ha nem az, **állj meg**, és szólj.
- Ha az `intent.md`-ben maradt nyitott kérdés, előbb ezeket tisztázd a felhasználóval.

## Lépések

1. **Olvasd el** az `intent.md`-t, a `CLAUDE.md` architektúra szabályait, és ha már
   létezik, a `contracts/openapi.json`-t és a meglévő Flyway migrációkat
   (`apps/api/src/main/resources/db/migration/`), hogy a spec illeszkedjen a meglévőhöz.

2. **Töltsd ki a [template.md](template.md) sablont**, és mentsd ide:
   `docs/changes/<id>-<slug>/spec.md`.
   - **Viselkedés:** felhasználói szemszögből, lépésenként, a hibaesetekkel együtt.
   - **API:** végpontonként metódus, út, kérés, válasz, hibakódok. Ez lesz az
     OpenAPI-szerződés alapja.
   - **Adatmodell:** táblák, oszlopok, típusok, kötelezőség, kapcsolatok. Csak az új
     vagy változó részeket írd le.
   - **Elfogadási kritériumok:** számozott, egyenként tesztelhető állítások
     `Adott … Amikor … Akkor …` formában. Minden kritériumból legalább egy teszt lesz.
   - Ami nem dönthető el, az a **Nyitott kérdések** közé kerül. Ne találj ki választ.
   - A státusz: `Draft`.

3. **Mutasd meg** a felhasználónak a kész fájlt, és **állj meg**. A `/plan` csak akkor
   indulhat, ha a felhasználó jóváhagyta, és a státuszt `Approved`-ra állítottad.

## Ne csináld

- Ne írj implementációs lépéseket, fájllistát vagy kódot.
- Ne módosítsd az `intent.md`-t. Ha a spec írása közben kiderül, hogy az intent hibás,
  szólj, és a felhasználó dönt.
- Ne folytasd a `/plan`-nel jóváhagyás nélkül.
