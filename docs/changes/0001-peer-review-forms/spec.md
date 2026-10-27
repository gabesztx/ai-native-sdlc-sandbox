# Spec: Kollégák értékelése űrlapokkal

- **ID:** 0001-peer-review-forms
- **Státusz:** Approved
- **Intent:** [intent.md](intent.md)
- **Dátum:** 2026-10-27

## Összefoglaló

Az admin e-mail címmel és jelszóval belép, értékelő űrlapot állít össze egy adott
csapattagról (fő téma + saját kérdések: skála vagy szabad szöveg, kötelező vagy
opcionális), és kiküldi a csapat többi tagjának. A kollégák a felhasználólistából
választják ki magukat, kitöltik és beküldik a nekik szóló űrlapokat. A kitöltés állapota
mindkét oldalon látszik, a válaszokat csak az admin látja.

## Viselkedés

### Szereplők és belépés

- **Csapattag (kolléga):** a fix, kitalált felhasználólista egy tagja. Jelszó nincs.
- **Admin:** egyetlen, előre beállított fiók (e-mail + jelszó környezeti változóból).
  Nem csapattag: nem szerepel a felhasználólistában, nem értékelhető, nem kap űrlapot.
- A kezdőoldal a belépés. Két út van rajta:
  1. **Admin belépés:** e-mail cím + jelszó.
  2. **Kolléga belépés:** a csapattagok listájából választás, jelszó nélkül.
- Belépés után a rendszer a szerepkörnek megfelelő felületre visz. Kilépéssel a
  kezdőoldalra kerül a felhasználó.
- Belépés nélkül csak a kezdőoldal és a csapattagok listája (a választóhoz) érhető el.

### Munkamenet: tokenek (süti nélkül)

- Sikeres belépéskor a backend két véletlen tokent ad:
  - **access token:** 15 percig érvényes, minden védett hívás
    `Authorization: Bearer <token>` fejlécben küldi;
  - **refresh token:** 8 óráig érvényes, csak új tokenpár kérésére szolgál.
- A frontend mindkettőt a **sessionStorage**-ban tárolja. Sütit a rendszer nem használ.
  Következmény: a böngészőlap bezárásakor a munkamenet megszűnik, és új lapon újra be
  kell lépni.
- Ha az access token lejárt (`401`), a frontend a refresh tokennel **automatikusan** új
  tokenpárt kér, és megismétli az eredeti hívást; a felhasználó ebből nem lát semmit.
- Frissítéskor a régi refresh token **érvénytelenné válik** (rotáció); egy már
  felhasznált refresh token újbóli használata `401`.
- Ha a refresh token is lejárt vagy érvénytelen, a felhasználó a kezdőoldalra kerül, és
  újra be kell lépnie.
- **Kilépéskor** a kilépés bizonyítéka maga a **refresh token**, nem az access token:
  a backend ez alapján azonosítja és azonnal érvényteleníti a munkamenetet (mindkét
  tokent), akkor is, ha az access token közben már lejárt. Így a kilépés `Authorization`
  fejléc nélkül, csak a refresh tokennel is működik. A frontend a válasz után törli a
  tokeneket a sessionStorage-ból.
- Ha a kilépéskor küldött refresh token ismeretlen (már korábban kilépett, vagy sosem
  létezett), a kilépés akkor is **sikeres** (`204`): nincs mit érvényteleníteni.
- A tokeneket a backend csak hash-elve tárolja, és soha nem naplózza.

### Navigáció

Belépés után mindkét szerepkör egy közös keretet lát: felül a navigációs sáv a saját
menüpontjaival, a bejelentkezett felhasználó nevével és a **Kilépés** gombbal.

| Szerepkör | Menüpontok | Kezdőoldal belépés után |
|---|---|---|
| **Admin** | Űrlapok · Új űrlap · Csapattagok | Űrlapok |
| **Kolléga** | Értékeléseim | Értékeléseim |

| Oldal | Út (URL) | Ki látja |
|---|---|---|
| Belépés | `/login` | mindenki |
| Űrlapok listája | `/admin/forms` | admin |
| Új űrlap (szerkesztő) | `/admin/forms/new` | admin |
| Űrlap részletei, állapotok, válaszok | `/admin/forms/{id}` | admin |
| Piszkozat szerkesztése | `/admin/forms/{id}/edit` | admin |
| Csapattagok | `/admin/team` | admin |
| Értékeléseim | `/reviews` | kolléga |
| Kitöltés | `/reviews/{id}` | kolléga |

- Belépés nélkül bármely védett oldal a `/login`-ra visz.
- Más szerepkör oldalára lépve (pl. kolléga a `/admin/...` címen) a felhasználó a saját
  kezdőoldalára kerül.
- A `/` cím belépés előtt a `/login`-ra, utána a saját kezdőoldalra visz.
- A menüben mindig kiemelve látszik az aktuális oldal.

### Fő folyamat – admin

1. Az admin belép e-mail címmel és jelszóval.
2. Az **Űrlapok** listán látja az összes űrlapot: fő téma, értékelt személy, állapot
   (`Piszkozat` / `Kiküldve`), kiküldött űrlapnál a beküldöttek száma a címzettekhez
   képest (pl. „3 / 5”).
3. **Új űrlapot** hoz létre:
   - megadja a **fő témát**;
   - kiválasztja az **értékelt személyt** a csapattagok közül;
   - tetszőleges számú **kérdést** ad hozzá, mindegyikhez:
     - a kérdés szövege;
     - típusa: **skála** vagy **szabad szöveg**;
     - skálánál a skála **legkisebb és legnagyobb értéke** (egész számok);
     - **kötelező** vagy **opcionális**;
   - a kérdések sorrendje a megadás sorrendje.
4. Mentéskor az űrlap **`Piszkozat`** állapotú. Piszkozatként szabadon módosítható, és
   **törölhető** (megerősítés után: „Biztosan törlöd?”). A törölt piszkozat a
   kérdéseivel együtt végleg eltűnik.
5. Az admin **kiküldi** az űrlapot:
   - a címzettek a csapat összes tagja, **az értékelt személy kivételével**;
   - minden címzetthez létrejön egy **kitöltés** `Kitöltendő` állapottal;
   - az űrlap állapota `Kiküldve` lesz, és onnantól **nem módosítható**.
6. A **Csapattagok** oldalon látja a csapat tagjainak listáját (név). Ebben a
   változásban a lista csak olvasható.
7. Egy kiküldött űrlap részleteinél látja a címzettek listáját, mindegyiknél az
   állapotot (`Kitöltendő` / `Beküldve`) és a beküldés idejét.
8. A beküldött kitöltéseket **név szerint** látja: ki, mikor, melyik kérdésre mit
   válaszolt.

### Fő folyamat – kolléga

1. A kolléga a kezdőoldalon kiválasztja magát a listából.
2. Az **Értékeléseim** listán látja a neki küldött űrlapokat: fő téma, értékelt személy
   neve, állapot (`Kitöltendő` / `Beküldve`), beküldött kitöltésnél a beküldés ideje.
3. Megnyit egy `Kitöltendő` űrlapot, és látja a fő témát, az értékelt személy nevét és a
   kérdéseket a megadott sorrendben, a kötelezőket jelölve.
4. Kitölti a kérdéseket (skálánál egy értéket választ a tartományból, szöveges
   kérdésnél szöveget ír), és **beküldi**.
5. A kitöltés állapota `Beküldve` lesz. Az admin felületén is azonnal így látszik.
6. Beküldött kitöltést nem lehet újra megnyitni szerkesztésre vagy újra beküldeni.

### Láthatósági szabályok

- Egy kolléga csak a **saját** kitöltéseit látja (téma, értékelt neve, kérdések,
  állapot). Más kitöltését, más válaszait soha nem kapja meg — sem a felületen, sem az
  API-ban.
- Az értékelt személy nem látja, hogy róla űrlap készült, és nem látja a róla adott
  válaszokat.
- A kolléga a saját beküldött válaszait sem kapja vissza; csak az állapotot és a
  beküldés idejét látja (lásd Nyitott kérdések 4.).
- Az admin-funkciók csak admin belépéssel érhetők el.

### Hibaesetek

- **Hibás admin e-mail vagy jelszó:** a belépés sikertelen, egy általános hibaüzenet
  jelenik meg („Hibás e-mail cím vagy jelszó”), amely nem árulja el, melyik volt hibás.
- **Kolléga belépés nem létező felhasználóval:** sikertelen, hibaüzenet.
- **Belépés nélküli vagy lejárt / érvénytelen access tokennel érkező hívás:** `401`.
  A felület egyszer megpróbálja frissíteni a tokent; ha nem sikerül, a kezdőoldalra visz.
- **Lejárt, érvénytelen vagy már felhasznált refresh token a `/refresh` hívásban:**
  `401`. (A `/logout` ettől eltér: ott az ismeretlen vagy már érvénytelen refresh token
  is `204`-et ad, mert a kilépés célja — a munkamenet megszűnése — enélkül is teljesül.)
- **Kolléga admin-funkciót hív:** elutasítva (`403`).
- **Kolléga más kitöltését kéri vagy küldi be:** úgy viselkedik, mintha nem létezne
  (`404`), hogy ne derüljön ki, létezik-e.
- **Űrlap mentése hiányos adatokkal:** üres fő téma, hiányzó vagy nem létező értékelt
  személy, üres kérdésszöveg, ismeretlen kérdéstípus, skálánál hiányzó határ vagy
  `legkisebb ≥ legnagyobb` → mentés elutasítva, mezőnkénti hibaüzenet.
- **Kiküldött űrlap módosítása:** elutasítva (`409`), „A kiküldött űrlap nem
  módosítható”.
- **Kiküldött űrlap újbóli kiküldése:** elutasítva (`409`).
- **Kiküldött űrlap törlése:** elutasítva (`409`), „A kiküldött űrlap nem törölhető”; a
  felületen kiküldött űrlapnál nincs Törlés gomb.
- **Kiküldés kérdés nélkül:** elutasítva, „Legalább egy kérdés szükséges”.
- **Kiküldés címzett nélkül** (a csapatban az értékelt személyen kívül nincs más):
  elutasítva, „Nincs címzett”.
- **Beküldés hiányzó kötelező válasszal:** elutasítva, a hiányzó kérdések jelölve; a
  kitöltés `Kitöltendő` marad.
- **Skálaérték a tartományon kívül, vagy nem egész szám:** elutasítva.
- **Válasz olyan kérdésre, amely nem az adott űrlaphoz tartozik, vagy a típusának nem
  megfelelő érték** (skálára szöveg, szövegesre szám): elutasítva.
- **Már beküldött kitöltés újbóli beküldése:** elutasítva (`409`), „Már beküldted”.
- **Nem létező űrlap:** `404`.

## API

Minden végpont JSON-t fogad és ad. A hívót az `Authorization: Bearer <access token>`
fejléc azonosítja; süti nincs. A hibák egységes formában jönnek
(RFC 9457 Problem Details); validációs hibánál mezőnkénti hibalistával.

### Belépés

| Metódus | Út | Kérés | Válasz | Hibák |
|---|---|---|---|---|
| `GET` | `/api/auth/team-members` | – | `200` + `TeamMember[]` | – |
| `POST` | `/api/auth/admin/login` | `{ email, password }` | `200` + `AuthTokens` | `400`, `401` |
| `POST` | `/api/auth/member/login` | `{ memberId }` | `200` + `AuthTokens` | `400`, `404` |
| `POST` | `/api/auth/refresh` | `{ refreshToken }` | `200` + `AuthTokens` (új pár) | `400`, `401` |
| `POST` | `/api/auth/logout` | `{ refreshToken }` | `204` | `400` |
| `GET` | `/api/auth/me` | Bearer | `200` + `Me` | `401` |

### Admin

Mind admin belépést igényel: belépés nélkül `401`, kollégaként `403`.

| Metódus | Út | Kérés | Válasz | Hibák |
|---|---|---|---|---|
| `GET` | `/api/admin/team-members` | – | `200` + `TeamMember[]` | `401`, `403` |
| `GET` | `/api/admin/forms` | – | `200` + `FormSummary[]` | `401`, `403` |
| `POST` | `/api/admin/forms` | `FormInput` | `201` + `FormDetail` | `400`, `401`, `403` |
| `GET` | `/api/admin/forms/{formId}` | – | `200` + `FormDetail` | `401`, `403`, `404` |
| `PUT` | `/api/admin/forms/{formId}` | `FormInput` | `200` + `FormDetail` | `400`, `401`, `403`, `404`, `409` |
| `DELETE` | `/api/admin/forms/{formId}` | – | `204` | `401`, `403`, `404`, `409` |
| `POST` | `/api/admin/forms/{formId}/send` | – | `200` + `FormDetail` | `401`, `403`, `404`, `409`, `422` |
| `GET` | `/api/admin/forms/{formId}/responses` | – | `200` + `FormResponse[]` | `401`, `403`, `404` |

### Kolléga

Mind kolléga belépést igényel: belépés nélkül `401`, adminként `403`.

| Metódus | Út | Kérés | Válasz | Hibák |
|---|---|---|---|---|
| `GET` | `/api/me/reviews` | – | `200` + `ReviewSummary[]` | `401`, `403` |
| `GET` | `/api/me/reviews/{reviewId}` | – | `200` + `ReviewDetail` | `401`, `403`, `404` |
| `POST` | `/api/me/reviews/{reviewId}/submit` | `SubmitInput` | `200` + `ReviewSummary` | `400`, `401`, `403`, `404`, `409` |

### Sémák

```jsonc
// TeamMember
{ "id": 3, "name": "Teszt Elek" }

// Me
{ "role": "ADMIN" | "MEMBER", "memberId": 3 /* csak MEMBER */, "name": "Teszt Elek" }

// AuthTokens
{
  "accessToken": "…", "accessTokenExpiresIn": 900,
  "refreshToken": "…", "refreshTokenExpiresIn": 28800,
  "me": { "role": "MEMBER", "memberId": 3, "name": "Teszt Elek" }
}

// FormInput
{
  "topic": "Csapatmunka Q4",
  "evaluatedMemberId": 3,
  "questions": [
    { "text": "Mennyire segítőkész?", "type": "SCALE", "required": true, "scaleMin": 1, "scaleMax": 5 },
    { "text": "Miben fejlődhetne?", "type": "TEXT", "required": false }
  ]
}

// FormSummary
{
  "id": 7, "topic": "Csapatmunka Q4",
  "evaluatedMember": { "id": 3, "name": "Teszt Elek" },
  "status": "DRAFT" | "SENT",
  "createdAt": "2026-10-27T10:00:00Z", "sentAt": null,
  "recipientCount": 0, "submittedCount": 0
}

// FormDetail = FormSummary +
{
  "questions": [ { "id": 11, "position": 1, "text": "...", "type": "SCALE", "required": true, "scaleMin": 1, "scaleMax": 5 } ],
  "recipients": [ { "member": { "id": 4, "name": "..." }, "status": "PENDING" | "SUBMITTED", "submittedAt": null } ]
}

// FormResponse (csak beküldött kitöltések)
{
  "member": { "id": 4, "name": "..." }, "submittedAt": "2026-10-27T12:00:00Z",
  "answers": [ { "questionId": 11, "scaleValue": 4, "textValue": null } ]
}

// ReviewSummary
{
  "id": 21, "formId": 7, "topic": "Csapatmunka Q4",
  "evaluatedMember": { "id": 3, "name": "Teszt Elek" },
  "status": "PENDING" | "SUBMITTED", "submittedAt": null
}

// ReviewDetail = ReviewSummary + { "questions": [ ...a FormDetail kérdései... ] }

// SubmitInput
{ "answers": [ { "questionId": 11, "scaleValue": 4 }, { "questionId": 12, "textValue": "..." } ] }
```

- Opcionális kérdésre a válasz elhagyható. Kötelező szöveges kérdésre csak
  nem üres (nem csak szóközökből álló) szöveg fogadható el.
- A `send` `422`-t ad, ha nincs kérdés vagy nincs címzett; `409`-et, ha már kiküldték.

## Adatmodell

Új táblák (első Flyway migráció). Időpontok ISO-8601 UTC szövegként (SQLite).

| Tábla | Oszlop | Típus | Kötelező | Megjegyzés |
|---|---|---|---|---|
| `team_member` | `id` | INTEGER | igen | elsődleges kulcs |
| | `name` | TEXT | igen | megjelenített név (kitalált tesztadat) |
| `admin_account` | `id` | INTEGER | igen | elsődleges kulcs |
| | `email` | TEXT | igen | egyedi |
| | `password_hash` | TEXT | igen | jelszó hash-e, soha nem nyílt szöveg |
| `auth_session` | `id` | INTEGER | igen | elsődleges kulcs; egy belépés |
| | `role` | TEXT | igen | `ADMIN` / `MEMBER` |
| | `member_id` | INTEGER | nem | → `team_member.id`; csak `MEMBER`-nél |
| | `access_token_hash` | TEXT | igen | egyedi; az access token hash-e |
| | `access_expires_at` | TEXT | igen | |
| | `refresh_token_hash` | TEXT | igen | egyedi; frissítéskor cserélődik |
| | `refresh_expires_at` | TEXT | igen | |
| | `revoked_at` | TEXT | nem | kilépéskor töltődik |
| | `created_at` | TEXT | igen | |
| `review_form` | `id` | INTEGER | igen | elsődleges kulcs |
| | `topic` | TEXT | igen | fő téma |
| | `evaluated_member_id` | INTEGER | igen | → `team_member.id` |
| | `status` | TEXT | igen | `DRAFT` / `SENT` |
| | `created_at` | TEXT | igen | |
| | `sent_at` | TEXT | nem | kiküldéskor töltődik |
| `form_question` | `id` | INTEGER | igen | elsődleges kulcs |
| | `form_id` | INTEGER | igen | → `review_form.id` |
| | `position` | INTEGER | igen | sorrend az űrlapon belül, egyedi űrlaponként |
| | `text` | TEXT | igen | |
| | `type` | TEXT | igen | `SCALE` / `TEXT` |
| | `required` | INTEGER | igen | 0 / 1 |
| | `scale_min` | INTEGER | nem | csak `SCALE`-nél, kötelező ott |
| | `scale_max` | INTEGER | nem | csak `SCALE`-nél, kötelező ott, `> scale_min` |
| `review_assignment` | `id` | INTEGER | igen | elsődleges kulcs (ez a kolléga „kitöltése”) |
| | `form_id` | INTEGER | igen | → `review_form.id` |
| | `recipient_member_id` | INTEGER | igen | → `team_member.id`; ≠ az űrlap értékeltje |
| | `status` | TEXT | igen | `PENDING` / `SUBMITTED` |
| | `submitted_at` | TEXT | nem | beküldéskor töltődik |
| `answer` | `id` | INTEGER | igen | elsődleges kulcs |
| | `assignment_id` | INTEGER | igen | → `review_assignment.id` |
| | `question_id` | INTEGER | igen | → `form_question.id` |
| | `scale_value` | INTEGER | nem | `SCALE` kérdésnél |
| | `text_value` | TEXT | nem | `TEXT` kérdésnél |

- `review_assignment`: egyedi (`form_id`, `recipient_member_id`).
- `answer`: egyedi (`assignment_id`, `question_id`).
- A csapattagok kitalált listája és az admin fiók előre feltöltött adat. Valódi név,
  e-mail vagy jelszó nem kerülhet a repóba.

## Elfogadási kritériumok

**Belépés**

1. **Adott** a kitalált csapattaglista, **amikor** belépés nélkül lekérem a
   csapattagokat, **akkor** megkapom a listát (id + név), és az admin nincs benne.
2. **Adott** az előre beállított admin fiók, **amikor** helyes e-mail címmel és jelszóval
   lépek be, **akkor** a belépés sikeres, és a `me` válaszban `ADMIN` szerepkör áll.
3. **Adott** a környezeti változókban megadott admin e-mail és jelszó, **amikor** a
   backend elindul, **akkor** ezekkel az admin be tud lépni; ha a jelszót a változóban
   megváltoztatom és újraindítom, a régi jelszóval a belépés `401`.
4. **Adott** az admin fiók, **amikor** hibás jelszóval vagy nem létező e-mail címmel
   lépek be, **akkor** `401`-et kapok, mindkét esetben ugyanazzal az üzenettel.
5. **Adott** egy létező csapattag, **amikor** kollégaként belépek vele, **akkor** a
   belépés sikeres, és a `me` válaszban `MEMBER` szerepkör és az ő azonosítója áll.
6. **Adott** egy nem létező csapattag-azonosító, **amikor** kollégaként belépek vele,
   **akkor** `404`-et kapok.
7. **Adott** egy belépett felhasználó, **amikor** kilépek, **akkor** a régi access
   tokennel a következő védett hívás `401`-et ad, és a régi refresh tokennel a frissítés
   is `401`.
8. **Adott** egy sikeres belépés, **amikor** megnézem a választ, **akkor** van benne
   access token (900 mp) és refresh token (28 800 mp), és a válasz nem állít be sütit.
9. **Adott** egy érvényes refresh token, **amikor** frissítek, **akkor** új tokenpárt
   kapok, az új access tokennel a védett hívás `200`, és a régi refresh tokennel egy
   újabb frissítés `401`.
10. **Adott** egy lejárt access token, **amikor** védett végpontot hívok vele, **akkor**
   `401`-et kapok.
11. **Adott** egy lejárt refresh token, **amikor** frissítek vele, **akkor** `401`-et
   kapok.
12. **Adott** a tokenek tárolása, **amikor** megnézem az `auth_session` táblát, **akkor**
   a tokenek nem nyílt szövegként szerepelnek.
13. **Adott** a jelszó tárolása, **amikor** megnézem az `admin_account` táblát, **akkor**
   a jelszó nem nyílt szövegként szerepel.

**Jogosultság**

14. **Adott** belépés nélküli állapot, **amikor** bármely `/api/admin/**` vagy
   `/api/me/**` végpontot hívom, **akkor** `401`-et kapok.
15. **Adott** egy kollégaként belépett felhasználó, **amikor** bármely `/api/admin/**`
   végpontot hívom, **akkor** `403`-at kapok.
16. **Adott** egy adminként belépett felhasználó, **amikor** bármely `/api/me/**`
    végpontot hívom, **akkor** `403`-at kapok.

**Űrlap összeállítása (admin)**

17. **Adott** admin belépés, **amikor** érvényes fő témával, értékelt személlyel, egy
    skálás (1–5, kötelező) és egy szöveges (opcionális) kérdéssel űrlapot hozok létre,
    **akkor** `201`-et kapok, az űrlap `DRAFT` állapotú, és a kérdések a megadott
    sorrendben, a megadott beállításokkal szerepelnek.
18. **Adott** admin belépés, **amikor** üres fő témával, nem létező értékelt személlyel,
    üres kérdésszöveggel, ismeretlen típussal, hiányzó skálahatárral vagy
    `scaleMin ≥ scaleMax` értékkel mentek, **akkor** `400`-at kapok mezőnkénti hibával,
    és nem jön létre űrlap.
19. **Adott** egy `DRAFT` űrlap, **amikor** módosítom a témáját és a kérdéseit, **akkor**
    `200`-at kapok, és a lekért űrlap az új tartalmat mutatja.
20. **Adott** admin belépés és több űrlap, **amikor** lekérem az űrlapok listáját,
    **akkor** mindegyiknél látszik a téma, az értékelt személy, az állapot, a címzettek
    és a beküldöttek száma.

21. **Adott** egy `DRAFT` űrlap kérdésekkel, **amikor** törlöm, **akkor** `204`-et
    kapok, az űrlap nem szerepel a listában, és a lekérése `404`.
22. **Adott** egy `SENT` űrlap, **amikor** törölni próbálom, **akkor** `409`-et kapok, és
    az űrlap, a kitöltései és a válaszai változatlanok.

**Kiküldés**

23. **Adott** egy `DRAFT` űrlap legalább egy kérdéssel és egy N tagú csapat, **amikor**
    kiküldöm, **akkor** az űrlap `SENT` lesz, kitöltve a `sentAt`, és pontosan N−1
    `PENDING` kitöltés jön létre: minden csapattagnak egy, az értékelt személynek nem.
24. **Adott** egy `SENT` űrlap, **amikor** módosítani próbálom, **akkor** `409`-et kapok,
    és az űrlap változatlan.
25. **Adott** egy `SENT` űrlap, **amikor** újra kiküldöm, **akkor** `409`-et kapok, és
    nem jön létre újabb kitöltés.
26. **Adott** egy kérdés nélküli `DRAFT` űrlap, **amikor** kiküldöm, **akkor** `422`-t
    kapok, és az űrlap `DRAFT` marad.
27. **Adott** egy `DRAFT` űrlap egy olyan csapatban, ahol az értékelt személyen kívül
    nincs más tag, **amikor** kiküldöm, **akkor** `422`-t kapok, és az űrlap `DRAFT`
    marad.

**Kitöltés (kolléga)**

28. **Adott** egy kiküldött űrlap, amelynek címzettje vagyok, **amikor** lekérem az
    értékeléseimet, **akkor** szerepel benne `PENDING` állapottal, a témával és az
    értékelt személy nevével.
29. **Adott** egy űrlap, amelynek én vagyok az értékeltje, **amikor** lekérem az
    értékeléseimet, **akkor** nem szerepel benne.
30. **Adott** egy `DRAFT` (még ki nem küldött) űrlap, **amikor** bármely kolléga lekéri
    az értékeléseit, **akkor** nem szerepel benne.
31. **Adott** egy saját `PENDING` kitöltés, **amikor** megnyitom, **akkor** látom a
    kérdéseket sorrendben, a típusukkal, a skálahatárokkal és a kötelezőséggel.
32. **Adott** egy saját `PENDING` kitöltés, **amikor** minden kötelező kérdésre érvényes
    választ adva beküldöm, **akkor** `200`-at kapok, az állapot `SUBMITTED` lesz, és
    kitöltődik a `submittedAt`.
33. **Adott** egy saját `PENDING` kitöltés, **amikor** egy kötelező kérdést kihagyva
    küldöm be, **akkor** `400`-at kapok a hiányzó kérdés megjelölésével, és az állapot
    `PENDING` marad.
34. **Adott** egy 1–5 skálás kérdés, **amikor** 0-t, 6-ot vagy szöveget küldök rá,
    **akkor** `400`-at kapok, és az állapot `PENDING` marad.
35. **Adott** egy kötelező szöveges kérdés, **amikor** üres vagy csak szóközökből álló
    szöveget küldök rá, **akkor** `400`-at kapok.
36. **Adott** egy saját `PENDING` kitöltés, **amikor** egy másik űrlap kérdésére küldök
    választ, **akkor** `400`-at kapok.
37. **Adott** egy opcionális kérdés, **amikor** válasz nélkül küldöm be a kitöltést,
    **akkor** a beküldés sikeres.
38. **Adott** egy saját `SUBMITTED` kitöltés, **amikor** újra beküldöm, **akkor**
    `409`-et kapok, és a korábbi válaszok változatlanok.

**Láthatóság**

39. **Adott** egy másik kolléga kitöltése, **amikor** kollégaként lekérem vagy
    beküldöm, **akkor** `404`-et kapok.
40. **Adott** egy saját `SUBMITTED` kitöltés, **amikor** lekérem az értékeléseimet vagy a
    kitöltést, **akkor** a válaszban nincsenek benne a válaszok — sem az enyémek, sem
    másokéi.
41. **Adott** egy kiküldött űrlap, ahol egy címzett beküldött, egy másik nem, **amikor**
    adminként lekérem a részleteit, **akkor** a beküldő `SUBMITTED` állapottal és
    időponttal, a másik `PENDING` állapottal szerepel, és a lista `submittedCount`
    értéke 1.
42. **Adott** egy kiküldött űrlap beküldött kitöltésekkel, **amikor** adminként lekérem
    a válaszokat, **akkor** minden beküldött kitöltésnél látom a beküldő nevét, idejét és
    kérdésenkénti válaszait; a `PENDING` kitöltések nem szerepelnek.

**Felület**

43. **Adott** a kezdőoldal, **amikor** megnyitom, **akkor** látom az admin belépést
    (e-mail, jelszó) és a kolléga választót.
44. **Adott** egy kollégaként belépett felhasználó, **amikor** a felületen kitölt és
    beküld egy űrlapot, **akkor** az Értékeléseim listán `Beküldve` állapot jelenik
    meg, és az űrlap nem nyitható meg újra kitöltésre.
45. **Adott** egy adminként belépett felhasználó, **amikor** a felületen létrehoz,
    kiküld egy űrlapot, és egy kolléga beküldi, **akkor** az űrlap részleteinél a
    kolléga `Beküldve` állapottal és a válaszaival látszik.

**Navigáció (felület)**

46. **Adott** egy adminként belépett felhasználó, **amikor** megjelenik a navigáció,
    **akkor** az Űrlapok, Új űrlap és Csapattagok menüpontot és a Kilépés gombot látja,
    az Értékeléseim menüpontot nem.
47. **Adott** egy kollégaként belépett felhasználó, **amikor** megjelenik a navigáció,
    **akkor** az Értékeléseim menüpontot és a Kilépés gombot látja, admin menüpontot nem.
48. **Adott** belépés nélküli állapot, **amikor** a `/admin/forms` vagy a `/reviews`
    címet nyitom meg, **akkor** a `/login` oldalra kerülök.
49. **Adott** egy kollégaként belépett felhasználó, **amikor** a `/admin/forms` címet
    nyitom meg, **akkor** az Értékeléseim oldalra kerülök; adminként a `/reviews` cím az
    Űrlapok oldalra visz.
50. **Adott** egy sikeres belépés, **amikor** a rendszer továbbvisz, **akkor** admin az
    Űrlapok, kolléga az Értékeléseim oldalra kerül.
51. **Adott** egy belépett felhasználó lejárt access tokennel és érvényes refresh
    tokennel, **amikor** a felületen műveletet végez, **akkor** a művelet újra-belépés
    nélkül sikerül.
52. **Adott** egy belépett felhasználó, **amikor** a Kilépésre kattint, **akkor** a
    sessionStorage-ban nem marad token, és a `/login` oldalra kerül.
53. **Adott** egy adminként belépett felhasználó, **amikor** a Csapattagok oldalt
    megnyitja, **akkor** látja a csapattagok nevét.

## Nincs benne (out of scope)

- A felhasználólista lekérése a Jirából (`0002`); itt fix, kitalált lista van.
- Valódi bejelentkezés a kollégáknak (jelszó, SSO). **Következmény:** bárki beléphet
  bármelyik kollégaként; ezt az intent elfogadja.
- Admin regisztráció, több admin, jelszócsere, elfelejtett jelszó.
- Belépési próbálkozások korlátozása (rate limiting), fiókzárolás.
- Csapattagok szerkesztése az admin felületen (a lista itt csak olvasható).
- **Kockázat, amit ez a spec elfogad:** a sessionStorage-ban lévő tokent egy esetleges
  XSS-hiba ki tudná olvasni. Ellene: az Angular beépített kimenet-escape-elése, `innerHTML`
  nélkül; tartalombiztonsági szabályzat (CSP) később.
- Határidő, emlékeztető, értesítés (e-mail stb.).
- Beküldött kitöltés módosítása, visszavonása; részleges mentés (piszkozat válasz).
- Űrlap másolása, sablonok, újrakiküldés.
- Összesítő kimutatás (átlag, grafikon) az értékelésekről.
- Jira csapat-dashboard (`0003`).

## Nyitott kérdések

Nincs. Eldöntve (2026-10-27):

1. A piszkozat törölhető, a kiküldött űrlap nem.
2. A skála határai tetszőleges egészek, `min < max`.
3. A szöveghosszak felső korlátját a plan javasolja.
4. A kolléga a saját beküldött válaszait nem kapja vissza, csak az állapotot és az időt.
5. A címzettek köre a kiküldés pillanatában rögzül.
6. Access token 15 perc, refresh token 8 óra.
7. Az admin e-mail címe és jelszava környezeti változóból jön.
