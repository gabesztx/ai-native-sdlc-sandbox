# Intent: Kollégák értékelése űrlapokkal

- **ID:** 0001-peer-review-forms
- **Státusz:** Approved
- **Dátum:** 2026-10-27

## Probléma

A cégen belüli HUGO csapat tagjai ma nem tudják egymást egy közös, egységes felületen
értékelni. Nincs olyan eszköz, amelyben egy admin értékelő űrlapot állíthat össze,
kiküldheti a csapattagoknak, és követheti, ki töltötte már ki. A kollégák sem látják egy
helyen, milyen értékelést kell kitölteniük.

## Kívánt eredmény

- Az **admin** létrehoz egy értékelő űrlapot:
  - megad egy **fő témát**;
  - ehhez tetszőleges számú, **saját megfogalmazású kérdést** ír;
  - a kérdés lehet **számozott skála** (a skálát az admin állítja be) vagy **szabad
    szöveges** válasz;
  - bármelyik kérdés lehet **kötelező vagy opcionális**.
- Egy űrlap **mindig egy adott személyről** szól: az admin kiválasztja, kit értékelnek
  vele.
- Az admin **kiküldi** az űrlapot. A címzettek a mentett HUGO csapat tagjai, az értékelt
  személy kivételével. Kiküldés után az űrlap nem módosítható.
- Senki nem értékeli saját magát: az értékelt személy nem lehet az űrlap címzettje.
- A **kolléga** látja a neki küldött, kitöltendő űrlapokat, kitölti és beküldi őket. A
  beküldött kitöltés nem módosítható.
- A kitöltés **állapota** (kitöltendő / beküldve) a kollégának és az adminnak is látszik.
- A kitöltéseket **csak az admin látja**, név szerint. A kollégák egymás válaszait nem
  látják.

**Sikeres, ha:** az admin létrehoz és kiküld egy űrlapot, a címzett kolléga kitölti és
beküldi, az állapot mindkét oldalon „beküldve” lesz, a választ pedig csak az admin látja.

## Érintettek

- **Felhasználók:**
  - **Admin:** előre beállított fiókkal, e-mail címmel és jelszóval lép be; űrlapot
    készít, kiküld, látja az állapotokat és a válaszokat. Nem tagja a csapatnak: őt nem
    értékelik, és ő nem kap kitöltendő űrlapot.
  - **Kolléga:** a csapattagok listájából választja ki magát; kitölti a neki küldött
    űrlapokat.
- **Rendszerrészek:** web, API, adat.

## Korlátok

- Két szerepkör van: admin és kolléga.
- A kezdőoldalon (belépés) két út van:
  - **admin belépés** e-mail címmel és jelszóval;
  - **kolléga belépés** egyszerű felhasználóválasztással, jelszó nélkül.
- Ebben a változásban a felhasználólista fix, kitalált felhasználókból áll; a Jirából
  való lekérés a `0002-jira-user-import` változás.
- A válaszokat csak az admin láthatja — a felületen és az API-ban is.
- A kiküldött űrlap és a beküldött kitöltés nem módosítható.
- A repó publikus: valódi nevek és céges adatok nem kerülhetnek bele.

## Nincs benne (out of scope)

- A felhasználólista lekérése a Jirából (`0002`).
- Határidő és emlékeztető.
- A beküldött kitöltés módosítása vagy visszavonása.
- Valódi bejelentkezés a kollégáknak.
- Admin regisztráció és admin fiókok kezelése (a fiók előre beállított).
- Összesítő kimutatás az értékelésekről.
- A Jira csapat-dashboard (`0003`).

## Nyitott kérdések

- Nincs.
