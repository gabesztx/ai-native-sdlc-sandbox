# Spec: Felhasználólista lekérése a Jirából

- **ID:** 0002-jira-user-import
- **Státusz:** Draft
- **Intent:** [intent.md](intent.md)
- **Dátum:** 2026-10-27

## Összefoglaló

Az adatbázis létrehozásakor a backend egyszer lekéri a „HUGO - Board” (scrum) aktív
sprintjének résztvevőit a Jira Cloud REST API-jából, és elmenti őket csapattagként. A
felhasználóválasztóban és az értékelések címzettjei között onnantól a valódi HUGO csapat
szerepel. A csapat utána nem változik.

## Viselkedés

### Fő folyamat

1. A backend indul. Ha a `team_member` tábla **üres**, és a Jira-kapcsolat be van
   állítva, elindul az import.
2. A backend megkeresi a „HUGO - Board” táblát és annak **aktív sprintjét**.
3. Lekéri az aktív sprint összes feladatát, és összegyűjti a feladatok **assignee-it**
   (lásd Nyitott kérdések 1.). Minden felhasználó egyszer szerepel.
4. Minden felhasználót elment csapattagként: Jira-azonosító, megjelenített név, e-mail
   (ha a Jira visszaadja), avatar URL, aktív-e, importálás ideje.
5. Az import egyetlen tranzakció: vagy minden felhasználó bekerül, vagy egy sem.
6. Ha a `team_member` tábla **nem üres**, az import nem fut le újra. A csapat így
   változatlan marad, a meglévő űrlapok és kitöltések érintetlenek.
7. Az importált csapattagok megjelennek a kolléga-választóban (`0001`), és ők lesznek az
   űrlapok értékeltjei és címzettjei.

### Fejlesztői és tesztkörnyezet

- Ha a Jira-kapcsolat **nincs beállítva**, az import nem fut; a `0001` kitalált
  csapattagjai töltődnek be. Így az app Jira-hozzáférés nélkül is fejleszthető és
  tesztelhető.
- Kitalált és valódi csapattag soha nem kerül ugyanabba az adatbázisba: az import csak
  üres táblába ír, a kitalált lista pedig csak Jira-beállítás nélkül töltődik be.

### Hibaesetek

- **A Jira nem elérhető, vagy hibás a hitelesítés:** az import megszakad, semmi nem
  mentődik, a backend hibát naplóz (a tokent nem írja a naplóba), és az app elindul üres
  csapattal. A következő induláskor az import újra megpróbálkozik, mert a tábla üres.
- **A tábla nem található** (nincs „HUGO - Board”): mint fent, a napló megnevezi az okot.
- **Nincs aktív sprint:** mint fent.
- **Az aktív sprintben nincs assignee-vel rendelkező feladat:** az import nem ment
  semmit, a napló jelzi.
- **Üres csapattal** a kolléga-választó üres listát mutat, és űrlap nem küldhető ki
  (`0001`: „Nincs címzett”).

## Beállítás

A Jira-kapcsolat adatai **környezeti változókból** jönnek, soha nem a repóból:

| Beállítás | Példa |
|---|---|
| Jira alap-URL | `https://<cég>.atlassian.net` |
| Jira felhasználó e-mail | a token tulajdonosa |
| Jira API token | – |
| Tábla neve | `HUGO - Board` |

A beállítás „be van állítva”, ha az URL, az e-mail és a token is meg van adva.

## API

Új végpont nincs. A meglévő `TeamMember` séma bővül, a többi `0001`-es végpont
változatlan:

```jsonc
// TeamMember
{ "id": 3, "name": "Teszt Elek", "avatarUrl": "https://..." /* lehet null */ }
```

Az e-mail és a Jira-azonosító nem jelenik meg az API-ban.

## Adatmodell

Új Flyway migráció, amely a `team_member` táblát bővíti:

| Tábla | Oszlop | Típus | Kötelező | Megjegyzés |
|---|---|---|---|---|
| `team_member` | `jira_account_id` | TEXT | nem | Jira Cloud `accountId`; egyedi, ha meg van adva; kitalált tagnál üres |
| | `email` | TEXT | nem | a Jira adatvédelmi beállításai miatt hiányozhat |
| | `avatar_url` | TEXT | nem | a Jira 48×48-as avatarjának URL-je |
| | `active` | INTEGER | igen | 0 / 1, alapérték 1; a Jira `active` mezője |
| | `imported_at` | TEXT | nem | az import ideje (ISO-8601 UTC); kitalált tagnál üres |

## Elfogadási kritériumok

A Jira-hívásokat a tesztek egy helyettesítő (mock) Jira-szerverrel váltják ki; valódi
Jira-hívás teszt közben nem történik.

1. **Adott** üres `team_member` tábla és beállított Jira-kapcsolat, és az aktív sprintben
   három különböző assignee, **amikor** a backend elindul, **akkor** pontosan három
   csapattag jön létre a nevükkel, Jira-azonosítójukkal, avatar URL-jükkel és az
   importálás idejével.
2. **Adott** egy aktív sprint, ahol ugyanannak a felhasználónak több feladata van,
   **amikor** az import lefut, **akkor** a felhasználó egyszer szerepel.
3. **Adott** egy aktív sprint assignee nélküli feladatokkal is, **amikor** az import
   lefut, **akkor** ezek a feladatok nem hoznak létre csapattagot.
4. **Adott** egy felhasználó, akinek az e-mailjét a Jira nem adja vissza, **amikor** az
   import lefut, **akkor** a csapattag létrejön üres e-maillel.
5. **Adott** nem üres `team_member` tábla, **amikor** a backend elindul, **akkor** a
   Jira nem kap hívást, és a csapattagok változatlanok.
6. **Adott** üres tábla, és a Jira hibát ad (pl. `401` vagy `500`), **amikor** a backend
   elindul, **akkor** az app elindul, a tábla üres marad, és a napló tartalmazza a hiba
   okát, de nem tartalmazza a tokent.
7. **Adott** üres tábla, és a Jira a sprint-feladatok lekérése közben hibát ad, **amikor**
   az import fut, **akkor** egyetlen csapattag sem mentődik (nincs félkész import).
8. **Adott** üres tábla, és nincs „HUGO - Board” tábla, vagy nincs aktív sprint,
   **amikor** a backend elindul, **akkor** a tábla üres marad, és a napló megnevezi az
   okot.
9. **Adott** üres tábla és **nem** beállított Jira-kapcsolat, **amikor** a backend
   elindul, **akkor** a kitalált csapattagok töltődnek be, és a Jira nem kap hívást.
10. **Adott** egy sikeres import, **amikor** belépés nélkül lekérem a csapattagokat,
    **akkor** az importált tagok szerepelnek a listában `id`, `name` és `avatarUrl`
    mezővel, e-mail és Jira-azonosító nélkül.
11. **Adott** egy sikeres import, **amikor** az admin űrlapot küld ki egy importált tag
    értékelésére, **akkor** a többi importált tag megkapja kitöltésre (`0001` szerint).

## Nincs benne (out of scope)

- A csapat későbbi változásának követése (új tag, kilépő tag), újraimportálás.
- Jira webhook a felhasználókhoz (a csapat nem változik).
- Bejelentkezés Jira-fiókkal.
- Az avatar megjelenítése a felületen (az URL mentődik, a megjelenítés a `0003` 2.
  fázisa).
- Bármilyen írás a Jirába.

## Nyitott kérdések

A spec a zárójelben lévő **feltételezéssel** készült.

1. **Kik az „aktív sprint résztvevői”?** A Jirában nincs külön sprinttagság.
   (Feltételezés: az aktív sprint feladatainak assignee-i. Alternatíva: a reporterek is.)
2. **Inaktív Jira-felhasználó:** ha egy assignee fiókja a Jirában inaktív, bekerüljön-e?
   (Feltételezés: bekerül, `active = 0` jelöléssel, és a választóban sem jelenik meg.)
3. **A token típusa:** személyes Jira API token (e-mail + token, Basic auth), vagy egy
   technikai felhasználó? (Feltételezés: személyes API token, csak helyi környezeti
   változóban.)
