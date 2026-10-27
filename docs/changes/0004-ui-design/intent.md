# Intent: Modern, reszponzív UI design a HUGO Feature Request Boardnak

- **ID:** 0004-ui-design
- **Státusz:** Approved
- **Dátum:** 2026-10-27

## Probléma

A 0001-ben elkészült felület (bejelentkezés, admin űrlapkezelés, kolléga értékelések)
funkcionálisan kész, de vizuálisan dísztelen: a globális `styles.scss` gyakorlatilag
üres, oldalanként csak néhány soros SCSS van, nincs egységes komponensrendszer, nincs
reszponzív viselkedés mobilra, és semmilyen animáció nincs. A felület eddig egyszer sem
volt tervezési téma — sem az intent, sem a spec, sem a plan nem foglalkozott a
megjelenéssel, csak a viselkedéssel.

## Kívánt eredmény

- A HUGO Feature Request Board minden meglévő oldala (bejelentkezés; admin: űrlaplista,
  szerkesztő, részletek, csapattagok; kolléga: értékeléseim, kitöltés) egységes, modern,
  "dashboard" jellegű megjelenést kap.
- A felület **mobilon és asztali nézetben is jól használható** (reszponzív elrendezés).
- A navigáció a jelenlegi egyetlen vízszintes fejléc helyett **bal oldali sidebar**
  (asztali nézet) és **alsó tab-bar** (mobil nézet) formát kap.
- A felületen **animációk** segítik a visszajelzést és az állapotváltások érzékelését
  (pl. lista-betöltés, állapotváltás, beküldés visszajelzése, oldalváltás) — kizárólag
  CSS-alapú eszközökkel, a projekt animációs szabálya szerint
  (`@keyframes`/`transition`/`@starting-style`, `animate.enter`/`animate.leave`), nem
  `@angular/animations`-szel.
- A felület **világos és sötét témát is** támogat; a felhasználó tud váltani köztük.
- A felület **modern, jól olvasható, egyedi tipográfiát** használ — nem a böngésző
  alapértelmezett rendszer-betűtípusát, és nem az általános "AI-generált" felületeken
  megszokott, elkoptatott betűtípust.
- A felület egy közös **komponensrendszerre** épül ahelyett, hogy minden oldal saját,
  egyedi HTML/SCSS elemeket írna újra.

**Sikeres, ha:** egy felhasználó (admin vagy kolléga) asztali és mobil böngészőben is
koherens, modern, reagáló felületet lát a meglévő összes oldalon, ugyanazokkal a
komponensekkel és átmenetekkel, funkcióvesztés nélkül a 0001-ben elkészült
viselkedéshez képest.

## Érintettek

- **Felhasználók:** a HUGO csapat minden tagja — admin és kolléga szerepkörben is.
- **Rendszerrészek:** kizárólag a **web** (`apps/web`). Az API-t és az adatmodellt nem
  érinti. Nem ad hozzá új funkciót, nem változtat a 0001-ben elfogadott viselkedésen —
  csak a meglévő oldalak megjelenését és a navigáció szerkezetét cseréli le.

## Korlátok

- A megjelenés **SCSS-alapú** marad (`apps/web/CLAUDE.md`: "Mindig SCSS — nem CSS, nem
  inline style"). Ez korábbi beszélgetésben eldőlt döntés: emiatt esett a választás a
  **PrimeNG** komponenskönyvtárra (Aura téma) a Tailwind-alapú shadcn-stílusú
  alternatívák (Spartan UI, Zard UI) helyett, mert azok a SCSS-szabállyal ütköznének.
- A PrimeNG **új függőség** — ezt a `/spec` vagy `/plan` lépésben explicit jóvá kell
  hagyni a `CLAUDE.md` határai szerint ("Új függőség hozzáadása előtt kérdezz"), ez az
  intent nem helyettesíti azt a jóváhagyást.
- Animáció csak natív CSS-sel és Angular `animate.enter`/`animate.leave`
  szintaxissal — `@angular/animations` csomag nem használható (projektszabály). A
  PrimeNG verziójának zoneless- és animációs-kompatibilisnek kell lennie (ellenőrizve:
  PrimeNG v21-től natív CSS-animációkra váltott, nem igényel
  `BrowserAnimationsModule`-t).
- A `0001-peer-review-forms`-ban elfogadott viselkedés, API-szerződés és adatmodell nem
  változhat.

## Nice-to-have (ha belefér)

Ezek nem feltételei a sikernek, de ha a `/spec`/`/plan` során belefér, érdemes
megvalósítani — mindegyikhez van kész PrimeNG komponens, nem igényelnek plusz
függőséget a PrimeNG-en felül:

- **Kitöltendő/piszkozat jelző badge a sidebarban** — kollégának hány értékelés vár rá,
  adminnak hány piszkozat van kiküldetlenül.
- **Toast visszajelzés minden mutáló műveletnél** (törlés, mentés, küldés), nemcsak a
  beküldésnél.
- **Animált megerősítő dialógus törlésnél** a natív `confirm()` helyett.
- **"Mentve X perce" jelzés** a form szerkesztőben, hogy látszódjon, nem veszett-e el a
  piszkozat.
- **Szűrés/keresés** az admin űrlaplistán (állapot szerint) és a csapattag-listán (név
  szerint).
- **Csinosított üres állapotok** ("Még nincs űrlap" helyett ikonos, barátságosabb
  üzenet).
- **Idővonal (Timeline) nézet** a form részleteknél — ki mikor küldte be, időrendben.
- **Nyomtatható/exportálható összegzés** egy kiküldött űrlap válaszairól (egyszerű
  print-CSS, PDF generálás nem kell hozzá).
- **Sticky beküldés-gomb** a kitöltő oldalon mobilon.
- **Frissítés gomb** a listákon.
- **Sötét mód automatikus követése** a rendszerbeállítás szerint induláskor, manuális
  felülbírálási lehetőséggel.

## Nincs benne (out of scope)

- Valódi grafikonos/idősoros adatmegjelenítés (pl. sprint trend, burndown) — az a
  `0003-jira-team-dashboard` témája, ahol tényleges idősoros adat is lesz.
- Az admin fiók, a bejelentkezési logika, a jogosultságkezelés funkcionális
  viselkedésének módosítása.
- Új oldal vagy funkció — ez a change kizárólag a meglévő oldalak megjelenését cseréli.

## Nyitott kérdések

- Melyik konkrét betűtípus(ok) legyenek — ezt a `/spec` lépésben kell kiválasztani
  (pl. Google Fonts-ról), figyelembe véve a PrimeNG Aura témával való összhangot.
- A `0001`-ben már elfogadott, **egyszerű állapot-számok** (pl. "4/6 beküldve") mellé
  kerüljön-e már ebben a change-ben egy saját, függőségmentes **SVG mini-grafikon**
  (pl. donut progress a form részleteknél), vagy ez is várjon a `0003`-ra?
- Pontosan mely PrimeNG verzió kerüljön rögzítésre (a `/plan` idején ellenőrizendő az
  Angular 22-vel való kompatibilitás)?
- A `.claude/skills/spec/template.md`-be felveendő egy önálló **"Felület" szakasz** — ez
  ennek a change-nek a része legyen (hogy a saját spec-je is ezt a formátumot kövesse),
  vagy külön, apró change?
