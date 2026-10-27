# Intent: Felhasználólista lekérése a Jirából

- **ID:** 0002-jira-user-import
- **Státusz:** Draft
- **Dátum:** 2026-10-27

## Probléma

A `0001` változásban a felhasználólista fix, kitalált felhasználókból áll. A valódi
használathoz a HUGO csapat tényleges tagjai kellenek, akiket ma kézzel kellene felvinni.

## Kívánt eredmény

- A rendszer a felhasználók listáját **egyszer lekéri** a „HUGO - Board” (scrum)
  **aktív sprintjéből**, és elmenti.
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

- Folyamatos, automatikus szinkronizálás.
- Valódi bejelentkezés Jira-fiókkal.
- Bármilyen Jira-adat a felhasználólistán kívül.

## Nyitott kérdések

- **Mi történik, ha a sprint csapata megváltozik?** Újra le lehet kérni, vagy a lista
  utána kézzel bővíthető?
- **Ki indítja a lekérést?** Az admin a felületen, vagy egyszeri beállítási lépés?
- **Mi kell egy felhasználóról?** Csak név, vagy e-mail, avatar is?
- **Mi történik a `0001` kitalált felhasználóival és az addigi kitöltéseikkel?**
