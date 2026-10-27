# Intent: Az Angular web app refaktorálása modern Angular struktúrára

- **ID:** 0005-angular-refactor
- **Státusz:** Approved
- **Dátum:** 2026-10-27

## Probléma

A 0001-ben elkészült `apps/web` kód működik, de egy Angularos fejlesztő számára idegen és
nehezen bővíthető. Ez a csapatnak fáj, és minden későbbi feature-nek (0002–0004) is,
mert ezek erre a kódra épülnek.

- **Nincs látható service-réteg.** Az API-hívó szolgáltatások szétszórva vannak
  (`core/auth`, `core/team`, a feature-mappák gyökere).
- **A generált kliens belső részletei átszivárognak a kézzel írt kódba.** A szolgáltatások
  a generált `fn/` függvényeket hívják `HttpClient`-tel, `rootUrl`-lel és `.body`
  kicsomagolással. Ez boilerplate, és nem Angularos minta.
- **Rossz helyen van a generált `api/` mappa.** Az `src/app/` gyökerében áll, a
  `core` / `features` / `shared` felosztáson kívül.
- **Elavult az adatlekérés a komponensekben.** Kézi `toObservable` → `switchMap` →
  `subscribe` → `signal.set` láncok és több `takeUntilDestroyed` szerepel bennük,
  betöltés- és hibaállapot nélkül.

A struktúrát az `apps/web/CLAUDE.md` írja elő. Ha csak a kódot javítjuk, a szabályt
nem, a következő feature ugyanígy készül el.

## Kívánt eredmény

- **Feature-first szerkezet.**
  - Minden feature egy helyen tartja az oldalait, a csak hozzá tartozó komponenseit és
    a szolgáltatásait, mindegyiket saját, jól látható helyen, és saját route-definíciója
    van.
  - Az app-szintű dolgok (auth, HTTP, layout, generált kliens) egy helyen vannak.
  - A több feature-ben használt elemek is egy helyen vannak.
- **Generált service-osztályok.** A generált kliens kész Angular szolgáltatásokat ad, így
  a kézzel írt kódban nem jelennek meg a generátor belső részletei (`fn/`, `rootUrl`,
  `.body`, `RequestBuilder`).
- **Signal-alapú reaktív erőforrások.** A komponensek az adatot ezeken keresztül kapják;
  a kézi feliratkozási láncok eltűnnek.
- **A legújabb Angular template- és űrlap-eszközök.** Az űrlapok Signal Formsra épülnek;
  a templatek `@let`-et és `@defer`-t, a komponensek a host-kötésekhez `host: {}`
  mezőt használnak, ahol ez indokolt.
- **Frissített `apps/web/CLAUDE.md`.** Az új struktúrát és mintákat írja le, hogy a
  következő feature már eszerint készüljön.

**Sikeres, ha:**
- egy Angularos fejlesztő megnyitja a kódot, és a mappaszerkezetből azonnal megtalálja
  egy feature oldalait, komponenseit és szolgáltatásait;
- a kézzel írt kódban nincs hivatkozás a generált kliens belső függvényeire;
- a 0001 viselkedése változatlan, és az `npm run check` zöld.

## Érintettek

- **Felhasználók:** a fejlesztők. A végfelhasználó (admin, kolléga) semmilyen változást
  nem lát.
- **Rendszerrészek:** csak a **web** (`apps/web`), plusz a generált kliens beállítása. Az
  API és az adatmodell nem változik.

## Korlátok

- **A felhasználó által meghozott döntések** (az ajánlati egyeztetés 1/A, 2/A és 3/A
  pontja):
  - **1/A:** feature-first struktúra, a feature-ön belül oldal-, komponens- és
    szolgáltatás-mappával;
  - **2/A:** marad a meglévő kliensgenerátor, de service-osztályokat és Observable-t
    generál;
  - **3/A:** a reaktív adatlekérés `rxResource`-szal történik.
- **Az ajánlaton túl ebbe a változásba kerül:** Signal Forms (a typed Reactive Forms
  helyett), `@let`, `@defer` és `host: {}`.
- Minden egyébben az `apps/web/CLAUDE.md` jelenlegi szabályai érvényesek: `loadComponent`
  + funkcionális guard, `.component` fájlnév-suffix, hibakezelés `catchError`-ral a
  pipe-ban.
- Új függőség nem kerülhet be.
- A felhasználó számára látható viselkedés nem változhat.
- **Tesztek:** a meglévő `*.spec.ts` fájlokban csak az importútvonalak változhatnak.
  - Egy spec a tesztelt fájljával együtt költözik.
  - Állítás, tesztnév és HTTP-elvárás (pl. `expectOne` URL) nem változik.
  - Tesztet nem lehet törölni, és új teszt sem jön.

## Nincs benne (out of scope)

- NgRx vagy más store-könyvtár.
- Központi hibakezelő interceptor.
- Fájlnév-konvenció váltása (marad a `.component` suffix).
- Bármilyen vizuális vagy UI-változás: az a 0004 dolga.
- API- vagy szerződésváltozás.
- **A kézzel írt `ProblemDetail` típus javítása.** Most marad, külön változás veszi fel az
  OpenAPI-szerződésbe.

## Eldöntött kérdések

- **Sorrend:** eredetileg az volt a döntés, hogy a 0005 megy előbb. Időközben a 0004 UI
  design elkészült, és bekerült a mainbe (PR #5). A 0005 ezért a 0004 utáni kódra épül,
  a PrimeNG-s felülettel együtt, és a 0004 `plan.md`-jét nem kell frissíteni.
- **Tesztfájlok:** csak az importútvonal változhat (lásd Korlátok).
- **`ProblemDetail`:** most marad, külön változásban javítjuk.

## Nyitott kérdések

- Nincs.
