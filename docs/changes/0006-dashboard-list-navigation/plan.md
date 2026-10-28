# Plan: Dashboard lista child route navigációval

- **ID:** 0006-dashboard-list-navigation
- **Státusz:** Approved
- **Spec:** [spec.md](spec.md)
- **Dátum:** 2026-10-28

## Megközelítés

A `dashboard.routes.ts`-ben a jelenlegi egyetlen `''` route gyermek-route-ot kap
(`:tileSegment`): a szülő (`DashboardPageComponent`) marad a főnézet (KPI-kártyák +
sprint progress card), a gyermek egy új `DashboardListPageComponent`, amely a szűrt
listát jeleníti meg. A kettő kölcsönös kizárását a szülő egy `hasChildRoute` signalja
(route-eseményből számolva) és egy `@if` blokk adja, natív `animate.enter`/`leave`
animációval; a gyermek komponens saját host-kötésű `animate.enter`/`leave`-jel animál
belépéskor/kilépéskor. A tile↔URL-szegmens és a tile-címke leképezés a
`dashboard.service.ts`-be kerül, hogy mindkét komponens ugyanazt a forrást használja.

## Lépések

| # | Mit | Fájl(ok) | AC | Párhuzamosítható |
|---|---|---|---|---|
| 1 | `Tile`↔URL-szegmens leképezés (`TILE_SEGMENTS`, `tileToSegment`, `segmentToTile`) és `TILE_LABELS` hozzáadása; `issueSeverity` ide költöztetése a `dashboard-page.component.ts`-ből | `apps/web/src/app/features/dashboard/services/dashboard.service.ts` | AC1, AC3, AC5, AC8 | – |
| 2 | Új, újrahasználható belépő/kilépő animáció-osztály (`view-leave` + `fade-out-down` keyframe) a meglévő `fade-in-up` párjaként | `apps/web/src/app/shared/animations/_animations.scss` | AC9 | 1-gyel |
| 3 | `dashboard.routes.ts` átalakítása: `''` route gyermek-route-tal (`:tileSegment` → `DashboardListPageComponent`) | `apps/web/src/app/features/dashboard/dashboard.routes.ts` | AC1, AC3 | – |
| 4 | `DashboardPageComponent`-ből a `selectedTile`/`issuesResult`/issue-lista logika kivétele; `hasChildRoute` signal (route-gyermek jelenléte alapján); `selectTile` relatív navigációra vált (`router.navigate([tileToSegment(tile)], { relativeTo: route })`); főnézet `@if (!hasChildRoute())` blokkban `animate.enter="fade-in-up" animate.leave="view-leave"`; `<router-outlet />` mindig a template-ben | `apps/web/src/app/features/dashboard/pages/dashboard-page/dashboard-page.component.ts` + `.html` + `.scss` | AC1, AC2, AC9 | – |
| 5 | Új oldal generálása CLI-vel: `npx ng generate component features/dashboard/pages/dashboard-list-page` (`apps/web`-ben futtatva) | `apps/web/src/app/features/dashboard/pages/dashboard-list-page/*` | – | 4-gyel (más fájl) |
| 6 | `DashboardListPageComponent`: `tileSegment = input.required<string>()`; `tile = computed(() => segmentToTile(...))`; érvénytelen szegmensnél `router.navigate(['..'], { relativeTo: route })` (AC5); issue-lekérdezés `toSignal(toObservable(tile).pipe(switchMap(...)))` mintával (route-paraméteres oldal, nem `rxResource`); host-kötésű `animate.enter="fade-in-up" animate.leave="view-leave"` | `dashboard-list-page.component.ts` | AC1, AC3, AC5, AC6, AC9 | 7-tel |
| 7 | `DashboardListPageComponent` sablon: `page-header` a szűrő nevével (`TILE_LABELS`) és projektált "Vissza" gombbal (`router.navigate(['..'], { relativeTo: route })`, AC2); skeleton betöltéskor (AC6); `empty-state` üres listánál (AC7); kártyaszerű lista-elemek (kulcs+link, `p-tag` állapot az `issueSeverity`-vel, `member-avatar` + név vagy "nincs") (AC8); reszponzív flex-elrendezés mobil töréssel | `dashboard-list-page.component.html` + `.scss` | AC2, AC6, AC7, AC8 | 6-tal |
| 8 | `DashboardPageComponent` tesztek frissítése: a régi, beágyazott lista-flush teszteket (jelenlegi AC7/AC10/AC8-jelölésű, 0003-as ACk) áthelyezni/kiváltani `DashboardListPageComponent` tesztekkel; új teszt a navigációs hívásra (AC1) | `dashboard-page.component.spec.ts` | AC1 | – |
| 9 | `DashboardListPageComponent` tesztek: direkt `tileSegment` inputtal renderelt lista (AC3 proxy), vissza-navigáció hívás (AC2), érvénytelen szegmens redirect (AC5), skeleton betöltéskor (AC6), üres lista (AC7), kártya-tartalom kulccsal/összegzéssel/állapottal/hozzárendelttel és "nincs"-csel (AC8), belépő animáció-osztály jelenléte (AC9) | `dashboard-list-page.component.spec.ts` | AC2, AC3, AC5, AC6, AC7, AC8, AC9 | 8-cal |

## Tesztek

| AC | Teszt | Fájl |
|---|---|---|
| AC1 | KPI-csempére kattintva a router relatív navigációt hív a megfelelő szegmenssel | `dashboard-page.component.spec.ts` |
| AC2 | A lista aloldal "Vissza" gombja a szülő route-ra navigál | `dashboard-list-page.component.spec.ts` |
| AC3 | `tileSegment` inputtal közvetlenül renderelt oldal rögtön a szűrt listát mutatja (a natív direkt URL-megnyitást a `withComponentInputBinding()` route-kötés biztosítja — kézi ellenőrzés is kell, ld. lent) | `dashboard-list-page.component.spec.ts` |
| AC4 | Natív böngésző-history — nincs egyedi logika, ezért kézi ellenőrzés (ld. Ellenőrzés) | – |
| AC5 | Érvénytelen `tileSegment` esetén a komponens a szülő route-ra navigál | `dashboard-list-page.component.spec.ts` |
| AC6 | Folyamatban lévő lekérdezésnél skeleton-sorok jelennek meg | `dashboard-list-page.component.spec.ts` |
| AC7 | Üres szűrt eredménynél az `empty-state` jelenik meg | `dashboard-list-page.component.spec.ts` |
| AC8 | Egy issue kártyán kulcs, összegzés, állapot és hozzárendelt (vagy "nincs") látszik | `dashboard-list-page.component.spec.ts` |
| AC9 | A főnézet `.dashboard-main` wrappere jelen van (statikus ellenőrzés); a tényleges `animate.enter`/`animate.leave` átmenet kézi ellenőrzés mindkét irányban (jsdom-ban az animáció időzítése nem determinisztikus) | `dashboard-page.component.spec.ts` |

## Ellenőrzés

- `npm run check` zöld minden lépés után.
- Kézi ellenőrzés `npm run dev:web`-bel: KPI-kártyára kattintás → animált átmenet a
  listára, URL változik (`/dashboard/blocked` stb.); "Vissza" → animált visszatérés;
  böngésző vissza/előre gomb natívan lépked; érvénytelen URL (`/dashboard/xyz`)
  megnyitása a főnézetre irányít; mobil szélességen a kártyák egy oszlopban, a
  metaadatok a cím alatt törnek.

## Kockázatok

- A `hasChildRoute` signal rossz forrásból számolva (pl. csak `ActivatedRoute.children`
  statikus értékéből) elmaradhat a router-eseménytől → `router.events`
  (`NavigationEnd`) alapján frissítjük, kezdőértéke `route.snapshot.firstChild`.
- Az `animate.leave` router-outlet-tel való viselkedése nem azonos az `@if`-fel —
  host-kötéssel (`host: { 'animate.enter': ..., 'animate.leave': ... }`) oldjuk meg a
  `DashboardListPageComponent`-en; ha ez a verify lépésben vizuálisan nem animál,
  CSS-transition fallback-re váltunk `@starting-style`-lal.
- A meglévő `withViewTransitions()` router-konfiguráció ütközhet a CSS-animációval —
  ha kettős animáció látszik kézi ellenőrzéskor, csak a CSS-animációt tartjuk meg, és
  jelentjük a felhasználónak.

## Nincs benne

- API- vagy adatmodell-változás.
- A sprint progress card vagy a KPI-kártyák adatlekérdezési logikájának módosítása.
- Új animációs JavaScript-könyvtár.
- Az `app.routes.ts` vagy más feature módosítása.
