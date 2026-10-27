# Intent: Felhasználólista lekérése a Jirából

- **ID:** 0002-jira-user-import
- **Státusz:** Approved
- **Dátum:** 2026-10-27

## Probléma

A `0001` változásban a felhasználólista fix, kitalált felhasználókból áll. A valódi
használathoz a HUGO csapat tényleges tagjai kellenek, akiket ma kézzel kellene felvinni.

## Kívánt eredmény

- A rendszer a felhasználók listáját **egyszer, az adatbázis létrehozásakor** lekéri a
  „HUGO - Board” (scrum) **aktív sprintjéből**, és elmenti. A csapat utána nem változik.
- Egy felhasználóról minden olyan adatot elment, ami a későbbi bővítéshez (pl. avatar a
  dashboardon) kellhet.
- A már beküldött kitöltések megmaradnak, az admin később is visszanézheti őket.
- A felhasználóválasztóban és az értékelés címzettjei között ez a lista jelenik meg.

**Sikeres, ha:** a lekérés után a felhasználóválasztóban az aktív sprint résztvevői
látszanak, és nekik ki lehet küldeni értékelő űrlapot.

## Érintettek

- **Felhasználók:** admin és kolléga (a választólista tartalma változik).
- **Rendszerrészek:** API, adat, Jira API (csak olvasás).

## Korlátok

- A Jirát a rendszer csak olvassa.
- A Jira-token és a lekért valódi adatok nem kerülhetnek a publikus repóba.

## Nincs benne (out of scope)

- Folyamatos, automatikus szinkronizálás, a csapat későbbi változásának követése.
- Valódi bejelentkezés Jira-fiókkal.
- Bármilyen Jira-adat a felhasználólistán kívül.

## Nyitott kérdések

- Nincs. (A kitalált felhasználók csak fejlesztői és tesztadatok; az éles adatbázisba
  nem kerülnek, így nem keverednek a valódi csapattal.)
