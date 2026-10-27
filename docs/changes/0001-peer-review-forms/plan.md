# Plan: Kollégák értékelése űrlapokkal

- **ID:** 0001-peer-review-forms
- **Státusz:** Draft
- **Spec:** [spec.md](spec.md)
- **Dátum:** 2026-10-27

## Megközelítés

Előbb a backend épül fel feature-önként (`user` → `auth` → `form` → `review`), mindegyik
a saját API-tesztjeivel, valódi SQLite-tal. Ha az API kész, újragenerálódik a
`contracts/openapi.json` és belőle az Angular kliens, majd a frontend következik: először
a közös mag (tokenkezelés, interceptor, guardok, navigáció), aztán az oldalak
feature-önként. A munkamenet véletlen tokenekkel megy, amelyeket egy servlet-filter
azonosít, a szerepkört pedig az útvonal-előtag (`/api/admin/**`, `/api/me/**`) dönti
el — így nincs szükség a teljes Spring Securityre és új függőségre.

## Lépések

Minden lépés után az `npm run check` zöld. Backend-útvonalak az
`apps/api/src/main/java/com/hugoboard/` alatt (röviden `…/`), frontend-útvonalak az
`apps/web/src/app/` alatt.

| # | Mit | Fájl(ok) | AC | Párhuzamosítható |
|---|---|---|---|---|
| 1 | **Adatbázis:** az első migráció mind a 7 táblával (`team_member`, `admin_account`, `auth_session`, `review_form`, `form_question`, `review_assignment`, `answer`), idegen kulcsokkal és egyedi megszorításokkal a spec szerint. **Közös rész:** `ProblemDetail`-alapú hibakezelő (`400` mezőnkénti listával), az üzleti kivételek (`NotFound`, `Conflict`, `Unprocessable`) és egy `Clock` bean, hogy a lejárat tesztelhető legyen. | `apps/api/src/main/resources/db/migration/V1__peer_review_forms.sql`, `…/common/ApiExceptionHandler.java`, `…/common/*Exception.java`, `…/common/ClockConfig.java` | alap (AC12, AC13 táblái) | – |
| 2 | **Csapattagok:** `TeamMemberRepository`, `TeamMemberService`; `GET /api/auth/team-members` (belépés nélkül). Fejlesztői tesztadat: `DevTeamSeeder`, csak a `dev` profilban, csak üres táblába (5 kitalált név). A `dev:api` szkript a `dev` profillal indul. | `…/user/TeamMember.java`, `…/user/TeamMemberRepository.java`, `…/user/TeamMemberService.java`, `…/user/TeamMemberController.java`, `…/user/DevTeamSeeder.java`, gyökér `package.json` (`dev:api`) | AC1 | – |
| 3 | **Belépés és tokenek:** `AdminBootstrap` (induláskor a `HUGO_ADMIN_EMAIL` / `HUGO_ADMIN_PASSWORD` alapján létrehozza vagy frissíti az admint, BCrypt-tel); admin- és kollégabelépés, `refresh` (rotációval), `logout`, `me`. Tokenek: 32 bájt `SecureRandom`, base64url, az adatbázisban SHA-256 hash; access 15 perc, refresh 8 óra. `AuthFilter` (Bearer → `CurrentUser` a kérésen) és `RoleInterceptor` (`/api/admin/**` = ADMIN, `/api/me/**` = MEMBER, különben `401`/`403` `ProblemDetail`-lel). Ugyanitt: `GET /api/admin/team-members`. | `…/auth/AuthController.java`, `…/auth/AuthService.java`, `…/auth/AuthSessionRepository.java`, `…/auth/AdminAccountRepository.java`, `…/auth/AdminBootstrap.java`, `…/auth/AuthFilter.java`, `…/auth/RoleInterceptor.java`, `…/auth/CurrentUser.java`, `…/auth/AuthWebConfig.java`, `…/auth/*` recordok (`AuthTokens`, `Me`, `AdminLoginRequest`, `MemberLoginRequest`, `RefreshRequest`), `…/user/TeamMemberController.java`, `application.yml` (`hugo.admin.*`) | AC2–AC16 | – |
| 4 | **Űrlapok (admin):** lista, létrehozás, lekérés, módosítás (csak `DRAFT`), törlés (csak `DRAFT`), kiküldés (címzettek = csapat − értékelt; `422`, ha nincs kérdés vagy címzett; `409`, ha már kiküldték). Szöveghossz-korlátok (lásd lent): téma ≤ 200, kérdés ≤ 500 karakter. | `…/form/FormController.java`, `…/form/FormService.java`, `…/form/FormRepository.java`, `…/form/*` recordok (`FormInput`, `QuestionInput`, `FormSummary`, `FormDetail`, `Question`, `Recipient`) | AC17–AC27 | – |
| 5 | **Kitöltések (kolléga) és válaszok (admin):** `GET /api/me/reviews`, `GET /api/me/reviews/{id}`, `POST …/submit` (validálás: kötelező válasz, skálatartomány, típusegyezés, a kérdés az űrlaphoz tartozik, szöveges válasz ≤ 2000 karakter; `409`, ha már beküldték; másik kolléga kitöltésére `404`). `GET /api/admin/forms/{id}/responses` a `form` controllerben, a `ReviewService`-en keresztül. | `…/review/ReviewController.java`, `…/review/ReviewService.java`, `…/review/ReviewRepository.java`, `…/review/*` recordok (`ReviewSummary`, `ReviewDetail`, `SubmitInput`, `AnswerInput`, `FormResponse`, `Answer`), `…/form/FormController.java` | AC28–AC42 | – |
| 6 | **Szerződés és kliens:** `npm run contract:update`, majd az Angular kliens generálása (`npm run api:client -w apps/web`). Ellenőrizni, hogy a generált típusnevek egyeznek a spec sémáival. | `contracts/openapi.json` | (minden API-AC szerződése) | – |
| 7 | **Frontend mag:** `provideHttpClient(withInterceptors(...))`, a generált kliens `rootUrl`-je; fejlesztői proxy (`/api` → `localhost:8080`). `AuthStore` (signalok; tokenek és `me` a sessionStorage-ban), `authInterceptor` (Bearer fejléc; `401`-nél egyetlen közös refresh, utána az eredeti kérés megismétlése; sikertelen refresh esetén kijelentkeztetés és `/login`), funkcionális guardok (`authGuard(role)`, `homeRedirectGuard`), `app.routes.ts` lazy `loadComponent`-tel a spec URL-jei szerint, és a navigációs keret (`shell`: menüpontok szerepkör szerint, név, Kilépés, aktív menüpont kiemelése). Minden elem `ng generate`-tel készül. | `core/auth/auth-store.service.ts`, `core/auth/auth.interceptor.ts`, `core/auth/auth.guard.ts`, `core/layout/shell/shell.component.*`, `app.routes.ts`, `app.config.ts`, `apps/web/proxy.conf.json`, `apps/web/angular.json` (`serve.options.proxyConfig`) | AC46–AC52 | – |
| 8 | **Belépés oldal:** admin űrlap (e-mail, jelszó, általános hibaüzenet) és kolléga választó; siker után továbbirányítás a szerepkör kezdőoldalára. | `features/auth/login-page/login-page.component.*` | AC43, AC50 (és AC2–AC6 felülete) | – |
| 9 | **Admin oldalak:** Űrlapok lista (állapot, „beküldött / címzett”), szerkesztő (új és piszkozat szerkesztése; typed Reactive Forms `FormArray`-jel a kérdésekhez; skálahatárok csak skálánál; mezőnkénti szerverhibák megjelenítése; Törlés megerősítéssel; Kiküldés), részletek (címzettek állapota, beküldött válaszok név szerint), Csapattagok. | `features/forms/form-list-page/*`, `features/forms/form-editor-page/*`, `features/forms/form-detail-page/*`, `features/team/team-page/*` | AC45, AC53 (és AC17–AC27, AC41–AC42 felülete) | 10-zel (más feature-mappa; az `app.routes.ts` közös, a merge-nél figyelni kell) |
| 10 | **Kolléga oldalak:** Értékeléseim lista (állapot, beküldés ideje), kitöltő oldal (skálaválasztó, szöveges mező, kötelezők jelölése, hibák megjelenítése; beküldés után vissza a listára, `Beküldve` állapot, újra nem nyitható). | `features/reviews/review-list-page/*`, `features/reviews/review-fill-page/*` | AC44 (és AC28–AC38 felülete) | 9-cel |
| 11 | **Végső ellenőrzés:** `npm run check`; kézi végigkattintás a két szerepkörrel (lásd Ellenőrzés). | – | mind | – |

### Szöveghossz-korlátok (a spec 3. döntése szerint a plan javaslata)

| Mező | Felső korlát | Túllépés |
|---|---|---|
| Fő téma | 200 karakter | `400`, mezőnkénti hiba |
| Kérdés szövege | 500 karakter | `400`, mezőnkénti hiba |
| Szöveges válasz | 2000 karakter | `400` |

## Tesztek

Backend: `apps/api/src/test/java/com/hugoboard/…`, `@SpringBootTest` + MockMvc (a
`AuthFilter`-rel együtt), valódi SQLite ideiglenes fájlban, minden teszt a saját adatait
hozza létre. A lejáratot egy tesztben állítható `Clock` adja. A tesztnév hivatkozik az
AC-re (`@DisplayName("AC23: …")`).
Frontend: Vitest, a tesztelt fájl mellett `*.spec.ts`, HTTP helyett
`HttpTestingController`.

| AC | Teszt | Fájl |
|---|---|---|
| AC1 | csapattagok listája belépés nélkül, admin nélkül | `user/TeamMemberApiTest.java` |
| AC2, AC3 | admin belépés a környezeti adatokkal; jelszócsere után a régi jelszó `401` | `auth/AdminLoginApiTest.java` |
| AC4 | hibás jelszó és nem létező e-mail: `401`, azonos üzenet | `auth/AdminLoginApiTest.java` |
| AC5, AC6 | kollégabelépés létező / nem létező taggal | `auth/MemberLoginApiTest.java` |
| AC7 | kilépés után a régi access és refresh token `401` | `auth/TokenLifecycleApiTest.java` |
| AC8 | belépési válasz: két token, élettartamok, nincs `Set-Cookie` | `auth/TokenLifecycleApiTest.java` |
| AC9 | refresh: új pár működik, a régi refresh `401` | `auth/TokenLifecycleApiTest.java` |
| AC10, AC11 | lejárt access / refresh token `401` (állított `Clock`) | `auth/TokenLifecycleApiTest.java` |
| AC12, AC13 | az `auth_session` és az `admin_account` nem tartalmaz nyílt tokent / jelszót | `auth/SecretStorageTest.java` |
| AC14–AC16 | `401` belépés nélkül; kolléga → admin `403`; admin → `me` `403` (minden `/api/admin/**` és `/api/me/**` végpontra) | `auth/AuthorizationApiTest.java` |
| AC17, AC18 | létrehozás `201` `DRAFT`; hiányos és túl hosszú adatok `400` | `form/FormApiTest.java` |
| AC19, AC20 | piszkozat módosítása; lista mezői és számlálói | `form/FormApiTest.java` |
| AC21, AC22 | piszkozat törlése `204` + `404`; kiküldött törlése `409` | `form/FormApiTest.java` |
| AC23–AC27 | kiküldés N−1 kitöltéssel; kiküldött módosítása / újrakiküldése `409`; kérdés vagy címzett nélkül `422` | `form/FormSendApiTest.java` |
| AC28–AC31 | saját lista; értékelt és piszkozat nem látszik; kitöltés részletei | `review/ReviewApiTest.java` |
| AC32–AC38 | beküldés sikeres; hiányzó kötelező, skálán kívüli, üres szöveg, idegen kérdés, túl hosszú szöveg `400`; opcionális kihagyható; újrabeküldés `409` | `review/ReviewSubmitApiTest.java` |
| AC39–AC42 | idegen kitöltés `404`; a kolléga válaszokat nem kap; admin állapotok és válaszok | `review/ReviewVisibilityApiTest.java` |
| AC43, AC50 | belépőoldal mindkét úttal; továbbirányítás szerepkör szerint | `features/auth/login-page/login-page.component.spec.ts` |
| AC44 | kitöltés és beküldés után `Beküldve`, nem nyitható újra | `features/reviews/review-fill-page/review-fill-page.component.spec.ts`, `features/reviews/review-list-page/review-list-page.component.spec.ts` |
| AC45 | admin: létrehozás, kiküldés, a beküldött kolléga állapota és válaszai a részleteknél | `features/forms/form-editor-page/form-editor-page.component.spec.ts`, `features/forms/form-detail-page/form-detail-page.component.spec.ts` |
| AC46, AC47, AC52 | menüpontok szerepkör szerint; Kilépés törli a tokeneket és `/login`-ra visz | `core/layout/shell/shell.component.spec.ts` |
| AC48, AC49 | guardok: belépés nélkül `/login`; másik szerepkör oldaláról a saját kezdőoldalra | `core/auth/auth.guard.spec.ts` |
| AC51 | lejárt access token: egyetlen refresh, az eredeti kérés megismétlődik | `core/auth/auth.interceptor.spec.ts` |
| AC53 | Csapattagok oldal neveket mutat | `features/team/team-page/team-page.component.spec.ts` |

## Ellenőrzés

- `npm run check` zöld minden lépés után.
- A `contracts/openapi.json` a 6. lépés után naprakész (a szerződésteszt őrzi).
- **Kézi ellenőrzés (verifier / ember):** `HUGO_ADMIN_EMAIL` és `HUGO_ADMIN_PASSWORD`
  beállítása a shellben, `npm run dev:api` és `npm run dev:web` két terminálban, majd:
  admin belép → űrlapot készít két kérdéssel → kiküldi → kilép; kolléga belép → kitölti
  → `Beküldve`; admin újra belép → a részleteknél látja a választ. Egy kolléga a
  `/admin/forms` címen a saját oldalára kerül.

## Kockázatok

- **Spring Boot 4 / springdoc 3 / Jackson 3 API-eltérések** (csomagnevek, MockMvc
  beállítás) → fordításkor derül ki; a meglévő szerződésteszt mintáját követjük
  (`MockMvcBuilders.webAppContextSetup`, a filter kézi hozzáadásával).
- **SQLite egy kapcsolattal (`maximum-pool-size: 1`)** → hosszú tranzakció blokkolhat;
  a tranzakciók rövidek, csak a `Service`-ben.
- **Párhuzamos `401`-ek a frontendben** több refresht indíthatnának, és a rotáció miatt a
  második elbukna → egyetlen közös, megosztott refresh kérés (`shareReplay`), a többi
  kérés arra vár.
- **AC44 és AC45 teljes folyamat** → komponensszinten (helyettesített HTTP-vel) és a
  backend API-tesztjeivel fedjük; böngészős end-to-end teszt nincs (ahhoz új függőség
  kellene). A kézi ellenőrzés pótolja.
- **Generált klienstípusok neve eltérhet** a spec sémáitól (springdoc névadás) → a 6.
  lépésben ellenőrizni; eltérésnél a record nevét igazítjuk, nem a generált kódot.
- **Admin környezeti változók hiánya** → a backend elindul, admin belépés nincs, a napló
  jelzi; a tesztek saját értéket adnak meg.
- **XSS → sessionStorage-token** (a spec által elfogadott kockázat) → `innerHTML` és
  `bypassSecurityTrust…` nincs a kódban.

## Nincs benne

- Böngészős end-to-end tesztek (Playwright) — új függőség lenne.
- Tartalombiztonsági szabályzat (CSP), rate limiting, fiókzárolás.
- Jira-import (`0002`), dashboard (`0003`).
- Csapattagok szerkesztése, admin-fiókkezelés, jelszócsere a felületen.
- A felület vizuális tervezése a spec által előírtakon túl (alap, Flexbox-alapú
  elrendezés; dizájnrendszer nincs).
