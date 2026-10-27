---
name: intent
description: Egy új ötletet, problémát vagy változtatási kérést rögzít a docs/changes/<id>-<slug>/intent.md fájlban — ez az SDLC lánc első lépése. Akkor használd, ha a felhasználó új feature-t, hibát vagy változtatást ír le, vagy a /intent parancsot adja ki.
---

# Intent

Az intent azt rögzíti, **mit és miért** akarunk — nem azt, hogyan. Megoldást,
technológiát, fájlneveket itt ne javasolj; az a `/spec` és a `/plan` dolga.

## Lépések

1. **Kérdezz, amíg nem tudod ezeket:**
   - Mi a probléma, és kinek fáj?
   - Mi a kívánt eredmény — miből látszik, hogy sikerült?
   - Kiket és milyen részeket érint (web, API, adat)?
   - Van-e korlát (határidő, ami nem változhat, ami kimarad)?

   Egyszerre legfeljebb 3 kérdést tegyél fel. Ami a kérésből egyértelmű, azt ne kérdezd meg.

2. **Azonosító:** nézd meg a `docs/changes/` mappát, és vedd a következő négyjegyű
   sorszámot (`0001`, `0002` …). A slug rövid, kisbetűs, kötőjeles angol kifejezés
   (pl. `0003-vote-on-request`).

3. **Töltsd ki a [template.md](template.md) sablont**, és mentsd ide:
   `docs/changes/<id>-<slug>/intent.md`.
   - Amire nincs válasz, az a **Nyitott kérdések** közé kerül. Soha ne találj ki választ.
   - A státusz: `Draft`.

4. **Mutasd meg** a felhasználónak a kész fájlt, és **állj meg**. A `/spec` csak akkor
   indulhat, ha a felhasználó jóváhagyta, és a státuszt `Approved`-ra állítottad.

## Ne csináld

- Ne írj spec-et, plant vagy kódot.
- Ne hozz létre más fájlt, mint az `intent.md`.
- Ne folytasd a `/spec`-kel jóváhagyás nélkül.
