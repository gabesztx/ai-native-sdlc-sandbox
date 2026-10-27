# Változások (docs/changes)

Minden nem triviális változásnak saját mappája van. A mappa a változás teljes
audit trailje: mit akartunk, mit specifikáltunk, hogyan terveztük — és a git history
mutatja, ki és mikor hagyta jóvá.

## Elnevezés

```
docs/changes/<id>-<slug>/
```

- `<id>`: négyjegyű, folyamatos sorszám (`0001`, `0002` …).
- `<slug>`: rövid, kisbetűs, kötőjeles angol kifejezés (pl. `0001-submit-request`).

## Tartalom és sorrend

| Fájl | Skill | Tartalom | Kapu |
|---|---|---|---|
| `intent.md` | `/intent` | probléma, kívánt eredmény, nyitott kérdések | `Approved` → jöhet a spec |
| `spec.md` | `/spec` | viselkedés, API, adatmodell, elfogadási kritériumok | `Approved` → jöhet a plan |
| `plan.md` | `/plan` | lépések, fájlok, tesztek, kockázatok | `Approved` → jöhet az implementáció |

Utána: implementáció → `verifier` subagent → `/review` (párhuzamos reviewerek).

## Státuszok

Minden fájl fejlécében: `Draft` → `Approved`. Csak ember állíthatja `Approved`-ra,
vagy az agent az ember kifejezett jóváhagyása után.

## Szabályok

- Jóváhagyott fájlt ne írj át csendben. Ha változni kell, jelezd, és a módosítás után
  a státusz visszakerül `Draft`-ra.
- Az agentek ezeken a fájlokon keresztül adják át egymásnak a munkát. Ami nincs
  bennük, arról nem született megállapodás.
