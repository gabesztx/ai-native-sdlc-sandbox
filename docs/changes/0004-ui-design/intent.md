# Intent: Modern, reszponzív UI design a HUGO Feature Request Boardnak

- **ID:** 0004-ui-design
- **Státusz:** Approved
- **Dátum:** 2026-10-27

## Probléma

A 0001-ben elkészült felület (bejelentkezés, admin űrlapkezelés, kolléga értékelések)
funkcionálisan kész, de vizuálisan dísztelen:

- a globális `styles.scss` gyakorlatilag üres, oldalanként csak néhány soros SCSS van;
- nincs egységes komponensrendszer;
- nincs reszponzív viselkedés mobilra;
- semmilyen animáció nincs.

A felület eddig egyszer sem volt tervezési téma: sem az intent, sem a spec, sem a plan
nem foglalkozott a megjelenéssel, csak a viselkedéssel. A további feature-ök (0002, 0003)
erre a felületre épülnek, ezért most érdemes rendbe tenni.

## Kívánt eredmény

- A HUGO Feature Request Board minden meglévő oldala egységes, modern, „dashboard”
  jellegű megjelenést kap:
  - bejelentkezés;
  - admin: űrlaplista, szerkesztő, részletek, csapattagok;
  - kolléga: értékeléseim, kitöltés.
- A megjelenés **letisztult, modern**, semleges színvilágú, egyetlen kiemelőszínnel.
- A felület **mobilon és asztali nézetben is jól használható** (reszponzív elrendezés).
- A navigáció a jelenlegi egyetlen vízszintes fejléc helyett:
  - asztali nézetben **bal oldali sidebar**;
  - mobil nézetben **alsó tab-bar**.
- **Animációk** segítik a visszajelzést és az állapotváltások érzékelését, pl.:
  - lista-betöltés;
  - állapotváltás;
  - beküldés visszajelzése;
  - oldalváltás.

  Csak CSS-alapú eszközökkel, a projekt animációs szabálya szerint
  (`@keyframes` / `transition` / `@starting-style`, `animate.enter` / `animate.leave`),
  nem `@angular/animations`-szel.
- **Világos és sötét téma:**
  - a felhasználó egy kapcsolóval vált közöttük;
  - az app világos módban indul;
  - a választás nem a rendszerbeállítást követi.
- **Modern, jól olvasható tipográfia** a böngésző alapértelmezett rendszer-betűtípusa
  helyett.
- A felület egy közös **komponensrendszerre** épül, nem úgy, hogy minden oldal újraírja
  a saját HTML/SCSS elemeit.

**Sikeres, ha:**
- egy felhasználó (admin vagy kolléga) asztali és mobil böngészőben is koherens, modern,
  reagáló felületet lát az összes meglévő oldalon;
- minden oldal ugyanazokat a komponenseket és átmeneteket használja;
- a 0001-ben elkészült viselkedéshez képest nincs funkcióvesztés.

## Érintettek

- **Felhasználók:** a HUGO csapat minden tagja, admin és kolléga szerepkörben is.
- **Rendszerrészek:** kizárólag a **web** (`apps/web`). Az API-t és az adatmodellt nem
  érinti. Nem ad hozzá új funkciót, és nem változtat a 0001-ben elfogadott viselkedésen,
  csak a meglévő oldalak megjelenését és a navigáció szerkezetét cseréli le.

## Korlátok

- **SCSS marad** (`apps/web/CLAUDE.md`: „Mindig SCSS — nem CSS, nem inline style”;
  Flexbox, Grid nem). Ezért esett a választás a **PrimeNG** komponenskönyvtárra (Aura
  téma). A Tailwind-alapú, shadcn-stílusú alternatívák (Spartan UI, Zard UI) ütköznének
  ezzel a szabállyal.
- **Új függőség csak jóváhagyással** jöhet be (`CLAUDE.md`). Ez a jóváhagyás a spec-ben
  megtörtént.
- **Animáció** csak natív CSS-sel és az Angular `animate.enter` / `animate.leave`
  szintaxisával. Az `@angular/animations` csomag nem használható (projektszabály). A
  PrimeNG v21 óta natív CSS-animációkat használ, és nem igényel
  `BrowserAnimationsModule`-t.
- A `0001-peer-review-forms`-ban elfogadott **viselkedés, API-szerződés és adatmodell
  nem változhat**. Az összesítők csak a már most is elérhető API-adatokból dolgoznak.
- A meglévő tesztek nem gyengülhetnek.

## Nice-to-have — döntés (2026-10-27)

**Bekerül a 0004-be.** Mindegyik a PrimeNG-gel megoldható, plusz függőség nélkül.

- **Jelvény a menüben:** a kollégánál a kitöltendő értékelések száma, az adminnál a
  piszkozatoké.
- **Felugró visszajelzés (toast)** minden mutáló műveletnél: mentés, kiküldés, törlés,
  beküldés.
- **Animált megerősítő párbeszédablak** törlésnél a natív `confirm()` helyett.
- **Barátságosabb üres állapotok** ikonnal.
- **Mindig látható Beküldés gomb** a kitöltő oldalon, mobilon.

**Később, külön változásban.** Ezek új funkciók, nem megjelenés:

- „Mentve X perce” jelzés a szerkesztőben (ehhez automatikus mentés is kellene).
- Szűrés és keresés az űrlaplistán és a csapattag-listán.
- Idővonal nézet az űrlap részleteinél (a címzettek táblázata időponttal ezt most
  lefedi).
- Nyomtatható vagy exportálható összegzés.
- Frissítés gomb a listákon.

**Elvetve:** a sötét mód automatikus követése a rendszerbeállítás szerint (döntés: csak
kapcsolóval vált).

## Nincs benne (out of scope)

- Valódi grafikonos vagy idősoros adatmegjelenítés (pl. sprint trend, burndown). Ez a
  `0003-jira-team-dashboard` témája, ahol tényleges idősoros adat is lesz.
- Az admin fiók, a bejelentkezési logika és a jogosultságkezelés funkcionális
  viselkedésének módosítása.
- Új oldal vagy funkció: ez a change kizárólag a meglévő oldalak megjelenését cseréli.
- Saját logó vagy arculati kézikönyv.

## Nyitott kérdések

Nincs. Eldöntve (2026-10-27):

1. **Betűtípus:** Manrope, az app saját csomagjából kiszolgálva, nem Google Fonts-ról.
2. **Kiemelőszín:** indigó, semleges szürke felületekkel.
3. **Kitöltöttség megjelenítése:** a PrimeNG folyamatjelzője, saját SVG-grafikon nélkül.
   A grafikonok a `0003`-ra várnak.
4. **PrimeNG verzió:** `^22.1.4` (Angular 22-vel kompatibilis, a spec rögzíti).
5. **Mobil töréspont:** 768 px. A kolléga mobilon nem kap alsó tab-bart, mert csak egy
   menüpontja van.
6. **„Felület” szakasz a spec-sablonban:** nem ennek a change-nek a része. A 0004 spec
   saját „Megjelenés” szakasza lesz a minta; a sablont egy külön, apró változás
   frissíti.
