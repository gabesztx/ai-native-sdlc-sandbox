# Spec: Kollégák értékelése űrlapokkal

- **ID:** 0001-peer-review-forms
- **Státusz:** Draft
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
- **Admin:** egyetlen, előre beállított fiók (e-mail + jelszó). Nem csapattag: nem
  szerepel a felhasználólistában, nem értékelhető, nem kap űrlapot.
- A kezdőoldal a belépés. Két út van rajta:
  1. **Admin belépés:** e-mail cím + jelszó.
  2. **Kolléga belépés:** a csapattagok listájából választás, jelszó nélkül.
- Belépés után a rendszer a szerepkörnek megfelelő felületre visz. Kilépéssel a
  kezdőoldalra kerül a felhasználó.
- Belépés nélkül csak a kezdőoldal és a csapattagok listája (a választóhoz) érhető el.

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
4. Mentéskor az űrlap **`Piszkozat`** állapotú. Piszkozatként szabadon módosítható.
5. Az admin **kiküldi** az űrlapot:
   - a címzettek a csapat összes tagja, **az értékelt személy kivételével**;
   - minden címzetthez létrejön egy **kitöltés** `Kitöltendő` állapottal;
   - az űrlap állapota `Kiküldve` lesz, és onnantól **nem módosítható**.
6. Egy kiküldött űrlap részleteinél látja a címzettek listáját, mindegyiknél az
   állapotot (`Kitöltendő` / `Beküldve`) és a beküldés idejét.
7. A beküldött kitöltéseket **név szerint** látja: ki, mikor, melyik kérdésre mit
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
- **Belépés nélküli hívás védett funkcióra:** elutasítva (`401`), a felület a
  kezdőoldalra visz.
- **Kolléga admin-funkciót hív:** elutasítva (`403`).
- **Kolléga más kitöltését kéri vagy küldi be:** úgy viselkedik, mintha nem létezne
  (`404`), hogy ne derüljön ki, létezik-e.
- **Űrlap mentése hiányos adatokkal:** üres fő téma, hiányzó vagy nem létező értékelt
  személy, üres kérdésszöveg, ismeretlen kérdéstípus, skálánál hiányzó határ vagy
  `legkisebb ≥ legnagyobb` → mentés elutasítva, mezőnkénti hibaüzenet.
- **Kiküldött űrlap módosítása:** elutasítva (`409`), „A kiküldött űrlap nem
  módosítható”.
- **Kiküldött űrlap újbóli kiküldése:** elutasítva (`409`).
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

Minden végpont JSON-t fogad és ad. A hívó azonosítása a belépéskor létrejött
munkamenet alapján történik (lásd Nyitott kérdések 6.). A hibák egységes formában jönnek
(RFC 9457 Problem Details); validációs hibánál mezőnkénti hibalistával.

### Belépés

| Metódus | Út | Kérés | Válasz | Hibák |
|---|---|---|---|---|
| `GET` | `/api/auth/team-members` | – | `200` + `TeamMember[]` | – |
| `POST` | `/api/auth/admin/login` | `{ email, password }` | `200` + `Me` | `400`, `401` |
| `POST` | `/api/auth/member/login` | `{ memberId }` | `200` + `Me` | `400`, `404` |
| `POST` | `/api/auth/logout` | – | `204` | – |
| `GET` | `/api/auth/me` | – | `200` + `Me` | `401` |

### Admin

Mind admin belépést igényel: belépés nélkül `401`, kollégaként `403`.

| Metódus | Út | Kérés | Válasz | Hibák |
|---|---|---|---|---|
| `GET` | `/api/admin/team-members` | – | `200` + `TeamMember[]` | `401`, `403` |
| `GET` | `/api/admin/forms` | – | `200` + `FormSummary[]` | `401`, `403` |
| `POST` | `/api/admin/forms` | `FormInput` | `201` + `FormDetail` | `400`, `401`, `403` |
| `GET` | `/api/admin/forms/{formId}` | – | `200` + `FormDetail` | `401`, `403`, `404` |
| `PUT` | `/api/admin/forms/{formId}` | `FormInput` | `200` + `FormDetail` | `400`, `401`, `403`, `404`, `409` |
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
  "id": 21, "topic": "Csapatmunka Q4",
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
3. **Adott** az admin fiók, **amikor** hibás jelszóval vagy nem létező e-mail címmel
   lépek be, **akkor** `401`-et kapok, mindkét esetben ugyanazzal az üzenettel.
4. **Adott** egy létező csapattag, **amikor** kollégaként belépek vele, **akkor** a
   belépés sikeres, és a `me` válaszban `MEMBER` szerepkör és az ő azonosítója áll.
5. **Adott** egy nem létező csapattag-azonosító, **amikor** kollégaként belépek vele,
   **akkor** `404`-et kapok.
6. **Adott** egy belépett felhasználó, **amikor** kilépek, **akkor** a következő védett
   hívás `401`-et ad.
7. **Adott** a jelszó tárolása, **amikor** megnézem az `admin_account` táblát, **akkor**
   a jelszó nem nyílt szövegként szerepel.

**Jogosultság**

8. **Adott** belépés nélküli állapot, **amikor** bármely `/api/admin/**` vagy
   `/api/me/**` végpontot hívom, **akkor** `401`-et kapok.
9. **Adott** egy kollégaként belépett felhasználó, **amikor** bármely `/api/admin/**`
   végpontot hívom, **akkor** `403`-at kapok.
10. **Adott** egy adminként belépett felhasználó, **amikor** bármely `/api/me/**`
    végpontot hívom, **akkor** `403`-at kapok.

**Űrlap összeállítása (admin)**

11. **Adott** admin belépés, **amikor** érvényes fő témával, értékelt személlyel, egy
    skálás (1–5, kötelező) és egy szöveges (opcionális) kérdéssel űrlapot hozok létre,
    **akkor** `201`-et kapok, az űrlap `DRAFT` állapotú, és a kérdések a megadott
    sorrendben, a megadott beállításokkal szerepelnek.
12. **Adott** admin belépés, **amikor** üres fő témával, nem létező értékelt személlyel,
    üres kérdésszöveggel, ismeretlen típussal, hiányzó skálahatárral vagy
    `scaleMin ≥ scaleMax` értékkel mentek, **akkor** `400`-at kapok mezőnkénti hibával,
    és nem jön létre űrlap.
13. **Adott** egy `DRAFT` űrlap, **amikor** módosítom a témáját és a kérdéseit, **akkor**
    `200`-at kapok, és a lekért űrlap az új tartalmat mutatja.
14. **Adott** admin belépés és több űrlap, **amikor** lekérem az űrlapok listáját,
    **akkor** mindegyiknél látszik a téma, az értékelt személy, az állapot, a címzettek
    és a beküldöttek száma.

**Kiküldés**

15. **Adott** egy `DRAFT` űrlap legalább egy kérdéssel és egy N tagú csapat, **amikor**
    kiküldöm, **akkor** az űrlap `SENT` lesz, kitöltve a `sentAt`, és pontosan N−1
    `PENDING` kitöltés jön létre: minden csapattagnak egy, az értékelt személynek nem.
16. **Adott** egy `SENT` űrlap, **amikor** módosítani próbálom, **akkor** `409`-et kapok,
    és az űrlap változatlan.
17. **Adott** egy `SENT` űrlap, **amikor** újra kiküldöm, **akkor** `409`-et kapok, és
    nem jön létre újabb kitöltés.
18. **Adott** egy kérdés nélküli `DRAFT` űrlap, **amikor** kiküldöm, **akkor** `422`-t
    kapok, és az űrlap `DRAFT` marad.
19. **Adott** egy `DRAFT` űrlap egy olyan csapatban, ahol az értékelt személyen kívül
    nincs más tag, **amikor** kiküldöm, **akkor** `422`-t kapok, és az űrlap `DRAFT`
    marad.

**Kitöltés (kolléga)**

20. **Adott** egy kiküldött űrlap, amelynek címzettje vagyok, **amikor** lekérem az
    értékeléseimet, **akkor** szerepel benne `PENDING` állapottal, a témával és az
    értékelt személy nevével.
21. **Adott** egy űrlap, amelynek én vagyok az értékeltje, **amikor** lekérem az
    értékeléseimet, **akkor** nem szerepel benne.
22. **Adott** egy `DRAFT` (még ki nem küldött) űrlap, **amikor** bármely kolléga lekéri
    az értékeléseit, **akkor** nem szerepel benne.
23. **Adott** egy saját `PENDING` kitöltés, **amikor** megnyitom, **akkor** látom a
    kérdéseket sorrendben, a típusukkal, a skálahatárokkal és a kötelezőséggel.
24. **Adott** egy saját `PENDING` kitöltés, **amikor** minden kötelező kérdésre érvényes
    választ adva beküldöm, **akkor** `200`-at kapok, az állapot `SUBMITTED` lesz, és
    kitöltődik a `submittedAt`.
25. **Adott** egy saját `PENDING` kitöltés, **amikor** egy kötelező kérdést kihagyva
    küldöm be, **akkor** `400`-at kapok a hiányzó kérdés megjelölésével, és az állapot
    `PENDING` marad.
26. **Adott** egy 1–5 skálás kérdés, **amikor** 0-t, 6-ot vagy szöveget küldök rá,
    **akkor** `400`-at kapok, és az állapot `PENDING` marad.
27. **Adott** egy kötelező szöveges kérdés, **amikor** üres vagy csak szóközökből álló
    szöveget küldök rá, **akkor** `400`-at kapok.
28. **Adott** egy saját `PENDING` kitöltés, **amikor** egy másik űrlap kérdésére küldök
    választ, **akkor** `400`-at kapok.
29. **Adott** egy opcionális kérdés, **amikor** válasz nélkül küldöm be a kitöltést,
    **akkor** a beküldés sikeres.
30. **Adott** egy saját `SUBMITTED` kitöltés, **amikor** újra beküldöm, **akkor**
    `409`-et kapok, és a korábbi válaszok változatlanok.

**Láthatóság**

31. **Adott** egy másik kolléga kitöltése, **amikor** kollégaként lekérem vagy
    beküldöm, **akkor** `404`-et kapok.
32. **Adott** egy saját `SUBMITTED` kitöltés, **amikor** lekérem az értékeléseimet vagy a
    kitöltést, **akkor** a válaszban nincsenek benne a válaszok — sem az enyémek, sem
    másokéi.
33. **Adott** egy kiküldött űrlap, ahol egy címzett beküldött, egy másik nem, **amikor**
    adminként lekérem a részleteit, **akkor** a beküldő `SUBMITTED` állapottal és
    időponttal, a másik `PENDING` állapottal szerepel, és a lista `submittedCount`
    értéke 1.
34. **Adott** egy kiküldött űrlap beküldött kitöltésekkel, **amikor** adminként lekérem
    a válaszokat, **akkor** minden beküldött kitöltésnél látom a beküldő nevét, idejét és
    kérdésenkénti válaszait; a `PENDING` kitöltések nem szerepelnek.

**Felület**

35. **Adott** a kezdőoldal, **amikor** megnyitom, **akkor** látom az admin belépést
    (e-mail, jelszó) és a kolléga választót.
36. **Adott** egy kollégaként belépett felhasználó, **amikor** a felületen kitölt és
    beküld egy űrlapot, **akkor** az Értékeléseim listán `Beküldve` állapot jelenik
    meg, és az űrlap nem nyitható meg újra kitöltésre.
37. **Adott** egy adminként belépett felhasználó, **amikor** a felületen létrehoz,
    kiküld egy űrlapot, és egy kolléga beküldi, **akkor** az űrlap részleteinél a
    kolléga `Beküldve` állapottal és a válaszaival látszik.

## Nincs benne (out of scope)

- A felhasználólista lekérése a Jirából (`0002`); itt fix, kitalált lista van.
- Valódi bejelentkezés a kollégáknak (jelszó, SSO). **Következmény:** bárki beléphet
  bármelyik kollégaként; ezt az intent elfogadja.
- Admin regisztráció, több admin, jelszócsere, elfelejtett jelszó.
- Belépési próbálkozások korlátozása (rate limiting), fiókzárolás.
- Határidő, emlékeztető, értesítés (e-mail stb.).
- Beküldött kitöltés módosítása, visszavonása; részleges mentés (piszkozat válasz).
- Űrlap másolása, sablonok, újrakiküldés.
- Összesítő kimutatás (átlag, grafikon) az értékelésekről.
- Jira csapat-dashboard (`0003`).

## Nyitott kérdések

Ezeket az intent nem dönti el. A spec a zárójelben lévő **feltételezéssel** készült; ha
más a válasz, a spec érintett részét módosítani kell.

1. **Piszkozat törlése:** törölhető-e egy `DRAFT` űrlap? (Feltételezés: nem, ebben a
   változásban nincs törlés.)
2. **Skála határai:** van-e megkötés az admin által megadott skálára (pl. legkisebb ≥ 0,
   legnagyobb ≤ 10)? (Feltételezés: tetszőleges egész, `min < max`.)
3. **Szöveghosszak:** van-e felső korlát a fő témára, a kérdésszövegre és a szöveges
   válaszra? (Feltételezés: nincs megadva; a plan javasoljon értéket.)
4. **Saját válaszok:** visszanézheti-e a kolléga a saját beküldött válaszait?
   (Feltételezés: nem, csak az állapotot és az időt látja.)
5. **Csapatváltozás kiküldés után:** a címzettek köre a kiküldés pillanatában rögzül.
   (Feltételezés: igen; a 0001-ben a lista amúgy is fix.)
6. **Munkamenet:** hogyan azonosítja a backend a belépett felhasználót? (Feltételezés:
   szerveroldali munkamenet HttpOnly sütivel; kolléga és admin ugyanígy.)
7. **Az admin fiók beállítása:** honnan jön az admin e-mail címe és jelszava úgy, hogy ne
   kerüljön a repóba? (A plan dönti el, pl. környezeti változó.)
