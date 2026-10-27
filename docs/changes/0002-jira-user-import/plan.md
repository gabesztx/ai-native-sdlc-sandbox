# Plan: Felhasználólista lekérése a Jirából

- **ID:** 0002-jira-user-import
- **Státusz:** Approved
- **Spec:** [spec.md](spec.md)
- **Dátum:** 2026-10-27

## Megközelítés

Új `jira/` feature-csomag kapja a Jira REST-kliens (Spring `RestClient`), az `user/`
csomag pedig az importálás üzleti logikáját és a meglévő `team_member` adatelérést. Az
import indulásakor egyszer, memóriában gyűjti össze a sprint összes oldaláról az
assignee-ket (deduplikálva), és csak a végén, egyetlen tranzakcióban írja az
adatbázisba — így a lapozás közbeni hiba (AC9) automatikusan nem hagy félkész állapotot,
mert addig egyetlen sor sem íródott. Nincs új végpont és nincs OpenAPI-változás: a
`TeamMember` API-válasz (`id`, `name`) változatlan, csak a `TeamMemberRepository` SQL-je
szűr `active = 1`-re — ez a szűrés magától érvényesül mindenhol, ahol a `TeamMemberService`-en
keresztül kérik le a listát (`FormService.send` is így működik, ellenőrizve).

A `RestClient`-et a teszteknek a Spring `MockRestServiceServer`-rel kell tudniuk
helyettesíteniük (CLAUDE.md-követelmény). Ezért a `JiraClient` nem gyorsítótárazza a
`RestClient`-et: minden hívásnál a befecskendezett, **singleton** `RestClient.Builder`-ből
építi fel, így a teszt a hívás előtt tudja hozzákötni a `MockRestServiceServer`-t
ugyanahhoz a builderhez.

## Lépések

| # | Mit | Fájl(ok) | AC | Párhuzamosítható |
|---|---|---|---|---|
| 1 | Flyway migráció: `team_member` bővítése (`jira_account_id`, `email`, `avatar_url`, `active` NOT NULL DEFAULT 1, `imported_at`), egyedi parciális index `jira_account_id`-ra | `apps/api/src/main/resources/db/migration/V2__jira_team_import.sql` | AC1, AC5, AC6, AC15 (előfeltétel) | – |
| 2 | Jira-kapcsolat beállítása: `hugo.jira.*` kulcsok az env változókra hivatkozva | `apps/api/src/main/resources/application.yml` | AC14 (előfeltétel) | – |
| 3 | `JiraProperties` — a 4 env változó beolvasása, `isConfigured()` (csak akkor igaz, ha mind a négy nem üres) | `apps/api/src/main/java/com/hugoboard/jira/JiraProperties.java` | AC14 | – |
| 4 | Jira válasz-DTO-k (Jackson rekordok, `@JsonIgnoreProperties(ignoreUnknown = true)`): sprint-lista, feladat-lap (lapozási mezőkkel), feladat, assignee (`accountId`, `displayName`, `emailAddress`, avatar URL, `active`) | `apps/api/src/main/java/com/hugoboard/jira/JiraSprint.java`, `JiraSprintPage.java`, `JiraIssuePage.java`, `JiraIssue.java`, `JiraUser.java` | AC1, AC2, AC3, AC4, AC5, AC6 | 5-tel |
| 5 | `JiraImportException` — egyetlen, nem-titkot-tartalmazó kivétel minden Jira-hibára (HTTP hiba, timeout, nincs aktív sprint, tábla nem található) | `apps/api/src/main/java/com/hugoboard/jira/JiraImportException.java` | AC8, AC10, AC11 | 4-gyel |
| 6 | `JiraConfig` — singleton `RestClient.Builder` bean, kapcsolódási és olvasási időkorláttal | `apps/api/src/main/java/com/hugoboard/jira/JiraConfig.java` | AC11 | – |
| 7 | `JiraClient` — Basic Auth (`email` + API-token); `findActiveSprintId(boardId)`; `fetchAssignees(sprintId)` lapozva, amíg van következő oldal; minden hibát `JiraImportException`-re fordít | `apps/api/src/main/java/com/hugoboard/jira/JiraClient.java` | AC1, AC2, AC3, AC4, AC8, AC10, AC11 | – |
| 8 | `TeamMemberRepository` bővítése: `findAll()`/`findById()` szűrés `WHERE active = 1`-re; új `insertImported(...)` metódus a Jira-mezőkkel; `insert(name)` és `count()` változatlan | `apps/api/src/main/java/com/hugoboard/user/TeamMemberRepository.java` | AC6, AC7, AC15, AC16 | – |
| 9 | `JiraTeamImportService.importIfNeeded()` — ha a tábla nem üres: kilép (AC7); ha nincs beállítva: naplóz, kilép; különben lekéri az aktív sprintet és az assignee-ket, `accountId` szerint deduplikál (AC2), kihagyja az assignee nélküli feladatokat (AC3), és egyetlen `@Transactional` blokkban menti az eredményt; a `JiraImportException`-t elkapja, hibát naplóz (token nélkül), és üres táblával tér vissza | `apps/api/src/main/java/com/hugoboard/user/JiraTeamImportService.java` | AC1–AC11 | – |
| 10 | `JiraTeamImportRunner` (`ApplicationRunner`) — indításkor meghívja az `importIfNeeded()`-et, profiltól függetlenül | `apps/api/src/main/java/com/hugoboard/user/JiraTeamImportRunner.java` | AC7, AC12, AC13 (vezénylés) | – |
| 11 | `DevTeamSeeder` módosítása: ha a Jira be van állítva (`JiraProperties.isConfigured()`), ne töltsön be kitalált csapattagot, akkor sem, ha a tábla üres | `apps/api/src/main/java/com/hugoboard/user/DevTeamSeeder.java` | AC12, AC13 | – |
| 12 | Teszt-segéd bővítése: `insertMember(name, active)` (és/vagy `insertImportedMember(...)`) az inaktív/importált csapattag közvetlen beszúrásához | `apps/api/src/test/java/com/hugoboard/support/ApiTestSupport.java` | AC15, AC16 (előfeltétel) | – |
| 13 | `JiraClient` egységtesztje: `MockRestServiceServer`-rel kötött, önálló `RestClient.Builder`-rel (Spring-kontextus nélkül) — sprint-keresés, lapozás, assignee-nélküli feladat kihagyása, 401/500 hiba, timeout | `apps/api/src/test/java/com/hugoboard/jira/JiraClientTest.java` | AC1, AC2, AC3, AC4, AC8, AC10, AC11 | 14-gyel |
| 14 | `JiraTeamImportService` integrációs tesztje: `@SpringBootTest`, a singleton `RestClient.Builder` bean-hez kötött `MockRestServiceServer`, a `hugo.jira.*` property-k teszt-értékre állítva; minden AC egy-egy teszteset, közvetlenül `importIfNeeded()`-et hívva (nem az alkalmazásindításon keresztül) | `apps/api/src/test/java/com/hugoboard/user/JiraTeamImportServiceTest.java` | AC1, AC2, AC3, AC4, AC5, AC6, AC7, AC8, AC9, AC10, AC11, AC14 | 13-mal |
| 15 | Dev-profil indulási teszt: `@SpringBootTest(...)` `@ActiveProfiles("dev")`, egyszer Jira-beállítás nélkül (kitalált tagok jönnek létre), egyszer beállítással és mock Jira-val a kontextusindítás előtt bekötve (nincs kitalált tag) | `apps/api/src/test/java/com/hugoboard/user/DevTeamSeederTest.java` | AC12, AC13 | – |
| 16 | `TeamMemberApiTest` bővítése: inaktív és aktív tag egyszerre a táblában → a végpont csak az aktívat adja vissza, csak `id`/`name` mezővel | `apps/api/src/test/java/com/hugoboard/user/TeamMemberApiTest.java` | AC15 | – |
| 17 | `FormSendApiTest` bővítése: inaktív (importált) csapattag nem kap kitöltést kiküldéskor | `apps/api/src/test/java/com/hugoboard/form/FormSendApiTest.java` | AC16 | – |

## Tesztek

| AC | Teszt | Fájl |
|---|---|---|
| AC1 | 3 különböző assignee → 3 csapattag, teljes adattal | `JiraTeamImportServiceTest.java` |
| AC2 | ugyanaz a felhasználó több feladaton → egyszer szerepel | `JiraTeamImportServiceTest.java`, `JiraClientTest.java` |
| AC3 | assignee nélküli feladat nem hoz létre tagot | `JiraTeamImportServiceTest.java`, `JiraClientTest.java` |
| AC4 | több oldalas feladatlista → minden oldal assignee-je bekerül | `JiraTeamImportServiceTest.java`, `JiraClientTest.java` |
| AC5 | hiányzó e-mail → üres e-maillel jön létre a tag | `JiraTeamImportServiceTest.java` |
| AC6 | inaktív Jira-fiók → `active = 0`, sehol nem látszik/választható | `JiraTeamImportServiceTest.java`, `TeamMemberApiTest.java` |
| AC7 | nem üres tábla → nincs Jira-hívás, tagok változatlanok | `JiraTeamImportServiceTest.java` |
| AC8 | Jira-hiba (401/500) → app elindul, tábla üres, napló ok nélkül tokent | `JiraTeamImportServiceTest.java`, `JiraClientTest.java` |
| AC9 | hiba a lapozás közben → egyetlen tag sem mentődik | `JiraTeamImportServiceTest.java` |
| AC10 | tábla nem létezik / nincs aktív sprint → tábla üres marad, napló megnevezi az okot | `JiraTeamImportServiceTest.java`, `JiraClientTest.java` |
| AC11 | Jira nem válaszol (timeout) → hiba, app elindul, tábla üres | `JiraTeamImportServiceTest.java`, `JiraClientTest.java` |
| AC12 | dev profil, nincs Jira-beállítás → kitalált csapattagok, nincs Jira-hívás | `DevTeamSeederTest.java` |
| AC13 | dev profil, van Jira-beállítás → import fut, nincs kitalált tag | `DevTeamSeederTest.java` |
| AC14 | részleges Jira-beállítás → nincs hívás, figyelmeztetés érték nélkül | `JiraTeamImportServiceTest.java` |
| AC15 | `GET /api/auth/team-members` csak aktív tagot ad vissza, `id`/`name` mezővel | `TeamMemberApiTest.java` |
| AC16 | kiküldéskor a többi aktív importált tag kap kitöltést, az inaktív nem | `FormSendApiTest.java` |

## Ellenőrzés

- `npm run check` zöld minden lépés után.
- Kézi ellenőrzés (verifier): a `V2__jira_team_import.sql` nem módosítja a `V1`-et; a
  `contracts/openapi.json` **nem** változik (nincs API-módosítás) — ha mégis változna, az
  hibajel, nem kell újragenerálni és commitolni.
- A `JiraTeamImportServiceTest` és a `JiraClientTest` közben nem történhet valódi hálózati
  hívás (a `MockRestServiceServer` minden várt kérést lefed; egy be nem állított kérés a
  teszt bukásával jár).
- A napló (teszt közbeni és kézi ellenőrzés) sosem tartalmazza a `HUGO_JIRA_API_TOKEN`
  értékét.

## Kockázatok

- **A Jira Agile REST API pontos válaszformátuma eltér a feltételezettől** (pl. lapozási
  mezők neve) → a DTO-k `ignoreUnknown = true`-val toleránsak az extra mezőkre; a
  `JiraClientTest` közvetlenül a feltételezett JSON-sémára épül, így eltérés esetén ott
  bukik, nem élesben.
- **`RestClient.Builder` bean-skóp ütközés** (Boot alapértelmezett `RestClient.Builder`-je
  prototípus) → a `JiraConfig` saját, megnevezett singleton bean-t ad, amit a `JiraClient`
  és a teszt is ugyanazon példányként kap meg.
- **Indítás elakadása lassú/nem válaszoló Jira miatt** → explicit kapcsolódási és olvasási
  időkorlát a `JiraConfig`-ban (AC11).
- **Token a naplóba kerül** egy jövőbeli hibaüzenet-bővítésnél → a `JiraImportException`
  üzenete sosem tartalmazhatja a nyers HTTP-kérést/fejléceket, csak a hiba okát (státuszkód,
  rövid leírás).
- **`DevTeamSeeder` és `JiraTeamImportRunner` sorrendje** → mindkettő a saját, egymástól
  független feltételét nézi (`JiraProperties.isConfigured()`, ill. `count() == 0`), így a
  futási sorrendjük nem befolyásolja a kimenetet.

## Nincs benne

- `contracts/openapi.json` újragenerálása és Angular-kliens generálás — nincs API-változás.
- Bármilyen frontend-módosítás.
- Folyamatos szinkronizálás, webhook, újraimportálás, Jira-bejelentkezés, Jira-írás — lásd
  spec „Nincs benne”.
- Az avatar URL megjelenítése bárhol (csak mentés) — a `0003` 2. fázisa.
