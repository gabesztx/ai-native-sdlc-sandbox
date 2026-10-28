# Spec: Dashboard lista child route navigációval

- **ID:** 0006-dashboard-list-navigation
- **Státusz:** Approved
- **Intent:** [intent.md](intent.md)
- **Dátum:** 2026-10-28

## Összefoglaló

A dashboard oldalon egy KPI-kártyára kattintva a rendszer mostantól önálló, saját
URL-lel rendelkező aloldalra navigál animált átmenettel, amely teljes nézetváltással
(a KPI-kártyák és a sprint progress card elrejtésével) jeleníti meg a szűrt
issue-listát, modernebb, kártyaszerű lista-elemekkel. Az aloldalról explicit
vissza-interakcióval lehet visszatérni, és a böngésző vissza/előre gombja, valamint a
közvetlen URL-megnyitás natívan működik.

## Viselkedés

### Tile → URL szegmens

| Tile (backend érték) | URL szegmens |
|---|---|
| `ALL` | `all` |
| `DONE` | `done` |
| `IN_PROGRESS` | `in-progress` |
| `BLOCKED` | `blocked` |
| `UNASSIGNED` | `unassigned` |

### Fő folyamat

1. A felhasználó a dashboard főnézeten (KPI-kártyák + sprint progress card) az egyik
   KPI-kártyára kattint.
2. A rendszer animált átmenettel a kártyához tartozó lista aloldalra navigál, saját
   URL-lel (pl. `/dashboard/blocked`); a KPI-kártyák és a sprint progress card ezalatt
   nem látszik.
3. A lista aloldal tetején megjelenik egy fejléc explicit vissza-elemmel és a szűrő
   nevével, alatta a szűrt issue-lista modern, kártyaszerű lista-elem megjelenéssel
   (kulcs, összegzés, állapot, hozzárendelt személy).
4. A felhasználó a vissza-elemre kattint; a rendszer animált átmenettel visszatér a
   dashboard főnézetre, ahol a KPI-kártyák és a sprint progress card ismét látszik.
5. A felhasználó közvetlenül megnyit egy érvényes lista aloldal URL-t (pl. linkből vagy
   bookmarkból); a rendszer rögtön a megfelelő szűrt listát tölti be, a dashboard
   főnézet megjelenítése nélkül.
6. A felhasználó a böngésző vissza/előre gombjával lépked a dashboard főnézet és a
   meglátogatott lista aloldalak között; ez natív böngésző-history navigáció, nem
   egyedi JavaScript-logika.

### Hibaesetek

- Érvénytelen vagy ismeretlen URL szegmens (pl. `/dashboard/xyz`) → a rendszer a
  dashboard főnézetre irányítja át a felhasználót.
- A szűrt lista lekérdezése hibára fut → a felhasználó üres listát lát (a meglévő
  hibakezelési viselkedés szerint), a lista aloldal navigációja és animációja
  változatlanul lezajlik.

## Felület

- **Elrendezés:** a dashboard route egy paraméteres gyermek-útvonalat kap a lista
  aloldalhoz. A dashboard főnézet (KPI-kártyák + sprint progress card) és a lista
  aloldal kölcsönösen kizárják egymást: egyszerre csak az egyik látszik.
- **Komponensek és állapotaik:**
  - Lista aloldal betöltés közben: a meglévőhöz hasonló skeleton-sorok.
  - Lista aloldal üres szűrt eredménnyel: a meglévő `empty-state` komponens.
  - Lista aloldal kitöltve: modern, kártyaszerű lista-elemek (nem puszta szöveges
    sorok) — kulcs, összegzés, állapot-jelölés (tag), hozzárendelt személy (avatar +
    név, vagy "nincs" jelzés).
  - A lista aloldal fejlécén mindig megjelenik egy vissza-elem és a szűrő neve.
- **Reszponzivitás:** mobil nézetben a lista-kártyák egy oszlopban jelennek meg; a
  metaadatok (állapot, hozzárendelt) a cím/összegzés alatt, külön sorban törnek, hogy
  szűk szélességen is olvashatók maradjanak.
- **Animáció:** a dashboard főnézet és a lista aloldal közti átmenet CSS-alapú,
  natív Angular `animate.enter` / `animate.leave` szintaxissal megvalósított
  csúszó+fade (slide+fade) animáció, kb. 200–300 ms időtartammal. Közvetlen URL-
  megnyitáskor a lista aloldal ugyanazzal a belépő animációval jelenik meg.
- **Design tokenek:** nincs új design token; a meglévő PrimeNG-alapú (`0004-ui-design`)
  színek, térközök és tipográfia érvényesek.

## API

Nincs API-változás. A meglévő végpontok (`GET /api/dashboard`,
`GET /api/dashboard/issues?tile=...`) változatlanok maradnak; a lista aloldal ugyanazt
a lekérdezést használja, mint ma a query paraméteres megjelenítés.

## Adatmodell

Nincs adatmodell-változás.

## Elfogadási kritériumok

1. **Adott** a dashboard főnézet (KPI-kártyák és sprint progress card látható),
   **amikor** a felhasználó egy KPI-kártyára kattint, **akkor** a rendszer a
   kártyához tartozó lista aloldalra navigál a megfelelő URL-lel (a fenti táblázat
   szerint), és a KPI-kártyák/sprint progress card eltűnik.
2. **Adott** egy nyitott lista aloldal, **amikor** a felhasználó a vissza-elemre
   kattint, **akkor** a rendszer visszanavigál a dashboard főnézetre, és a
   KPI-kártyák/sprint progress card ismét látszik.
3. **Adott** egy érvényes lista aloldal URL (pl. `/dashboard/blocked`), **amikor** a
   felhasználó közvetlenül megnyitja ezt az URL-t, **akkor** a rendszer rögtön a
   megfelelő szűrt listát tölti be, a dashboard főnézet megjelenítése nélkül.
4. **Adott**, hogy a felhasználó egy lista aloldalon van, **amikor** a böngésző vissza
   gombjára kattint, **akkor** a rendszer natív böngésző-history alapján a dashboard
   főnézetre navigál.
5. **Adott** egy érvénytelen/ismeretlen URL szegmens (pl. `/dashboard/xyz`), **amikor**
   a felhasználó megnyitja, **akkor** a rendszer a dashboard főnézetre irányítja át.
6. **Adott** egy lista aloldal, amelyen az issue-k lekérdezése folyamatban van,
   **amikor** az oldal megjelenik, **akkor** a felhasználó betöltési (skeleton)
   állapotot lát.
7. **Adott** egy lista aloldal, **amikor** az adott szűrőhöz nincs egyetlen issue sem,
   **akkor** a felhasználó a meglévő `empty-state` komponenst látja.
8. **Adott** egy lista aloldal kitöltött issue-listával, **amikor** megjelenik,
   **akkor** minden issue modern, kártyaszerű elrendezésben jelenik meg, feltüntetve a
   kulcsot, összegzést, állapotot és a hozzárendelt személyt (vagy "nincs" jelzést).
9. **Adott** a dashboard főnézet vagy egy lista aloldal, **amikor** a kettő közötti
   navigáció (bármelyik irányban) lezajlik, **akkor** a felhasználó CSS-alapú,
   natív Angular `animate.enter`/`animate.leave` átmenetet lát.

## Nincs benne (out of scope)

- API- vagy adatmodell-változás.
- A sprint progress card vagy a KPI-kártyák adatlekérdezési logikájának módosítása —
  csak a láthatóságuk változik a főnézet/aloldal állapottól függően.
- A korábbi toggle-viselkedés (ugyanarra a kártyára kattintva bezáródás) megtartása —
  ezt az intentben eldöntött explicit vissza-interakció váltja fel.
- Új animációs JavaScript-könyvtár bevezetése.

## Nyitott kérdések

Nincs.
