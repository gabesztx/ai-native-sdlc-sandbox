# Plan: Modern Angular struktúra és minták a webes appban

- **ID:** 0005-angular-refactor
- **Státusz:** Approved
- **Spec:** [spec.md](spec.md)
- **Dátum:** 2026-10-27

## Megközelítés

A refaktor kis lépésekben halad, és minden lépés után zöld az `npm run check`.
1. **Szerkezet (1–3. lépés):** a generált kliens áthelyezése, a mappák átrendezése és a
   service-réteg kialakítása. Ezek a lépések csak mozgatnak és átkötnek, viselkedést nem
   változtatnak.
2. **Modern minták (4–6. lépés):** `rxResource`, Signal Forms, `@let`, `@defer`.
3. **Rögzítés (7–8. lépés):** lintszabályok és `apps/web/CLAUDE.md`. A lintszabályok
   azért jönnek a végén, mert addigra a kód már megfelel nekik.

API, szerződés és adatbázis nem változik. Az új Angular-elemek (service, komponens) az
Angular CLI-vel készülnek, a fájlmozgatás `git mv`-vel, hogy a spec-fájlok története
megmaradjon (AC2).

## Lépések

| # | Mit | Fájl(ok) | AC | Párhuzamosítható |
|---|---|---|---|---|
| 1 | **Generált kliens: service-ek, új hely.** A generátor beállítása: `output: "src/app/core/api"`, `services: true`, `promises: false`. A gitignore a régi `apps/web/src/app/api/` helyett az `apps/web/src/app/core/api/` mappát zárja ki, az ESLint `ignores` pedig a `src/app/core/api/**` mappát. Minden `api/…` import (modellek, `api-configuration`, és átmenetileg a `fn/`) átírása az új helyre: kód, spec és `src/testing/`. A spec-ekben csak az import sor változik. A régi `src/app/api/` mappa törlése a munkakönyvtárból. | `apps/web/ng-openapi-gen.json`, `.gitignore`, `apps/web/eslint.config.js`, `app.config.ts`, minden `api/`-t importáló `.ts` | AC5, AC17, AC2 | – |
| 2a | **Feature-mappák átrendezése.** Az oldal-komponensek `git mv`-vel a `features/<feature>/pages/<oldal>/` alá kerülnek, a `.spec.ts`-ükkel együtt: `login-page`, `form-list-page`, `form-detail-page`, `form-editor-page`, `review-list-page`, `review-fill-page`, `team-page`. Az importok átírása; a spec-ekben csak az import sor változik. | `src/app/features/**` | AC6, AC2 | – |
| 2b | **`shared/` rendbetétele.** A `shared/ui/*` mappa `git mv`-vel `shared/components/*` lesz. Az `initials.ts` a `shared/components/member-avatar/` mappába kerül (csak az használja), a `status-labels.ts` a `shared/components/status-tag/` mappába (csak az használja). Az importok átírása. | `src/app/shared/**`, az importáló fájlok | AC7 | – |
| 2c | **Feature route-fájlok.** Négy route-fájl készül: `auth.routes.ts` (`''` → login), `forms.routes.ts` (`''`, `new`, `:id`, `:id/edit`), `reviews.routes.ts` (`''`, `:id`) és `team.routes.ts` (`''`). Mindegyikben `loadComponent`. Az `app.routes.ts` a `login`, az `admin/forms`, az `admin/team` és a `reviews` útvonalon `loadChildren`-nel tölti be őket. A guardok, az URL-ek és az átirányítások nem változnak. | `features/*/<feature>.routes.ts`, `app.routes.ts` | AC6 | – |
| 3 | **Service-réteg a generált service-ek fölött.** Új szolgáltatások, a CLI-vel (`npx ng generate service features/<f>/services/<nev>`). Mindegyik a generált service-t injektálja, és a mostani metódusneveket és visszatérési típusokat adja. `fn/`, `rootUrl` és `.body` nem marad. <br> **features/forms:** `forms.service.ts` (űrlap CRUD, kiküldés, válaszok és csapattagok), `confirm-delete.service.ts` (a mostani `confirm-delete.ts` függvény, `ConfirmationService`-szel). <br> **features/reviews:** `reviews.service.ts`. <br> **features/team:** `team.service.ts`. <br> **features/auth:** `login.service.ts` (admin és kolléga belépés, belépési csapattag-lista). <br> **core/auth:** az `auth-api.service.ts` csak a `refresh` és a `logout` metódust tartja meg, `AuthControllerService`-szel. <br> **core/layout:** a `menu-badges.service.ts` a `FormControllerService`-t és a `ReviewControllerService`-t használja. <br> **Törlődik:** `core/team/`, `features/forms/forms-api.service.ts`, `features/reviews/reviews-api.service.ts`, `features/forms/confirm-delete.ts`. | `features/*/services/*`, `core/auth/auth-api.service.ts`, `core/layout/menu-badges.service.ts`, a hívó komponensek | AC3, AC4, AC6, AC8 | – |
| 4 | **`rxResource` és `numberAttribute`.** <br> **Teszt-segéd:** a `src/testing/http-test-setup.ts` `createPage` függvénye egy sort kap: `fixture.detectChanges()`. <br> **Listaoldalak** (űrlapok, értékeléseim, csapattagok): `rxResource`-ot használnak. A `loading` a `isLoading()`-ból, az üres vagy hibás állapot a `value()` és az `error()` alapján jön, ugyanazzal a DOM-mal és szöveggel. <br> **Részletek oldal:** **egyetlen** resource adja az űrlapot és a válaszokat. Ha az űrlap `SENT`, a stream `switchMap`-pel ugyanabban a láncban kéri le a válaszokat. Kiküldés után a mutáció ugyanígy lánccal kéri le a válaszokat, és `resource.set(...)`-tel írja be őket. Így a kérések sorrendje és száma azonos a maival (AC45-ös teszt). <br> **Szerkesztő:** a resource paramétere az opcionális `id` (ha `undefined`, nincs kérés). A nem `DRAFT` státuszú űrlapnál az átirányítás `tap`-ben történik, mert az side effect. A csapattag-lista is `rxResource`. <br> **Kitöltés:** `rxResource` az `id` alapján. <br> **Menüjelvények:** a `NavigationEnd` események számlálója `toSignal`, és ez a resource `params`-a, a szerepkörrel együtt. Így minden oldalváltáskor frissül, mint ma. <br> **Route `id`:** a részletek és a kitöltés oldalon `input.required({ transform: numberAttribute })`. A szerkesztőben opcionális input, olyan transzformmal, amely `undefined`-ot `undefined`-on hagy, mást `numberAttribute`-tal alakít. A komponensekből kikerül a `Number(...)`, a `toObservable` és az adatbetöltő `subscribe`. <br> **Marad `toSignal`:** a belépés csapattag-listája, a `shell` URL-signalja és a `breakpoint`. | `src/testing/http-test-setup.ts`, `features/*/pages/**/*.component.ts` és `.html`, `core/layout/menu-badges.service.ts` | AC11, AC12, AC13, AC2 | – |
| 5a | **Signal Forms: belépés.** A két űrlap `form()`-ra, `[formField]`-re és `form[formRoot]`-ra áll át, `required` sémával. A beküldés a `(submit)` eseményre fut; érvénytelen űrlapnál minden mező touched lesz. A kódon már kipróbáltam. | `features/auth/pages/login-page/*` | AC9, AC10 | 5b, 5c |
| 5b | **Signal Forms: szerkesztő.** <br> **Modell:** signal `{ topic, evaluatedMemberId, questions: QuestionDraft[] }`. Minden kérdés kap egy csak kliens oldali `key` mezőt; ez a `@for` `track` kulcsa, hogy törléskor a `animate.leave` a jó kártyán fusson. A `key` nem kerül a kérés törzsébe. <br> **Séma:** `required` és `maxLength(200)` a témán, `required` az értékelten, `applyEach` a kérdéseken (`required`, `maxLength(500)` a szövegen). <br> **Kérdés hozzáadása és törlése:** a modell signal frissítésével. <br> **Betöltés szerkesztéskor:** a resource értékéből tölti a modellt. <br> **Hibák:** a szerver mezőnkénti hibái a mostani `serverErrors` signalban maradnak, ugyanazzal a DOM-mal. Az `isInvalid` a mező `invalid()` és `touched()` állapotából, plusz a szerverhibából számol. | `features/forms/pages/form-editor-page/*` | AC9, AC10 | 5a, 5c |
| 5c | **Signal Forms: kitöltés.** A modell a válaszok tömbje, a kérdések sorrendjében. A kötelező kérdésekre kliensoldali `required` szabály kerül, a szövegre `maxLength(2000)`. A `SelectButton`, az `InputNumber` és a `Textarea` `[formField]`-del köt. Érvénytelen beküldésnél nem megy kérés, és a hibajelzés a mostani helyen és szöveggel jelenik meg. | `features/reviews/pages/review-fill-page/*` | AC9, AC10 | 5a, 5b |
| 6 | **`@let` és `@defer`.** A szerkesztőben a kérdésenkénti `error('questions[' + i + '].…')` hívások `@let`-be kerülnek. Ugyanígy minden más helyen, ahol egy blokk ugyanazt a hívást többször olvassa (részletek, kitöltés, lista). A részletek oldal *Beküldött válaszok* harmonikája `@defer (on immediate)` blokkba kerül, `@placeholder`-rel. | `features/*/pages/**/*.html` | AC15, AC16 | – |
| 7 | **Lintszabályok.** <br> **Közös `no-restricted-imports` alap:** a meglévő tiltások, plusz: <br> · `**/api/fn/**` pattern; <br> · `@angular/forms`: `ReactiveFormsModule`, `FormGroup`, `FormControl`, `FormArray`, `FormBuilder`; <br> · `@angular/core`: `HostBinding`, `HostListener`. <br> **`**/*.component.ts` kiegészítés** (ugyanarra az alapra): `@angular/common/http`: `HttpClient`, és pattern `**/core/api/services*`. <br> **Feature-ök közötti import:** feature-enként egy konfig, amely `regex` patternnel tiltja a többi feature-mappába vezető relatív importot. <br> **`no-restricted-syntax`:** a `@HostBinding` és `@HostListener` dekorátor tiltása (a meglévő `Input`/`Output` mintára). <br> Flat configban a későbbi blokk felülírja a szabály teljes opcióját, ezért az alap-tömböt mindenhol újra meg kell adni. | `apps/web/eslint.config.js` | AC3, AC4, AC8, AC9, AC14 | – |
| 8 | **`apps/web/CLAUDE.md` frissítése.** <br> · **Mappastruktúra:** az új fa. <br> · **API-kliens:** generált service-ek csak szolgáltatásban, `fn/` tilos. <br> · **Signals:** `rxResource` minta, egy resource az összefüggő adatoknak, `numberAttribute`. <br> · **Űrlapok:** Signal Forms; a Reactive Forms szabály kikerül, minta: `form()`, `[formField]`, `formRoot`, `applyEach`. <br> · **Komponensek:** `host: {}`, `@let`, `@defer (on immediate)`. <br> · **Lint és anti-patterns:** frissítve. <br> A többi fejezet változatlan. | `apps/web/CLAUDE.md` | AC18 | 7-tel |
| 9 | **Végellenőrzés.** Teljes `npm run check`. Spec-diff ellenőrzés (`git diff -M main -- '*.spec.ts' src/testing`). Kódkeresések az AC3–AC17-hez. Kézi végigkattintás `dev:web` + `dev:api` mellett. | – | AC1, AC2, AC19 | – |

## Tesztek

Új teszt nem készül, meglévőt nem törlünk (intent, spec). Az ellenőrzés a meglévő 40
tesztre, a lintre, kódkeresésre és kézi ellenőrzésre épül.

| AC | Ellenőrzés | Hol |
|---|---|---|
| AC1 | `npm run check` zöld, a web 40/40 teszt | teljes suite |
| AC2 | `git diff -M main` a `*.spec.ts` és a `src/testing/` fájlokon. Csak `import` sorok változnak, plusz a `createPage`-ben egy `fixture.detectChanges()` sor. | verifier |
| AC3 | lint (`**/api/fn/**`) és keresés: `fn/`, `RequestBuilder`, `StrictHttpResponse`, `rootUrl` → 0 találat a `src/app/core/api/` mappán kívül | `eslint.config.js`, verifier |
| AC4 | lint: komponensben tiltott a `HttpClient` és a `core/api/services` import | `eslint.config.js` |
| AC5 | `npm run api:client` → létrejön a `src/app/core/api/services/`, a `src/app/api/` nem | verifier |
| AC6 | keresés: a 4 feature mindegyikében van `pages/`, `services/` és `<feature>.routes.ts`, a gyökérben más fájl nincs | verifier |
| AC7 | keresés: a `shared/` és a `core/` gyökerében nincs fájl, és nincs `shared/ui/` | verifier |
| AC8 | lint: feature-ök közötti import tiltva | `eslint.config.js` |
| AC9 | lint (`@angular/forms` tiltott importok) és keresés: `[formField]` mind a 4 űrlapon | `eslint.config.js`, verifier |
| AC10 | meglévő tesztek változatlanul zöldek: AC43/AC50 (login), AC45 + szerverhiba (szerkesztő), AC44 + kötelező válasz + szerverhiba (kitöltés) | `login-page.component.spec.ts`, `form-editor-page.component.spec.ts`, `review-fill-page.component.spec.ts` |
| AC11 | keresés: `rxResource` a részletek, szerkesztő és kitöltés oldalon; nincs `toObservable` és adatbetöltő `subscribe` | verifier; a viselkedést az AC45 és AC44 tesztek fedik |
| AC12 | keresés: `rxResource` a 3 listaoldalon és a `menu-badges.service`-ben | verifier; a viselkedést a lista- és AC53 tesztek fedik |
| AC13 | keresés: `numberAttribute` az id-s oldalakon, nincs `Number(` az id-re | verifier |
| AC14 | lint (`no-restricted-syntax` és importtiltás) | `eslint.config.js` |
| AC15 | keresés: `@defer (on immediate)` a részletek oldalon; a build kimenetében külön lazy chunk; AC45 teszt zöld | verifier, `form-detail-page.component.spec.ts` |
| AC16 | review: nincs ismételt azonos hívás egy blokkon belül | reviewer |
| AC17 | keresés: `eslint.config.js` `ignores` = `src/app/core/api/**` | verifier |
| AC18 | review: a CLAUDE.md fejezetei | reviewer |
| AC19 | kézi: admin és kolléga fő folyamatai a futó appban | kézi |

## Ellenőrzés

- `npm run check` zöld minden lépés után; a webes tesztek száma mindig 40.
- A spec-fájlokban lépésenként csak import sor változhat. Ha egy lépés ennél többet
  kívánna, a lépés megáll, és jelzem.
- Kézi (AC19), a futó appban:
  - **Admin:** belépés → új űrlap → szerkesztés → kiküldés → válaszok megtekintése →
    piszkozat törlése → menüjelvény → kilépés.
  - **Kolléga:** belépés → kitöltés kötelező hibával, majd beküldés → menüjelvény →
    kilépés.
  - Mobil szélességen is végig kell nézni.

## Kockázatok

- **`rxResource` időzítése a tesztekben.** A kérés csak change detection után megy ki.
  → Ezt a `createPage` egy sora oldja meg (kipróbálva, 40/40). A részletek oldalon pedig
  egyetlen összevont resource és láncolt mutáció tartja a mostani kérés-sorrendet. Ha egy
  teszt mégis elbukik időzítés miatt, nem a tesztet írom át, hanem jelzem.
- **Signal Forms és dinamikus kérdéslista.** A kérdés törlésekor a `track` rossz elemet
  animálhat ki. → Stabil, csak kliens oldali `key` lesz a `track` kulcsa.
- **Signal Forms beküldés-szemantikája.** Kérdés, hogy érvénytelen beküldésnél minden
  mező touched lesz-e. → A kitöltés oldal „kötelező válasz hiányában…” tesztje ezt
  ellenőrzi. Ha a gyökér `markAsTouched` nem terjed le, a `submit()` függvényt használom.
- **ESLint flat config felülírás.** Az átfedő blokkok `no-restricted-imports` opciója
  nem fésülődik össze. → Közös alap-tömb, mindenhol újra megadva; lépés után próba egy
  szándékosan tiltott importtal, ami nem kerül commitba.
- **Feature-ök közötti regex tiltás hamis találata** (pl. `core/auth` vs. `features/auth`).
  → A regex a relatív út `../` szakaszai utáni első mappanevet nézi. Lépés után próba
  mindkét irányban.
- **`@defer` lazy chunk.** Ha a PrimeNG Accordion máshol is be van húzva, nem lesz külön
  chunk. → A build kimenetét a verifier nézi. Ha nincs külön chunk, jelzem; a spec
  szerint a cél a kódvágás.
- **`git mv` és spec-történet.** Ha egy lépés a mozgatással együtt túl sokat változtat egy
  fájlon, a git nem ismeri fel a mozgatást. → A mozgatás (2a, 2b) külön commit, a tartalmi
  változás később jön.

## Párhuzamosíthatóság

- Az 1–4. lépés sorrendben megy: közös fájlokat érintenek (importok, `app.routes.ts`,
  teszt-segéd).
- Az 5a, 5b és 5c független (három külön oldal). A méret kicsi, ezért egy agent, sorban
  végzi őket.
- A 7. és a 8. lépés független egymástól.

## Nincs benne

- Új teszt, tesztállítás módosítása, teszt törlése.
- NgRx, központi hibakezelő interceptor, új függőség.
- Fájlnév-konvenció váltása, vizuális változás, API- vagy szerződésváltozás.
- A kézzel írt `ProblemDetail` típus javítása.
- Üres `shared/pipes/` mappa. Pipe-ra most nincs szükség; a `status-labels` és az
  `initials` a használó komponens mappájába kerül. A mappa akkor jön létre, amikor az
  első pipe megszületik.
