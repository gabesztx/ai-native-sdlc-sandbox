# Plan: Modern, reszponzív UI design

- **ID:** 0004-ui-design
- **Státusz:** Draft
- **Spec:** [spec.md](spec.md)
- **Dátum:** 2026-10-27

## Megközelítés

Csak az `apps/web` változik: API, szerződés és adatbázis nem.

1. Először egy **alapréteget** rakunk le:
   - PrimeNG saját indigó presettel;
   - témaszolgáltatás;
   - globális stílusok és animációk;
   - toast és megerősítő ablak az app szintjén.
2. Utána jönnek a **közös UI-elemek** (`shared/ui/`) és az új **navigációs keret**.
3. Végül az oldalak **feature-mappánként** átállnak. Ezek egymástól függetlenek, így
   párhuzamosan is mehetnek.

**Tesztek:** a felhasználó döntése szerint (2026-10-27) a UI/design munkához **nem
készül új teszt**. Az elfogadási kritériumokat kézi, böngészős ellenőrzéssel nézzük
végig.

A meglévő 0001-tesztek megmaradnak, és zöldnek kell maradniuk. Ahol a DOM változik (pl.
natív `select` → PrimeNG `p-select`, rádiógomb → gombsor, `window.confirm` → saját
ablak), ott **csak a szelektort és a kezelés módját** igazítjuk. Egyetlen állítás sem
gyengül, és egyetlen teszt sem törlődik.

## Lépések

Az összes új komponens és szolgáltatás Angular CLI-vel készül (`ng generate`), az
`apps/web/CLAUDE.md` szerint. Minden lépés után `npm run check`, utána commit.

| # | Mit | Fájl(ok) | AC | Párhuzamosítható |
|---|---|---|---|---|
| 1 | **Függőségek és alapréteg** — lásd részletesen lent | `apps/web/package.json`, `package-lock.json`, `angular.json`, `src/styles.scss`, `src/app/app.config.ts`, `src/app/app.component.{ts,html}`, `src/app/core/theme/hugo-preset.ts`, `src/app/core/theme/theme.service.ts`, `src/app/shared/animations/_animations.scss`, `src/testing/ui-providers.ts` | AC3–7, AC47–48 | – |
| 2 | **Közös UI-elemek** — lásd részletesen lent | `src/app/shared/ui/{theme-toggle,page-header,stat-tile,status-tag,empty-state,member-avatar}/`, `src/app/shared/initials.ts` | AC5, AC7, AC12, AC17, AC49 | – |
| 3 | **Navigációs keret** — lásd részletesen lent | `src/app/core/layout/breakpoint.service.ts`, `src/app/core/layout/menu-badges.service.ts`, `src/app/core/layout/shell/*`, `shell.component.spec.ts` (igazítás) | AC9–16, AC35, AC38–40 | – |
| 4 | **Belépés oldal** | `features/auth/login-page/*`, spec igazítás | AC7, AC45 | 5–7-tel, 8-cal, 9–10-zel |
| 5 | **Űrlaplista** | `features/forms/form-list-page/*`, spec igazítás | AC17–20, AC33–34, AC49 | 4-gyel, 8-cal, 9–10-zel (6–7 ugyanabban a mappában, sorban) |
| 6 | **Űrlapszerkesztő** | `features/forms/form-editor-page/*`, spec igazítás | AC24–25, AC29, AC41, AC45–46 | mint 5 |
| 7 | **Űrlap részletei** | `features/forms/form-detail-page/*`, spec igazítás | AC21–22, AC26–28, AC42, AC45 | mint 5 |
| 8 | **Csapattagok** | `features/team/team-page/*`, spec igazítás | AC33, AC49 | 4-gyel, 5–7-tel, 9–10-zel |
| 9 | **Értékeléseim** | `features/reviews/review-list-page/*`, spec igazítás | AC23, AC33, AC49 | 4-gyel, 5–7-tel, 8-cal |
| 10 | **Kitöltés** | `features/reviews/review-fill-page/*`, spec igazítás | AC30–32, AC43–45, AC50 | mint 9 |
| 11 | **Záró átnézés** — lásd részletesen lent | – (csak ellenőrzés; javítás az érintett oldalon) | AC1–2, AC8, AC14–15, AC36–37, AC47–48, AC50 | – |

### 1. Függőségek és alapréteg

- **Telepítés** az `apps/web` workspace-be (jóváhagyva a spec-ben):
  `npm install -w apps/web primeng@^22.1.4 @primeuix/themes@^3.1.0 @angular/cdk@^22.2.0 primeicons@^8.0.2 @fontsource-variable/manrope@^5.3.0`
- **`core/theme/hugo-preset.ts`:** `definePreset(Aura, …)`.
  - A `primary` paletta az indigó skálára mutat (`{indigo.50}` … `{indigo.950}`).
  - A `surface` világos és sötét módban a semleges `zinc` skála.
  - Fókuszgyűrű kiemelőszínnel.
- **`app.config.ts`:**
  - `providePrimeNG({ theme: { preset: HugoPreset, options: { darkModeSelector: '.app-dark', cssLayer: … } } })`. Az explicit `darkModeSelector` miatt a rendszer sötét módja **nem** hat a témára (AC4).
  - `MessageService` és `ConfirmationService` provider.
  - `withViewTransitions()` a routerhez, az oldalváltás animációjához.
  - `provideAppInitializer`: a `ThemeService` még az első renderelés előtt beállítja a
    mentett témát, így nincs villanás.
- **`core/theme/theme.service.ts`:**
  - `theme` signal (`'light' | 'dark'`), alapértéke `light`;
  - `toggle()`;
  - az `app-dark` osztályt a `document.documentElement`-re teszi, illetve leveszi róla;
  - `localStorage['hugo-theme']` olvasás/írás `try/catch`-ben. Ha a tár nem elérhető,
    világos marad.
- **`src/styles.scss`:**
  - CSS layer sorrend: reset, primeng, app;
  - `@fontsource-variable/manrope` és `primeicons` importja;
  - `font-family: 'Manrope Variable', system-ui, sans-serif`;
  - `body` háttér és szöveg a PrimeNG design tokenekből (`--p-surface-*`,
    `--p-text-color`);
  - `::view-transition-old/new(root)` áttűnés (AC47);
  - globális `@media (prefers-reduced-motion: reduce)`: animációk és view transition
    kikapcsolva (AC48);
  - az `_animations.scss` betöltése.
- **`shared/animations/_animations.scss`:** közös `@keyframes`:
  - `fade-in-up`;
  - `collapse-out`;
  - lépcsőzetes beúszás (`stagger-in`) az első 10 elemre `:nth-child` + `animation-delay`
    alapon;
  - 150–300 ms-os időzítések SCSS-változóként.
- **`app.component.html`:** `<p-toast>` (asztalon jobb felül, a `breakpoints` inputtal
  mobilon felül, teljes szélességben), `<p-confirmdialog>` és `<router-outlet>`.
- **`src/testing/ui-providers.ts`:** a tesztekhez közös provider-lista
  (`providePrimeNG`, `MessageService`, `ConfirmationService`). Ez nem új teszt, hanem a
  meglévő tesztek beállításához kell.

### 2. Közös UI-elemek

Mind `ng generate component`, OnPush, `input()`, külön `.html` és `.scss` fájl.

| Elem | Mit csinál |
|---|---|
| `theme-toggle` | `p-togglebutton` vagy ikonos `p-button` nap/hold ikonnal, `aria-label` („Sötét mód” / „Világos mód”), a `ThemeService`-t hívja |
| `page-header` | cím, opcionális alcím, és projektált tartalom-slot jobbra a fő gombnak |
| `stat-tile` | címke + nagy szám vagy „x / y” szöveg, `p-card` alapon |
| `status-tag` | `p-tag`: státusz → felirat (`status-labels.ts`) és súlyosság (secondary / info / warn / success) |
| `empty-state` | PrimeIcons ikon + szöveg + projektált művelet |
| `member-avatar` | `p-avatar` monogrammal |

A `shared/initials.ts` egy tiszta függvény, amely a név első két szókezdő betűjéből
képzi a monogramot.

### 3. Navigációs keret

- **`core/layout/breakpoint.service.ts`:** a CDK `BreakpointObserver`-ből `isMobile`
  signal (`(max-width: 768px)`). jsdom alatt (nincs `matchMedia`) asztali nézetet ad,
  így a meglévő tesztek változatlanul az asztali DOM-ot látják.
- **`core/layout/menu-badges.service.ts`:**
  - belépéskor és minden `NavigationEnd`-kor frissíti a számot;
  - admin: a generált `listForms` hívásból a `DRAFT`-ok száma;
  - kolléga: a saját kitöltések közül a `PENDING`-ek száma;
  - hiba esetén `null`, így nincs jelvény (AC40).

  A `core` nem importálhat `features`-ből, ezért közvetlenül a generált kliens
  függvényeit használja.
- **`shell` átírása:**
  - asztali nézetben `aside` oldalsáv:
    - az app neve;
    - `nav[aria-label="Fő navigáció"]` a menüpontokkal: PrimeIcons ikon, felirat,
      `p-badge`;
    - lent `member-avatar`, név, `theme-toggle` és a Kilépés `p-button`.
  - mobilon (`@if (isMobile())`):
    - felső fejléc: app neve, a kolléga jelvénye, `theme-toggle`, csak ikonos Kilépés
      `aria-label`-lel;
    - alsó menüsáv csak akkor, ha `menu().length > 1`.
  - A menüpont kijelölésének mostani logikája (`activePath`) marad, és kiegészül egy
    `aria-current="page"` attribútummal.
  - A menüdefiníció ikonnal bővül (`pi-file`, `pi-plus`, `pi-users`, `pi-inbox`).
- **`shell.component.spec.ts` igazítása:**
  - szelektorok;
  - a jelvény-kérések kiszolgálása (`HttpTestingController.expectOne(...)`), hogy a
    `verify()` ne bukjon el.

### 4–10. Oldalak — közös szabályok

- Az oldal teteje `page-header`.
- A betöltés előtti állapotot egy `loading` signal jelzi: a `toSignal` `undefined`
  kezdőértékkel indul, és amíg `undefined`, `p-skeleton` látszik az üres üzenet helyett
  (AC33).
- A mutáló művelet gombja `p-button [loading]="saving()"`. A `saving` signal `finalize`
  operátorral áll vissza; a `loading` alatt a gomb le van tiltva (AC45).
- Sikeres mutáció után `MessageService.add({ severity: 'success', summary: '…' })`, utána
  a mostani navigáció (AC41–43). Hiba esetén nincs toast, a hiba ott marad, ahol most van
  (AC44).
- A hibák `p-message severity="error"` stílusban, ugyanazzal a szöveggel és ugyanott.
  Mezőszinten `p-invalid` osztály és `aria-invalid="true"` (AC25).
- Az oldal SCSS-e **csak elrendezés**: Flexbox, `gap`, szélesség és a lépcsőzetes
  animáció osztálya. Saját gomb-, mező- vagy táblázatstílus nincs (AC37).
- Az asztali és a mobil elrendezés közti váltás a `BreakpointService.isMobile()` alapján
  `@if`-fel történik, nem duplikált DOM-mal.

**Oldalankénti részletek:**

- **4. Belépés:**
  - két `p-card`: kolléga (`p-select` a nevekkel) és admin (`pInputText` e-mail +
    jelszó);
  - `theme-toggle` a sarokban;
  - mobilon a kártyák egymás alá kerülnek (`flex-wrap`).
- **5. Űrlaplista:**
  - 4 `stat-tile`, `computed()`-dal a betöltött listából (AC18);
  - asztali nézetben `p-table`, mobilon kártyák;
  - a sorra kattintva `router.navigate`; a sor `tabindex="0"`, és Enterre is megnyílik
    (AC20);
  - `status-tag` és `p-progressbar` + „x / y” (AC19);
  - `empty-state` + Új űrlap gomb (AC34, AC49).
- **6. Szerkesztő:**
  - kérdéskártyák `p-card`-dal, `animate.enter` / `animate.leave` animációval (AC46);
  - típus `p-tag`, `p-toggleswitch` a Kötelező mezőhöz, `p-inputnumber` a skálahatárokhoz;
  - csak ikonos Kérdés törlése gomb `aria-label`-lel;
  - `p-select` az értékelt személyhez;
  - a törlés a `ConfirmationService`-szel (AC29);
  - toast mentéskor.
- **7. Részletek:**
  - `page-header` + `status-tag`;
  - gombok: Szerkesztés, Kiküldés (elsődleges), Törlés (`severity="danger"`);
  - `p-progressbar` „x / y beküldve” felirattal;
  - címzettek `p-table` + `status-tag` (AC21);
  - válaszok `p-accordion`-ban, címzettenként egy panel (AC22);
  - törlés megerősítéssel (AC26–28); toast kiküldéskor és törléskor (AC42).
- **8. Csapattagok:** lista `member-avatar` + név elemekkel, skeleton, `empty-state`.
- **9. Értékeléseim:** 2 `stat-tile` (AC23), kártyalista `status-tag`-gel, Kitöltés
  gomb, skeleton, `empty-state`.
- **10. Kitöltés:**
  - kérdéskártyák;
  - skála: ha az értékek száma (`max − min + 1`) legfeljebb 11, `p-selectbutton`,
    különben `p-inputnumber` a tartomány feliratával (AC30–31);
  - `textarea pTextarea`;
  - mobilon `position: sticky; bottom: 0` Beküldés sáv, a tartalom alján kellő
    `padding`-gel (AC50);
  - toast beküldéskor.

**Tesztek igazítása (4–10):**

- `#member-select option` és `#evaluated` → a `p-select` megnyitása, majd a
  `[role="option"]` elemre kattintás;
- `.scale-option input[type=radio]` → a `p-selectbutton` gombjára kattintás;
- `vi.spyOn(window, 'confirm')` → a megerősítő ablak **Törlés** gombjára kattintás;
- osztálynév-szelektorok az új szerkezetre.

Az elvárt értékek (szövegek, darabszámok, API-kérések) változatlanok maradnak.

### 11. Záró átnézés

- `npm run check` zöld.
- `git diff main -- contracts apps/api` üres (AC2).
- Grep a 7 oldal SCSS-ében: `display: grid`, `color:`, `background`, `border` saját
  elemekre (AC37).
- **Kézi ellenőrzés böngészőben** (Superset böngésző pane, képernyőképekkel):
  - 1280 px és 375 px szélesség × világos és sötét mód × mind a 7 oldal (AC8, AC14–15);
  - billentyűzetes bejárás (AC36);
  - animációk (AC47);
  - mozgáscsökkentés emulálva (AC48);
  - hosszú kitöltés mobilon (AC50).

## Tesztek

**Új teszt nem készül** (felhasználói döntés, 2026-10-27: „UI és designhez NE csinálj
teszteket”). Az ellenőrzés módja kritériumonként:

| AC | Ellenőrzés |
|---|---|
| AC1 | `npm run check` — a meglévő 0001 web- és API-tesztek zöldek; a tesztfájlokban csak szelektor- és kezelésmód-igazítás, az elvárt értékek diffje üres |
| AC2 | `git diff main -- contracts apps/api` üres |
| AC3–7 | kézi: friss böngészőprofil, OS sötét mód, kapcsoló, újratöltés, kilépés/belépés, belépés oldal |
| AC8, AC14–15, AC36, AC47–48, AC50 | kézi böngészős átnézés (11. lépés) |
| AC9–13, AC16 | meglévő `shell.component.spec.ts` (menüpontok, kijelölés, név, kilépés, igazítva) + kézi |
| AC17–23, AC30–32, AC38–46, AC49 | kézi, admin és kolléga szereppel, a dev adatbázis tesztadataival |
| AC24–29 | meglévő `form-editor-page` / `form-detail-page` specek (igazítva) + kézi |
| AC33–35 | kézi (lassított hálózat a DevTools-ban a skeletonhoz; `aria-label` a DOM-ban) |
| AC37 | 11. lépés grep + kódátnézés |

## Ellenőrzés

- `npm run check` zöld minden lépés után; a 11. lépés után a verifier subagent is
  lefuttatja.
- A kézi ellenőrzést a 11. lépés listája szerint végezzük. Az eredményt (képernyőképek,
  talált hibák) a build végén jelentem.
- A **spec** elfogadási kritériumai közti mondat („Minden más kritériumhoz automatikus
  teszt tartozik”) ellentmond az új döntésnek. Ezt a spec-ben át kell írni. Ez a plan
  nem módosítja a spec-et; a felhasználó dönt róla.

## Kockázatok

- **Bundle-méret:** a PrimeNG + Aura preset + betű növeli a kezdő bundle-t. A mostani
  budget 500 kB-nál figyelmeztet, 1 MB-nál hibát ad. → A komponenseket egyenként
  importáljuk, és a route-ok lazy loadingja marad. Ha a figyelmeztetési határ mégis
  átlépődik, **nem emelem csendben**: jelzem, és a felhasználó dönt.
- **`anyComponentStyle` 4 kB-os budget:** az oldalak SCSS-e kicsi marad, mert csak
  elrendezést tartalmaz; a közös animációk globálisan vannak.
- **PrimeNG vezérlők jsdom-ban:** a `p-select` overlay vagy a `p-selectbutton` másképp
  reagálhat a szintetikus eseményekre. → Ha egy meglévő teszt csak gyengítéssel
  igazítható, megállok és jelzem; nem írok át állítást.
- **Shell jelvény-kérések a meglévő tesztekben:** új HTTP-hívások jelennek meg (és az
  `app.routes.spec.ts`-ben is). → A tesztekben kiszolgáljuk őket; az elvárások nem
  változnak.
- **PrimeNG 22 API-eltérés** (komponens- vagy inputnevek): a lépés elején a telepített
  verzió típusaiból ellenőrizzük, nem emlékezetből.
- **Sötét mód „beragadt” felületek** (saját SCSS-ben fix színek). → Csak design tokent
  (`var(--p-…)`) használunk, fix színkódot nem; a 11. lépés grepje ezt is nézi.
- **Új tesztek hiánya:** a jelvényszámolás, a téma-perzisztencia és a skála-gombsor
  logikájára nem lesz automatikus védőháló. Egy későbbi változtatás csendben
  elronthatja. Ezt a felhasználói döntés tudatosan elfogadja.

## Nincs benne

- Új tesztfájl vagy új teszteset.
- Bármilyen API-, szerződés- vagy adatbázis-módosítás.
- A spec „Nincs benne” listája (szűrés, keresés, idővonal, export stb.).
- Budget-emelés jóváhagyás nélkül.
- A spec-sablon „Felület” szakasza (külön változás).
