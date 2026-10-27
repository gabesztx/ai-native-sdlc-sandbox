# Plan: Jira csapat-dashboard – 1. fázis: sprint-áttekintés

- **ID:** 0003-jira-team-dashboard
- **Státusz:** Approved
- **Spec:** [spec.md](spec.md)
- **Dátum:** 2026-10-27

## Megközelítés

Új `com.hugoboard.dashboard` csomag kapja az üzleti logikát; a `jira/` csomag (0002-ből)
bővül új DTO-mezőkkel és kliens-metódusokkal, de a 0002-ben használt metódusok
(`findActiveSprintId`, `fetchAssignees`) változatlanok maradnak. A backend egy
`@Scheduled(fixedDelay=...)` időzítő minden ciklusban **két** Jira-hívást tesz:

1. **Delta-lekérdezés** — csak a legutóbbi sikeres lekérdezés óta módosult feladatok
   (JQL `updated >= …`), ezekkel frissül a pillanatkép; ha egy ilyen feladat `sprint`
   mezője már nem az aktív sprint, a sora megmarad, de `sprint_id = NULL` lesz
   (AC13 — kikerült a sprintből, de az adata megmarad).
2. **Kulcs-lista lekérdezés** — az aktív sprint **összes jelenlegi** feladatkulcsa
   (csak `fields=key`, olcsó hívás); amelyik nálunk `sprint_id`-ja az aktív sprintre
   mutat, de már nincs ebben a listában, az a Jirában **törölve lett** → a sora
   **törlődik** a pillanatképből (AC14). Enélkül a törlés nem derülne ki, mert egy
   törölt feladat sosem jelenik meg „módosultként”.

A `fixedDelay` (nem `fixedRate`) ütemezés önmagában biztosítja, hogy egy lekérdezés
sosem fut párhuzamosan a következővel (AC8) — nincs szükség külön zárolásra. A
`DashboardSyncScheduler` csak egy vékony `@Scheduled` wrapper a `DashboardSyncService`
köré, ugyanúgy, ahogy a `JiraTeamImportRunner` csak hívja a `JiraTeamImportService`-t —
ezért a tesztek a szolgáltatást közvetlenül hívják, nem várnak az időzítőre.

Az SSE-t sima Spring MVC `SseEmitter`-rel oldjuk meg (nem kell WebFlux/reaktív verem);
az `AuthFilter` szinkron, `chain.doFilter()`-rel visszatér, ezért nem blokkolja az
aszinkron SSE-választ. A `RoleInterceptor`-t bővíteni kell, mert ma csak
`/api/admin/**`, `/api/me/**` és a kőbeégetett `/api/auth/me` igényel belépést — a
`/api/dashboard**` útvonalakat explicit fel kell venni mint „bármely belépett
szerepkör”.

A frontend az `/api/dashboard` és `/api/dashboard/issues` híváshoz a generált klienst
használja; a stream-hez (`/api/dashboard/stream`) **nem**, mert az `EventSource` nem
tud fejlécet küldeni — egy kézzel írt, `fetch`-alapú szolgáltatás olvassa, a meglévő
`AuthStoreService`/`TokenRefreshService` segítségével kézzel kezelve a Bearer-fejlécet
és a token-frissítést (az `auth.interceptor.ts` nem vonatkozik `fetch`-re).

## Lépések

| # | Mit | Fájl(ok) | AC | Párhuzamosítható |
|---|---|---|---|---|
| 1 | Flyway migráció: `jira_sprint`, `jira_issue`, `jira_sync_state` táblák (`CHECK` az enumokra, egyedi index `issue_key`-re, egysoros `jira_sync_state` `CHECK (id = 1)`-gyel) | `apps/api/src/main/resources/db/migration/V3__jira_dashboard.sql` | AC1–AC23 (előfeltétel) | – |
| 2 | `JiraSprint` bővítése: `name`, `startDate`, `endDate` mezők (a 0002-es `findActiveSprintId()` nem törik, csak `id`/`state`-et használ) | `apps/api/src/main/java/com/hugoboard/jira/JiraSprint.java` | AC5, AC6, AC21 | 3-mal |
| 3 | `JiraIssue`/`Fields` bővítése: `key` a rekord tetején, `summary`, `status` (új `JiraStatus(name, statusCategory)` + `JiraStatusCategory(key)` beágyazott rekord Jira `new`/`indeterminate`/`done` kulcsaihoz), `updated`; a 0002-es `fetchAssignees()` nem törik, csak `assignee`-t olvas | `apps/api/src/main/java/com/hugoboard/jira/JiraIssue.java` | AC1–AC3, AC7, AC8, AC11–AC18, AC21–AC23 | 2-vel |
| 4 | `JiraClient` új metódusai: `findActiveSprint()` (teljes `JiraSprint`, a `findActiveSprintId()` változatlan marad), `fetchActiveSprintIssueKeys(sprintId)` (csak `fields=key`, lapozva — a törlésellenőrzéshez), `fetchIssuesUpdatedSince(sprintId, updatedAtIso)` (JQL `updated >= …`, `fields=summary,status,assignee,updated`, lapozva); mindhárom a meglévő `client()`/`JiraConfig` singleton `RestClient.Builder`-mintát követi | `apps/api/src/main/java/com/hugoboard/jira/JiraClient.java` | AC1–AC3, AC11–AC18, AC21–AC23 | – |
| 5 | `JiraRateLimitException` — a `JiraImportException` testvére, a Jira `Retry-After` fejlécét hordozza; a `JiraClient` ezt dobja `429` válasznál | `apps/api/src/main/java/com/hugoboard/jira/JiraRateLimitException.java` | AC17 | 4-gyel |
| 6 | `application.yml`: `hugo.dashboard.poll-interval-ms` (alapérték 30000, `HUGO_DASHBOARD_POLL_INTERVAL_MS`-ből felülírható) | `apps/api/src/main/resources/application.yml` | AC11–AC21 (előfeltétel) | – |
| 7 | `@EnableScheduling` a fő alkalmazásosztályon | `apps/api/src/main/java/com/hugoboard/ApiApplication.java` | AC11–AC21 (előfeltétel) | – |
| 8 | `com.hugoboard.dashboard` csomag: belső adatrekordok (`DashboardSprint`, `DashboardIssue`) és a Jira `statusCategory.key` → `TODO`/`IN_PROGRESS`/`DONE` leképezés | `apps/api/src/main/java/com/hugoboard/dashboard/StatusCategoryMapper.java` | AC1–AC3 | 9-cel |
| 9 | `DashboardRepository` (`JdbcClient`): aktív sprint lekérdezése/felülírása, feladatok upsertje, `clearSprintIdForIssuesNoLongerInActiveSprint(...)` (AC13), `deleteIssuesNotIn(activeSprintId, liveKeys)` (AC14), `last_synced_at` olvasás/írás, KPI-khez és lefúráshoz szükséges lekérdezések (`findDashboardSnapshot()`, `findIssuesByTile(tile)`) | `apps/api/src/main/java/com/hugoboard/dashboard/DashboardRepository.java` | AC1–AC4, AC7–AC9, AC11–AC18, AC21–AC23 | – |
| 10 | `DashboardSyncService.syncNow()`: lekéri az aktív sprintet; ha változott (új sprint indult), teljes újratöltés (mint a kezdeti betöltés); különben delta-lekérdezés + kulcs-lista törlésellenőrzés (lásd Megközelítés); hiba esetén naplóz (token nélkül), a pillanatkép és `last_synced_at` változatlan marad; `429`-nél eltárolja, meddig nem szabad újra próbálni, és addig kihagyja a ciklust; sikeres, változást hozó szinkron után hívja a broadcastot | `apps/api/src/main/java/com/hugoboard/dashboard/DashboardSyncService.java` | AC11–AC23 | – |
| 11 | `DashboardSyncScheduler` — vékony `@Scheduled(fixedDelayString = "${hugo.dashboard.poll-interval-ms}")` wrapper, csak a szolgáltatást hívja | `apps/api/src/main/java/com/hugoboard/dashboard/DashboardSyncScheduler.java` | AC8 (vezénylés) | – |
| 12 | `DashboardEventBroadcaster` — regisztrált `SseEmitter`-ek szálbiztos nyilvántartása, `broadcast(Dashboard)`, kapcsolódáskor azonnali első esemény, lezáráskor/hibánál eltávolítás | `apps/api/src/main/java/com/hugoboard/dashboard/DashboardEventBroadcaster.java` | AC19, AC20 | – |
| 13 | `DashboardService` — a `Dashboard` és `Issue[]` API-válasz összeállítása a `DashboardRepository` adataiból (KPI-számítás: `donePercent` kerekítve, `blocked` = `status_name = 'Blocked'`, `unassigned` = üres `assignee_account_id`); `Tile` enum (`ALL`,`DONE`,`IN_PROGRESS`,`BLOCKED`,`UNASSIGNED`) | `apps/api/src/main/java/com/hugoboard/dashboard/DashboardService.java`, `Tile.java`, `Dashboard.java`, `Issue.java`, `Sprint.java` (API-rekordok) | AC1–AC10 | – |
| 14 | `DashboardController` — `GET /api/dashboard`, `GET /api/dashboard/issues?tile=` (ismeretlen `tile` → `BadRequestException`, meglévő `ApiExceptionHandler`-mintával → `400`), `GET /api/dashboard/stream` (`SseEmitter`, regisztrálja a broadcasternél, azonnali első esemény) | `apps/api/src/main/java/com/hugoboard/dashboard/DashboardController.java` | AC1–AC4, AC7–AC10, AC19, AC20, AC24, AC25 | – |
| 15 | `RoleInterceptor` bővítése: `/api/dashboard` és minden alútvonala bármely belépett szerepkörnek engedélyezett (mint a kőbeégetett `/api/auth/me`), bejelentkezés nélkül `401` | `apps/api/src/main/java/com/hugoboard/auth/RoleInterceptor.java` | AC20, AC24, AC25 | – |
| 16 | `contracts/openapi.json` újragenerálása (`npm run contract:update`) — **ez az első API-változás 0002 óta** | `contracts/openapi.json` | előfeltétel a klienshez | – |
| 17 | Angular kliens generálása (`npm run api:client -w apps/web`) | `apps/web/src/app/core/api/*` (gitignore-olt) | előfeltétel | – |
| 18 | `authGuard()` bővítése/új „bármely belépett szerepkör” guard-változat (a mai `authGuard(role)` csak egy konkrét szerepkört enged) | `apps/web/src/app/core/auth/auth.guard.ts` | AC24, AC25 | – |
| 19 | `/dashboard` útvonal felvétele a `ShellComponent` gyermekei közé (az `admin`/`reviews` melletti testvér, mindkét szerepkörnek), `features/dashboard/dashboard.routes.ts` | `apps/web/src/app/app.routes.ts`, `apps/web/src/app/features/dashboard/dashboard.routes.ts` | AC24, AC25 | – |
| 20 | Navigációs menüpont a Dashboardhoz mindkét szerepkörnek (meglévő shell-menü minta szerint) | `apps/web/src/app/core/layout/shell/shell.component.ts` (vagy ahol a menüelemek vannak) | Felület (előfeltétel) | – |
| 21 | `DashboardService` (web) — a generált klienst csomagolja (`getDashboard()`, `getIssues(tile)`); komponens sosem hívja közvetlenül a generált service-t | `apps/web/src/app/features/dashboard/services/dashboard.service.ts` | AC1–AC10 | 22-vel |
| 22 | `DashboardStreamService` — kézzel írt `fetch`-alapú SSE-olvasó: `Authorization: Bearer` fejléc az `AuthStoreService`-ből, `401`/lejárt tokennél `TokenRefreshService.refresh()` majd újrakapcsolódás, automatikus újrakapcsolódás hálózati hiba esetén, `connected`/`paused` jelző signal | `apps/web/src/app/features/dashboard/services/dashboard-stream.service.ts` | AC19, AC20, AC26, AC27 | 21-gyel |
| 23 | `DashboardPageComponent` — KPI-csempék, haladási sáv, lefúrási lista (dialógus vagy beágyazott szekció), „Nincs aktív sprint” / „Az adatok nem érhetők el” / „Élő frissítés szünetel” állapotok, feladatsorra kattintva `<a target="_blank">` a Jira URL-re, meglévő PrimeNG-komponensekkel (`Card`/`ProgressBar`/`Tag`/`Table`), a `0004`-ben lefektetett státusz-színkódolás kiterjesztve a Jira státuszkategóriákra | `apps/web/src/app/features/dashboard/pages/dashboard-page/dashboard-page.component.{ts,html,scss,spec.ts}` | AC2, AC4, AC7–AC10, AC26–AC28 | – |

## Tesztek

| AC | Teszt | Fájl |
|---|---|---|
| AC1–3 | KPI-számítás vegyes státuszokkal | `DashboardSyncServiceTest.java` (mock Jira-adat) vagy `DashboardServiceTest.java` |
| AC4 | üres aktív sprint → minden KPI 0 | ua. |
| AC5–6 | haladási sáv számítás (folyamatban lévő / lejárt sprint) | `DashboardServiceTest.java` |
| AC7–9 | lefúrás csempénként, ismeretlen `tile` → `400` | `DashboardControllerApiTest.java` |
| AC10 | csempe→lista→Jira-link (href, `target=_blank`) | `dashboard-page.component.spec.ts` |
| AC11–18, AC21 | delta-lekérdezés, új/törölt/kikerült feladat, 429 várakozás, `jira_updated_at` versenyhelyzet, sprintváltás | `DashboardSyncServiceTest.java` (`MockRestServiceServer`, a szolgáltatás közvetlen hívásával, nem időzítőre várva) |
| AC19–20 | SSE: csatlakozáskor azonnali esemény, változásnál küldés, belépés nélkül `401` | `DashboardControllerApiTest.java` (MockMvc async/SSE támogatással) |
| AC22–23 | kezdeti betöltés sikeres/sikertelen Jirával | `DashboardSyncServiceTest.java` |
| AC24–25 | jogosultság: admin és kolléga is `200`, belépés nélkül `401` | `DashboardControllerApiTest.java`; bővíteni a meglévő `AuthorizationApiTest` mátrixot a `/api/dashboard**` útvonalakkal |
| AC26–27 | élő frissítés újratöltés nélkül, „Élő frissítés szünetel” jelzés és eltűnése | `dashboard-page.component.spec.ts` (stream-service mockolásával) |
| AC28 | szín-konzisztencia világos/sötét témában | **kézi ellenőrzés** (mint a `0001` AC19-es klikk-tesztje) — jsdom nem rendereli a tényleges CSS-színeket |

## Ellenőrzés

- `npm run check` zöld minden lépés után.
- Kézi ellenőrzés (verifier): `contracts/openapi.json` ezúttal **valóban változik** — ez
  elvárt, nem hiba (ellentétben a 0002-vel).
- Kézi ellenőrzés: AC28 (szín-konzisztencia világos/sötét témában a csempén, listán,
  címkén).
- A `DashboardSyncServiceTest`-ben és a `JiraClientTest` bővítésében nem történhet
  valódi hálózati hívás.
- A napló sosem tartalmazza a `HUGO_JIRA_API_TOKEN` értékét.

## Kockázatok

- **Két Jira-hívás pollingonként** (delta + kulcs-lista) megduplázza a kéréskvótát →
  a `JiraConfig` időkorlátjai és a `429`-kezelés (AC17) védenek a túligénybevétel ellen;
  a kulcs-lista hívás `fields=key`-re szorítkozik, így olcsó.
- **`SseEmitter` + szervlet szál-korlát** sok egyidejű nyitott dashboarddal → ez a HUGO
  csapat méretéhez (néhány tucat fő) képest elhanyagolható kockázat, nem igényel
  reaktív veremre váltást.
- **Jira `statusCategory.key` értékei** (`new`/`indeterminate`/`done`) eltérhetnek a
  feltételezettől → a `StatusCategoryMapper` egy helyen, explicit `switch`-csel
  leképezve, nem szóródik szét a kódban; eltérés esetén egy hely javítandó.
- **`RoleInterceptor` bővítése hibásan** engedélyezné a `/api/dashboard**`-ot belépés
  nélkül is → AC20/AC24 teszt erre kifejezetten figyel, és a meglévő
  `AuthorizationApiTest` parametrizált mátrixa bővül vele.
- **A frontend `fetch`-alapú SSE-kliens** a meglévő `HttpClient`-interceptoron kívül esik
  → saját token-csatolás és frissítés kell, ezt a `DashboardStreamService` tesztje
  külön ellenőrzi (lejárt token → `TokenRefreshService.refresh()` → újracsatlakozás).
- **A globális `@EnableScheduling` a teszt Spring-kontextusban is elindítja az időzítőt**
  → egy hosszabb ideig élő teszt-kontextusban a valódi `@Scheduled` kör véletlenül
  lefuthatna a `MockRestServiceServer` ellen, és váratlan/extra hívással buktathatná a
  pontos elvárás-listás teszteket. Megoldás: a `DashboardSyncServiceTest` és minden más,
  Jirát mockoló teszt a `hugo.dashboard.poll-interval-ms` property-t egy nagyon nagy
  értékre állítja (`@SpringBootTest(properties = "hugo.dashboard.poll-interval-ms=999999999")`),
  így a valódi időzítő a teszt futása alatt gyakorlatilag sosem sül el; a tesztek a
  `DashboardSyncService.syncNow()`-t mindig közvetlenül hívják.

## Nincs benne

- Az intent 2. és 3. fázisa (csapattag-kártyák, hőtérkép, Tempo, flow-diagramok,
  eseményfolyam, nézetek, heti összefoglaló).
- Trendgörbék, időszakváltó (előző sprint, utolsó 30 nap) — az adatok megmaradnak,
  de a szűrő felület később jön.
- Jira webhook, Automation, felhős közvetítő.
- WebSocket, Tempo-adatok.
- A `/dashboard` route-ot nem teszi alapértelmezett kezdőlappá (`homePathFor`
  változatlan) — a spec ezt nem írja elő, csak elérhetőséget mindkét szerepkörnek.
