# Plan: <rövid cím>

- **ID:** <id>-<slug>
- **Státusz:** Draft
- **Spec:** [spec.md](spec.md)
- **Dátum:** <ÉÉÉÉ-HH-NN>

## Megközelítés

<2–4 mondat: a megoldás lényege, és miért ez.>

## Lépések

| # | Mit | Fájl(ok) | AC | Párhuzamosítható |
|---|---|---|---|---|
| 1 | <Flyway migráció a … táblához> | `apps/api/src/main/resources/db/migration/V<n>__<leiras>.sql` | AC1 | – |
| 2 | <…> | `…` | AC1, AC2 | 3-mal |

## Tesztek

| AC | Teszt | Fájl |
|---|---|---|
| AC1 | <mit ellenőriz> | `…Test.java` / `….spec.ts` |

## Ellenőrzés

- `npm run check` zöld minden lépés után.
- <kézi ellenőrzés, ha kell: mit nézzen meg a verifier>

## Kockázatok

- <kockázat> → <mit teszünk ellene>

## Nincs benne

- <amit ez a plan szándékosan nem csinál meg>
