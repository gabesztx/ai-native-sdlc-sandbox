# apps/api — Spring Boot és adatbázis szabályok

A gyökér `CLAUDE.md` kiegészítése a backendhez. A cél az **egyszerű, átlátható** kód:
kevés réteg, kevés varázslat, az SQL mindig látható.

## Stack

Java 25, Spring Boot 4.1, Maven wrapper (`./mvnw`). Adatbázis: **SQLite** (egyetlen
fájl, külön szerver nélkül) + **Flyway** migrációk. Adatelérés: Spring **`JdbcClient`**,
kézzel írt SQL — JPA / Hibernate nincs. API-dokumentáció: springdoc (OpenAPI). Teszt:
JUnit 5, Spring Boot Test, MockMvc.

## Csomagstruktúra

Feature szerint, nem rétegenként. Alapcsomag: `com.hugoboard`.

```
com.hugoboard/
  auth/        belépés, munkamenet, jogosultság
  user/        csapattagok (és a 0002-ben a Jira-import)
  form/        értékelő űrlapok, kérdések, kiküldés (admin)
  review/      kitöltések és válaszok (kolléga)
  dashboard/   Jira dashboard (0003)
  jira/        Jira REST kliens, közös a user- és a dashboard-feature-nek
  common/      hibakezelés, közös konfiguráció
```

## Egy feature felépítése

Feature-önként legfeljebb ez a három fajta osztály, plusz a hozzájuk tartozó adatok:

| Osztály | Felelősség | Tilos benne |
|---|---|---|
| `…Controller` | HTTP: út, kérés-validálás, válasz, státuszkód | üzleti szabály, SQL |
| `…Service` | üzleti szabályok, tranzakció (`@Transactional`) | HTTP-fogalmak, SQL |
| `…Repository` | SQL `JdbcClient`-tel | üzleti szabály |

- Adatok: Java **`record`**-ok (kérés, válasz, adatbázis-sor). Lombok nincs.
- A kérés- és válasz-recordok az OpenAPI-szerződés alapjai: a nevük egyezzen a spec
  sémáival (pl. `FormInput`, `FormDetail`).
- DI: konstruktoron keresztül, `final` mezőkkel (Springben ez a bevett mód).
- Feature másik feature-t csak a `Service`-én keresztül hív, a `Repository`-ját soha.

## SQL és adatbázis

- SQL csak a `Repository`-ban, Java text blockban (`"""…"""`), olvashatóan tördelve.
- **Mindig nevesített paraméter** (`:id`), soha string-összefűzés — ez véd az SQL
  injection ellen.
- Típusok SQLite-ban: `INTEGER`, `TEXT`. Logikai érték: `INTEGER` 0/1. Időpont:
  `TEXT`, ISO-8601 UTC (pl. `2026-10-27T10:00:00Z`).
- Táblanév és oszlopnév: `snake_case`, egyes szám (`review_form`, `form_id`).
- Az idegen kulcsokat (`REFERENCES`) mindig írd ki; az SQLite-ban a
  `foreign_keys` bekapcsolását a konfiguráció végzi.
- Az adatbázisfájl helye `apps/api/data/`, és **soha nincs commitolva** (valódi adat
  lehet benne).

## Flyway migrációk

- Hely: `src/main/resources/db/migration/`, név: `V<n>__<leiras>.sql` (pl.
  `V1__peer_review_forms.sql`).
- **Meglévő migrációt soha ne szerkessz** — változáshoz új migráció.
- Egy migráció egy változáshoz (`docs/changes/<id>`) tartozik.
- A fejlesztői tesztadat (kitalált csapattagok) nem migrációban van, hanem külön,
  csak fejlesztői profilban betöltve.

## Hibakezelés és validáció

- Hibaválasz egységesen **RFC 9457 Problem Details** (`ProblemDetail`), egy közös
  `@RestControllerAdvice`-ből a `common/`-ban.
- Bemenet-validálás Bean Validationnel a kérés-recordokon (`@NotBlank`, `@NotNull` …);
  validációs hibánál `400` mezőnkénti hibalistával.
- Üzleti szabálysértéshez saját kivétel (pl. `FormAlreadySentException`), amit a
  közös advice fordít státuszkódra (`409`, `422` …). Controllerben nincs `try/catch`.
- Hibaüzenetben nincs belső részlet (stack trace, SQL, fájlútvonal).

## Biztonság és konfiguráció

- Titok (Jira-token, admin jelszó, munkamenet-kulcs) csak **környezeti változóból**;
  az `application.yml`-ben legfeljebb hivatkozás (`${JIRA_API_TOKEN}`).
- Jelszót csak hash-elve tárolunk (BCrypt).
- Naplóba soha: token, jelszó, munkamenet-azonosító, értékelő válaszok tartalma.

## Kommentek

Angolul, és csak ott, ahol a *miért* nem derül ki a kódból. Ami a kódból látszik, azt
ne kommenteld.

## Tesztek

- Minden spec-elfogadási kritériumhoz legalább egy teszt; a neve hivatkozzon rá:
  `@DisplayName("AC16: kiküldött űrlap módosítása 409")`.
- API-viselkedés: `@SpringBootTest` + MockMvc, valódi SQLite-tal egy ideiglenes
  fájlban — az adatbázist nem mockoljuk.
- Külső hívás (Jira) teszt közben soha: a Spring `MockRestServiceServer`
  helyettesíti.
- Egy teszt a saját adatait hozza létre; tesztek nem függhetnek egymás sorrendjétől.

## OpenAPI-szerződés

- A springdoc generálja; a `contracts/openapi.json` a build része, commitolva.
- Ha az API változik, a fájlt újra kell generálni; az `npm run check` elbukik, ha elavult.
