# Spec: <rövid cím>

- **ID:** <id>-<slug>
- **Státusz:** Draft
- **Intent:** [intent.md](intent.md)
- **Dátum:** <ÉÉÉÉ-HH-NN>

## Összefoglaló

<1–3 mondat: mit tud a rendszer a változás után, amit ma nem.>

## Viselkedés

### Fő folyamat

1. <a felhasználó …>
2. <a rendszer …>

### Hibaesetek

- <érvénytelen bemenet → mit lát a felhasználó>
- <nem létező erőforrás → …>

## API

| Metódus | Út | Kérés | Válasz | Hibák |
|---|---|---|---|---|
| `GET` | `/api/...` | – | `200` + <séma> | `404` |

<Ha szükséges, a kérés/válasz törzse JSON példával.>

## Adatmodell

| Tábla | Oszlop | Típus | Kötelező | Megjegyzés |
|---|---|---|---|---|
| `<tabla>` | `id` | INTEGER | igen | elsődleges kulcs |

## Elfogadási kritériumok

1. **Adott** <kiinduló állapot>, **amikor** <művelet>, **akkor** <elvárt eredmény>.
2. …

## Nincs benne (out of scope)

- <amit ez a spec szándékosan nem fed le>

## Nyitott kérdések

- <a /plan előtt meg kell válaszolni>
