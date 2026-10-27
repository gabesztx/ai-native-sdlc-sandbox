---
name: review
description: Átnézi egy változás diffjét párhuzamosan futó specialista reviewer subagentekkel (security, teszt, spec-megfelelés), és a REVIEW.md szerint összesíti az eredményt — az SDLC lánc utolsó lépése. Akkor használd, ha a felhasználó review-t kér egy branchre vagy változásra, vagy a /review parancsot adja ki.
---

# Review

Ebben a skillben a fő session az **orchestrator**: nem maga reviewzik, hanem
specialista subagenteket indít párhuzamosan, és összefésüli az eredményüket.
Semmit nem szerkeszt, csak jelent.

## 1. Hatókör meghatározása

- **Diff tartomány:** ha a felhasználó megadta (`/review <tartomány>`), azt használd.
  Különben feature branchen `main...HEAD`. Ha a `main`-en állsz, kérdezd meg, melyik
  commitokat nézzük át (pl. `HEAD~3..HEAD`).
- Futtasd: `git diff <tartomány> --stat`, hogy lásd, mely fájlok változtak.
- **Változás mappa:** keresd meg a hozzá tartozó `docs/changes/<id>-<slug>/` mappát
  (a branch neve, a commit üzenetek vagy a felhasználó alapján). Ha nincs, a
  spec-megfelelés reviewer ezt jelzi; ne találj ki mappát.

## 2. Reviewerek indítása — párhuzamosan

**Egyetlen üzenetben** indítsd el ezt a három subagentet, hogy egyszerre fussanak:

| Subagent | Fókusz |
|---|---|
| `security-reviewer` | injection, validáció, secretek, személyes adat |
| `test-reviewer` | minden elfogadási kritériumhoz van-e teszt, és el tud-e bukni |
| `spec-compliance-reviewer` | a diff pontosan a `plan.md`-t valósítja-e meg, se többet, se kevesebbet |

Mindegyiknek add át:
- a diff tartományát,
- a változás mappa útvonalát (`intent.md`, `spec.md`, `plan.md`),
- hogy a `REVIEW.md` szerint, annak formátumában jelentsen.

Ha egy subagent nem létezik még a `.claude/agents/` mappában, azt a fókuszt te magad
nézd át, és az összesítésben jelezd, hogy nem külön agent végezte.

## 3. Összesítés a REVIEW.md szerint

1. Gyűjtsd össze a találatokat, és **szűrd a duplikátumokat** (ugyanaz a fájl:sor és
   ugyanaz a probléma → egy találat).
2. Minden Important találatnál ellenőrizd, hogy van-e konkrét hibaeset. Ha nincs,
   minősítsd Nit-re, vagy dobd el.
3. Nitekből legfeljebb 5 marad, a többi csak darabszámként.
4. Kimenet a `REVIEW.md` „Review kimenet” része szerint: verdikt, Important
   találatok, nitek, mit néztünk át — és melyik subagent mit talált.

## Ne csináld

- Ne javítsd a talált hibákat. Ha a felhasználó kéri, az egy külön lépés.
- Ne reviewzd azt a kódot, amit ebben a sessionben te írtál — ilyenkor szólj, és
  javasold, hogy friss session vagy a subagentek végezzék.
- Ne indítsd a reviewereket egymás után; párhuzamosan fussanak.
