# Intent: Jira csapat-dashboard

- **ID:** 0003-jira-team-dashboard
- **Státusz:** Draft
- **Dátum:** 2026-10-27

## Probléma

A HUGO csapat fejlesztési állapotáról nincs gyors, közös áttekintés. A státusz kiderítése
meetingekkel és kézi riportokkal megy, a kockázatok és az elakadások későn látszanak, és
nem látszik, ki min dolgozik, ki túlterhelt, és kinek kell segítség.

## Kívánt eredmény

Egy közös, modern dashboard, amelyből a csapat és a vezetők gyorsan látják a HUGO
fejlesztés állapotát, a kockázatokat és az elakadásokat. Az adatok a Jirából és a
Tempóból jönnek.

- **A csapatnak:** napi áttekintés a standuphoz — ki min dolgozik, mi akadt el, kinek kell
  segítség.
- **A vezetőknek:** sprint- és projektszintű kép külön riport nélkül — halad-e a terv, hol
  a kockázat, kell-e átcsoportosítani.

A dashboard három fázisban készül el:

### 1. fázis – Sprint-áttekintés
- **KPI-csempék:** összes feladat, kész %, folyamatban, blokkolt, gazdátlan — mindegyik
  mellett mini trendgörbe.
- **Haladási sáv** a sprint hátralévő idejéről.
- **Lefúrás:** csempe → szűrt feladatlista → maga a Jira-feladat.
- **Időszakváltó:** aktuális sprint, előző sprint, utolsó 30 nap.
- Világos és sötét téma, színvak-barát színek; egy státusz mindenhol ugyanazt a színt
  kapja.

### 2. fázis – Csapat és terhelés
- **Csapattag-kártyák:** avatar, név, színes sáv a feladatok állapotáról; jelzés, ha
  valaki túlterhelt vagy elakadt.
- **Terhelés-hőtérkép:** emberek × napok, a szín erőssége az aktivitást mutatja.
- **Ledolgozott órák** a Tempo adataiból.

### 3. fázis – Folyamat és nézetek
- **Flow-diagramok:** halmozott sávdiagram arról, hol torlódik a munka, és pontdiagram az
  átfutási időkről.
- **Eseményfolyam:** idővonal a legutóbbi változásokról.
- **Kétféle nézet:** vezetői (összesített, trendközpontú) és csapat (részletes, napi
  szintű).
- **Rendszeres (pl. heti) vezetői összefoglaló** automatikusan.

**Sikeres, ha:** kevesebb a státuszegyeztető meeting és a kézi riport, a kockázatok
korábban látszanak, és a terhelés kiegyensúlyozottabb.

## Érintettek

- **Felhasználók:** a HUGO csapat tagjai és a vezetők.
- **Rendszerrészek:** web, API, Jira és Tempo API (csak olvasás).

## Korlátok

- Nem kapcsolódik az értékeléshez (`0001`).
- A Jirát és a Tempót a rendszer csak olvassa.
- Csapatszintű átláthatóság, nem egyéni teljesítménymérés: nincs rangsor, az egyéni nézet
  a terhelést és a segítségigényt mutatja.
- Tokenek és valódi adatok nem kerülhetnek a publikus repóba.

## Nincs benne (out of scope)

- **Az első spec csak az 1. fázist fedi.** A 2. és a 3. fázis a dashboard későbbi
  iterációja.
- AI-funkciók: automatikus vezetői összefoglaló és kérdezhető felület (most nem kell).
- Bármilyen írás a Jirába vagy a Tempóba.

## Nyitott kérdések

- **Mit jelent a „blokkolt” és a „gazdátlan”?** Melyik Jira-státusz, flag vagy mező
  alapján?
- **Milyen gyakran frissüljenek az adatok?** Oldalbetöltéskor, időzítve, kézzel?
- **A trendgörbéhez kell előzmény:** a rendszer tárolja a napi állapotokat, vagy a Jirából
  számolható?
- **Mikor számít valaki túlterheltnek vagy elakadtnak?** (2. fázis)
- **Mit mutat a hőtérkép „aktivitása”?** Tempo-órák, Jira-változások, vagy mindkettő?
  (2. fázis)
- **Honnan jön az avatar?** (2. fázis)
- **Hova megy a heti összefoglaló, és mi van benne?** (3. fázis)
