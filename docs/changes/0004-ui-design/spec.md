# Spec: Egységes, igényes felület

- **ID:** 0004-ui-design
- **Státusz:** Approved
- **Intent:** [intent.md](intent.md)
- **Dátum:** 2026-10-27

## Összefoglaló

Az app egy kész komponenskészletre épülő, letisztult és modern megjelenést kap, világos és
sötét móddal. Asztali gépen bal oldali menü látszik, mobilon felső fejléc, és az adminnál
alsó menüsáv is. Mind a 7 meglévő oldal az új elemekből épül, és az állapotokat színes
címkék, a kitöltöttséget folyamatjelző és összesítő csempék mutatják. Visszafogott
CSS-animációk, felugró visszajelzések és menüjelvények segítik a tájékozódást. A működés,
az útvonalak, a jogosultságok, az API és az adatok nem változnak.

## Megjelenés

### Komponenskészlet és téma

- **PrimeNG** komponenskészlet, **Aura** témával. Ez a PrimeTek saját design-rendszere,
  amely design tokenekkel testreszabható, és nem igényel Tailwindet. Így megfér az
  `apps/web/CLAUDE.md` szabályaival: SCSS-ben stílusozunk, és Flexboxot használunk, Gridet
  nem.
- **Színvilág:** semleges szürke felületek, egyetlen kiemelőszínnel (**indigó**). Ez adja a
  gombokat, linkeket, a kijelölt menüpontot és a fókuszjelzést.
- **Betűtípus:** **Manrope** (változtatható vastagságú változat), az app saját
  csomagjából betöltve, külső betűtípus-szolgáltatás nélkül. Ha valamiért nem töltődik be,
  a rendszer saját sans-serif betűje (`system-ui`) lép a helyére.
- **Ikonok:** PrimeIcons. Az ikonok a menüpontokat és az elsődleges műveleteket
  egészítik ki, a szöveges feliratot nem helyettesítik. Ez alól kivétel a mobil fejléc
  téma- és kilépés-gombja, amelyeknek akadálymentes neve van.
- **Felületi elemek:** minden oldal ugyanazokat az elemeket használja: gomb, beviteli mező,
  legördülő lista, jelölőnégyzet vagy kapcsoló, táblázat, kártya, címke, jelvény,
  avatar, folyamatjelző, helykitöltő, felugró visszajelzés (toast) és megerősítő
  párbeszédablak. Oldalanként egyedi alapelem-stílus nincs, az oldalak SCSS-e
  csak elrendezést tartalmaz.

### Új függőségek (jóváhagyva)

| Csomag | Verzió | Miért |
|---|---|---|
| `primeng` | `^22.1.4` | komponenskészlet (Angular 22-vel kompatibilis) |
| `@primeuix/themes` | `^3.1.0` | az Aura téma és a design tokenek |
| `@angular/cdk` | `^22.2.0` | a PrimeNG kötelező peer függősége |
| `primeicons` | `^8.0.2` | ikonkészlet |
| `@fontsource-variable/manrope` | `^5.3.0` | a Manrope betűtípus, helyben kiszolgálva |

Grafikonkönyvtár (pl. Chart.js) **nem** kerül be: a kitöltöttséget a PrimeNG saját,
grafikon nélküli folyamatjelzője mutatja.

### Állapotcímkék

| Állapot | Felirat | Szín |
|---|---|---|
| Űrlap: piszkozat | `Piszkozat` | szürke (secondary) |
| Űrlap: kiküldve | `Kiküldve` | kék (info) |
| Kitöltés: kitöltendő | `Kitöltendő` | sárga (warn) |
| Kitöltés: beküldve | `Beküldve` | zöld (success) |

A feliratok azonosak a mostaniakkal. Az állapot a szín nélkül, a felirat alapján is
egyértelmű.

### Világos és sötét mód

- Az app **világos módban** indul, akkor is, ha az operációs rendszer sötét módra van
  állítva. A rendszerbeállítás semmilyen esetben nem hat a megjelenésre.
- Egy **kapcsoló** vált a két mód között. A kapcsoló minden oldalon elérhető, a belépés
  oldalon is.
- A választás a böngészőben megmarad: oldalfrissítés, kilépés és újbóli belépés után is
  a legutóbb választott mód látszik. Ez nem személyhez kötött, hanem böngészőhöz.
- Mindkét módban minden szöveg és vezérlő olvasható; a kontraszt legalább WCAG AA.

## Viselkedés

### Navigációs keret (belépés után)

A menüpontok, sorrendjük, a kezdőoldalak és a kijelölés szabálya (mindig a legpontosabban
illeszkedő menüpont kiemelt) **változatlanok** a 0001-hez képest.

| Szerepkör | Menüpontok (ikonnal) |
|---|---|
| **Admin** | Űrlapok · Új űrlap · Csapattagok |
| **Kolléga** | Értékeléseim |

**Asztali nézet (768 px-től szélesebb):**

- Bal oldalon fix szélességű **oldalsáv**:
  - felül az app neve;
  - alatta a menüpontok, egymás alatt, ikonnal és felirattal (és ahol van, jelvénnyel,
    lásd lent);
  - alul a bejelentkezett felhasználó neve (a nevéből képzett monogramos avatarral), a
    téma-kapcsoló és a **Kilépés** gomb.
- Jobbra a tartalom, olvasható maximális szélességgel.

**Mobil nézet (768 px és keskenyebb):**

- Nincs oldalsáv.
- Felül keskeny **fejléc**: az app neve, a téma-kapcsoló és a **Kilépés** gomb.
- Alul fix **menüsáv**: a menüpontok egymás mellett, ikonnal és felirattal. A kijelölt
  menüpont kiemelt.
- A menüsáv **csak akkor jelenik meg, ha egynél több menüpont van**. A kollégának
  egyetlen menüpontja van, ezért nála nincs alsó menüsáv: egy egygombos sávnak nincs
  navigációs szerepe, csak helyet foglalna. Neki a fejléc és a tartalom látszik.
- A tartalom nem csúszik a fejléc és a menüsáv alá. Vízszintes görgetés nincs.

### Menüjelvények

- **Admin, *Űrlapok* menüpont:** a piszkozatok száma.
- **Kolléga, *Értékeléseim* menüpont:** a kitöltendő értékelések száma.
- A jelvény csak akkor látszik, ha a szám nagyobb nullánál.
- A számot a már meglévő listavégpontokból számoljuk; új API nincs.
- A szám belépéskor és minden oldalváltáskor frissül. Így mentés, kiküldés, törlés vagy
  beküldés után, amikor az app a következő oldalra visz, már az új érték látszik.
- Ha a szám lekérése nem sikerül, a jelvény nem látszik. Hibaüzenet ilyenkor nincs, és
  a navigáció ettől működik.
- Mobilon a kolléga nem lát alsó menüsávot, ezért a jelvénye a fejlécben, az app neve
  mellett jelenik meg.
- A jelvénynek akadálymentes felirata van, pl. „2 kitöltendő”, „1 piszkozat”.

### Oldalak

Minden oldal tetején **oldalfejléc** van: cím, és ha az oldalon van fő művelet, akkor
jobbra az elsődleges gomb.

- **Belépés (`/login`):**
  - középre igazított kártyák: **Kolléga belépés** (legördülő névválasztó) és **Admin
    belépés** (e-mail, jelszó);
  - asztali nézetben egymás mellett, mobilon egymás alatt;
  - felül az app neve, sarokban a téma-kapcsoló;
  - a hibaüzenetek a kártyán belül, figyelmeztető stílusban jelennek meg, a mostani
    szöveggel.
- **Űrlapok (`/admin/forms`):**
  - oldalfejléc **Új űrlap** gombbal;
  - felül 4 **összesítő csempe**:
    - *Összes űrlap*;
    - *Piszkozat*;
    - *Kiküldve*;
    - *Beküldött kitöltések* (a kiküldött űrlapok beküldött kitöltéseinek összege a
      címzettek összegéhez képest, pl. „7 / 12”).
  - A csempék a betöltött listából számolnak, új API-hívás nincs.
  - Alattuk a táblázat a mostani oszlopokkal. Az *Állapot* oszlopban címke van. A
    *Beküldve* oszlopban kiküldött űrlapnál folyamatjelző és „3 / 5” felirat látszik,
    piszkozatnál „–”.
  - A sor bármely pontjára kattintva megnyílik az űrlap.
  - Mobilon a táblázat helyett soronként egy kártya, ugyanazokkal az adatokkal.
  - Üres lista: ikon, „Még nincs űrlap.” és egy **Új űrlap** gomb.
- **Új űrlap / Piszkozat szerkesztése:**
  - a *Fő téma* és a *Kit értékelnek?* mező címkével, a hibaüzenet a mező alatt;
  - minden kérdés egy **kártya**:
    - fejlécében a sorszám és a típus címkéje (*Skála* / *Szabad szöveg*);
    - benne a kérdés szövege, skálánál a legkisebb és legnagyobb érték;
    - a *Kötelező* **kapcsoló**;
    - a kártya jobb felső sarkában a *Kérdés törlése* gomb (ikon + akadálymentes név).
  - A kérdések alatt a **+ Skálás kérdés** és **+ Szöveges kérdés** gomb.
  - Az oldal alján a **Mentés piszkozatként** elsődleges gomb, szerkesztéskor a
    **Törlés** veszélyes (piros) stílusú gomb.
- **Űrlap részletei (`/admin/forms/{id}`):**
  - oldalfejléc a fő témával és az állapotcímkével, alatta az értékelt személy neve;
  - piszkozatnál a **Szerkesztés**, **Kiküldés** (elsődleges) és **Törlés** (veszélyes)
    gomb;
  - a kérdések számozott listája, skálánál a tartomány, a kötelezők *kötelező*
    címkével;
  - kiküldött űrlapnál:
    - **kitöltöttségi sáv**, pl. „3 / 5 beküldve”;
    - a címzettek táblázata állapotcímkével és beküldési idővel;
    - a beküldött válaszok címzettenként egy **lenyitható panelben**, a panel fejlécében
      a név és a beküldés ideje;
    - ha még senki nem küldte be: „Még senki nem küldte be.”
- **Csapattagok (`/admin/team`):**
  - a tagok listája, mindegyiknél monogramos avatar és név;
  - üres lista: ikon és „Nincs csapattag.”
- **Értékeléseim (`/reviews`):**
  - felül 2 **összesítő csempe**: *Kitöltendő* és *Beküldve* darabszám, a betöltött
    listából számolva;
  - alatta soronként egy **kártya**: fő téma, értékelt neve, állapotcímke, beküldött
    kitöltésnél a beküldés ideje;
  - kitöltendőnél **Kitöltés** gomb, beküldöttnél nincs művelet;
  - üres lista: ikon és „Nincs kitöltendő értékelésed.”
- **Kitöltés (`/reviews/{id}`):**
  - oldalfejléc a fő témával, alatta az értékelt neve;
  - minden kérdés egy kártya; a kötelezőknél a kérdés mellett `*`;
  - a skálás kérdés **gombsorként** jelenik meg, ha legfeljebb 11 lehetséges értéke
    van. Egyszerre egy érték választható, a választott kiemelt. 11-nél több érték
    esetén a skála **számbeviteli mező** a tartomány megjelölésével;
  - a szöveges kérdés többsoros szövegmező;
  - a hiba az adott kérdés kártyáján, figyelmeztető stílusban jelenik meg;
  - alul a **Beküldés** elsődleges gomb. Mobilon a gomb a képernyő alján rögzítve,
    görgetés közben is látszik, és nem takar ki tartalmat;
  - beküldött kitöltésnél a mostani szöveg látszik: „Ezt az értékelést már beküldted.”

### Megerősítés

A **Törlés** (a részleteknél és a szerkesztőben is) a böngésző saját `confirm` ablaka
helyett az app saját, animáltan megjelenő **megerősítő párbeszédablakát** nyitja meg. A szöveg „Biztosan
törlöd?”, a gombok **Törlés** (veszélyes) és **Mégse**. A viselkedés változatlan: a
**Mégse** semmit nem töröl, a **Törlés** ugyanúgy töröl és a listára visz, mint eddig.

### Felugró visszajelzések (toast)

Sikeres mutáló műveletek után rövid, magától eltűnő visszajelzés jelenik meg. Közben az
app pontosan oda navigál, ahova eddig.

| Művelet | Szöveg |
|---|---|
| Űrlap mentése piszkozatként (új vagy szerkesztett) | „Piszkozat mentve” |
| Kiküldés | „Űrlap kiküldve” |
| Piszkozat törlése | „Piszkozat törölve” |
| Kitöltés beküldése | „Értékelés beküldve” |

- Helye: asztalon jobb felül, mobilon felül középen.
- Kb. 4 másodperc után magától eltűnik, és be is zárható.
- A képernyőolvasó felolvassa (`aria-live`).
- **Hiba nem jelenik meg toastban.** A hibaüzenetek ott maradnak, ahol eddig voltak (lásd
  lent), hogy a hiba a hibás mező vagy oldal mellett látszódjon.

### Animációk

Csak natív CSS-sel és az Angular `animate.enter` / `animate.leave` szintaxisával, az
`apps/web/CLAUDE.md` animációs szabályai szerint.

- **Oldalváltás:** az új oldal tartalma rövid áttűnéssel és enyhe felfelé mozdulással
  jelenik meg.
- **Listák betöltése:** a táblázatsorok, kártyák és csempék betöltéskor lépcsőzetesen
  úsznak be. A lépcsőzés legfeljebb az első 10 elemre vonatkozik, hogy hosszú listánál se
  kelljen várni.
- **Kérdés hozzáadása és törlése** a szerkesztőben: a kártya beúszik, illetve
  összecsukódva tűnik el.
- **Lenyitható panelek** (válaszok): a lenyitás és összecsukás animált.
- **Kitöltöttségi sávok:** betöltéskor nulláról töltődnek fel a valódi értékig.
- **Állapotcímke, téma-váltás, menüpont-kijelölés:** a színváltás finom átmenettel
  történik, nem ugrásszerűen.
- **Párbeszédablak és toast:** animált megjelenés és eltűnés.
- **Gombok várakozás közben:** a belépés, mentés, kiküldés, törlés és beküldés gombja a
  kérés idejére töltő állapotba vált (forgó ikon), és ilyenkor nem kattintható.
- Az animációk 150–300 ms-osak, és egyik sem késlelteti a használatot: kattintani,
  gépelni az animáció alatt is lehet.
- **Csökkentett mozgás:** ha a rendszerben a „mozgás csökkentése” be van kapcsolva
  (`prefers-reduced-motion: reduce`), a mozgó animációk elmaradnak, és csak azonnali
  vagy nagyon rövid áttűnés marad. Ez akadálymentességi beállítás; a téma ettől
  függetlenül továbbra sem követi a rendszert.

### Töltés és hibák

- Amíg egy oldal adatai töltődnek, a tartalom helyén **helykitöltő (skeleton)** látszik.
  Üres lista vagy hibaüzenet nem villan fel közben.
- Minden hibaüzenet szövege és megjelenési helye ugyanaz, mint eddig. Csak a stílusuk
  egységes: figyelmeztető üzenetdoboz az oldal vagy kártya tetején, mezőszintű hibánál
  piros szöveg a mező alatt, és a mező kerete is piros.

### Akadálymentesség

- Minden vezérlő billentyűzettel elérhető, és a fókusz jól látható (kiemelőszínű keret).
- A csak ikonból álló gomboknak akadálymentes neve (`aria-label`) van.
- A navigáció `nav` elem „Fő navigáció” névvel. A kijelölt menüpontnak
  `aria-current="page"` attribútuma van.

## API

Nincs változás. A `contracts/openapi.json` nem módosul.

## Adatmodell

Nincs változás. Új Flyway migráció nem készül.

A téma választása a böngésző helyi tárában (`localStorage`) marad meg. A kulcs neve
`hugo-theme`, az értéke `light` vagy `dark`. Ez nem személyes és nem érzékeny adat.

## Elfogadási kritériumok

A felhasználó döntése szerint (2026-10-27) ehhez a UI/design változáshoz **új teszt nem
készül**. Minden kritériumot kézi ellenőrzéssel nézünk végig: a build végén böngészőben,
asztali és mobil szélességen, mindkét módban. A *[kézi]* jelölés azokat a kritériumokat
jelöli, amelyek eleve csak vizuálisan ellenőrizhetők (pl. képernyőszélesség, kontraszt).
A meglévő 0001-tesztek megmaradnak, és zöldnek kell maradniuk (AC1).

**Működés változatlan**

1. **Adott** a 0001 összes meglévő web- és API-tesztje, **amikor** lefut az `npm run
   check`, **akkor** mind zöld. Egyetlen állítás sem gyengül, a tesztek legfeljebb az új
   DOM-szerkezethez igazodnak (pl. másik szelektor).
2. **Adott** a 0004 változás, **amikor** összevetem a `contracts/openapi.json`-t és a
   migrációkat a `main`-nel, **akkor** nincs eltérés.

**Téma**

3. **Adott** egy böngésző, amelyben nincs mentett téma, **amikor** megnyitom az appot,
   **akkor** világos mód látszik.
4. **Adott** egy böngésző, amelyben nincs mentett téma, és az operációs rendszer sötét
   módra van állítva, **amikor** megnyitom az appot, **akkor** is világos mód látszik.
5. **Adott** világos mód, **amikor** a kapcsolóra kattintok, **akkor** sötét mód
   látszik, és a `hugo-theme` értéke `dark`. Újabb kattintásra világos lesz, és az érték
   `light`.
6. **Adott** mentett `dark` téma, **amikor** újratöltöm az oldalt, **akkor** sötét mód
   látszik. Ugyanez igaz kilépés és újbóli belépés után is.
7. **Adott** a belépés oldal, **amikor** megnyitom, **akkor** a téma-kapcsoló ott is
   elérhető és működik.
8. *[kézi]* **Adott** világos, majd sötét mód, **amikor** végignézem mind a 7 oldalt,
   **akkor** minden szöveg, címke és vezérlő olvasható, és nincs „beragadt” (a módot nem
   követő) felület.

**Navigációs keret**

9. **Adott** belépett admin, **amikor** bármely admin oldalon vagyok, **akkor** a
   navigációban az *Űrlapok*, *Új űrlap*, *Csapattagok* pont látszik ebben a sorrendben,
   mindegyik ikonnal és felirattal.
10. **Adott** belépett kolléga, **amikor** az *Értékeléseim* oldalon vagyok, **akkor** a
    navigációban csak az *Értékeléseim* pont látszik.
11. **Adott** belépett admin a `/admin/forms/7` oldalon, **amikor** megnézem a menüt,
    **akkor** pontosan egy pont kiemelt (*Űrlapok*), és annak `aria-current="page"`
    attribútuma van.
12. **Adott** belépett felhasználó, **amikor** megnézem a keretet, **akkor** látszik a
    neve, a nevéből képzett monogram, a téma-kapcsoló és a **Kilépés** gomb.
13. **Adott** belépett felhasználó, **amikor** a **Kilépés** gombra kattintok, **akkor**
    ugyanúgy kilépek és a `/login`-ra kerülök, mint eddig.
14. *[kézi]* **Adott** 768 px-nél szélesebb ablak, **amikor** belépek, **akkor** bal
    oldali menüsáv látszik, alsó menüsáv nem.
15. *[kézi]* **Adott** 768 px vagy keskenyebb ablak (pl. 375 px), **amikor** adminként
    belépek, **akkor** felső fejléc és alsó menüsáv látszik, oldalsáv nem; a tartalom nem
    csúszik alájuk, és nincs vízszintes görgetés egyik oldalon sem.
16. **Adott** belépett kolléga (egy menüpont), **amikor** megnézem a keretet, **akkor**
    az alsó menüsáv nem jön létre a DOM-ban; belépett adminnál (három menüpont)
    létrejön.

**Állapotok és összesítők**

17. **Adott** egy piszkozat és egy kiküldött űrlap, **amikor** az *Űrlapok* oldalon
    vagyok, **akkor** az állapotuk `Piszkozat`, illetve `Kiküldve` feliratú címkében
    látszik, eltérő színnel.
18. **Adott** 3 űrlap (1 piszkozat; 2 kiküldött, 2/3 és 1/4 beküldéssel), **amikor** az
    *Űrlapok* oldalon vagyok, **akkor** a csempék: *Összes űrlap* 3, *Piszkozat* 1,
    *Kiküldve* 2, *Beküldött kitöltések* „3 / 7”.
19. **Adott** egy kiküldött űrlap 3/5 beküldéssel, **amikor** az *Űrlapok* oldalon
    vagyok, **akkor** a sorában „3 / 5” felirat és 60%-os folyamatjelző látszik.
    Piszkozatnál „–” látszik, folyamatjelző nincs.
20. **Adott** egy űrlap a listában, **amikor** a sorára (nem csak a címére) kattintok,
    **akkor** megnyílik a részletek oldala.
21. **Adott** egy kiküldött űrlap 3/5 beküldéssel, **amikor** megnyitom a részleteit,
    **akkor** „3 / 5 beküldve” kitöltöttségi sáv látszik, és a címzettek állapota
    `Kitöltendő` vagy `Beküldve` címkében jelenik meg.
22. **Adott** egy kiküldött űrlap 2 beküldött kitöltéssel, **amikor** megnyitom a
    részleteit, **akkor** a válaszok 2 lenyitható panelben vannak, a fejlécben a névvel
    és a beküldés idejével. Lenyitva a kérdés–válasz párok a mostani tartalommal
    látszanak.
23. **Adott** egy kolléga 2 kitöltendő és 1 beküldött kitöltéssel, **amikor** az
    *Értékeléseim* oldalon van, **akkor** a csempék: *Kitöltendő* 2, *Beküldve* 1.
    A kitöltendő kártyákon **Kitöltés** gomb van, a beküldöttön nincs.

**Űrlapok és megerősítés**

24. **Adott** a szerkesztő, **amikor** skálás és szöveges kérdést adok hozzá, **akkor**
    mindkettő külön kártyán jelenik meg sorszámmal és típuscímkével (*Skála* / *Szabad
    szöveg*), és a *Kötelező* kapcsolóval állítható.
25. **Adott** a szerkesztő üres fő témával, **amikor** mentek, **akkor** a mostani
    hibaüzenet a mező alatt jelenik meg, és a mező hibás állapotú (`aria-invalid="true"`).
26. **Adott** egy piszkozat részletei, **amikor** a **Törlés** gombra kattintok, **akkor**
    az app saját megerősítő ablaka nyílik meg „Biztosan törlöd?” szöveggel. A böngésző
    `confirm` ablaka nem hívódik meg.
27. **Adott** a megerősítő ablak, **amikor** a **Mégse** gombra kattintok, **akkor** az
    ablak bezárul, nem indul törlő API-hívás, és az oldalon maradok.
28. **Adott** a megerősítő ablak, **amikor** a **Törlés** gombra kattintok, **akkor** a
    piszkozat törlődik, és az *Űrlapok* listára kerülök.
29. **Adott** a szerkesztőben egy meglévő piszkozat, **amikor** a **Törlés** gombra
    kattintok, **akkor** ugyanaz a megerősítő ablak és viselkedés érvényes, mint a 26–28.
    pontban.

**Kitöltés**

30. **Adott** egy 1–5 skálás kérdés, **amikor** megnyitom a kitöltést, **akkor** 5 gomb
    látszik (1…5), egyszerre egy választható, és a választott érték kiemelt.
31. **Adott** egy 0–20 skálás kérdés (21 érték), **amikor** megnyitom a kitöltést,
    **akkor** gombsor helyett számbeviteli mező látszik a „0–20” tartomány
    megjelölésével. A beküldött érték ugyanaz, mint amit beírtam.
32. **Adott** egy kitöltés hiányzó kötelező válasszal, **amikor** beküldöm, **akkor** a
    mostani hibaüzenet az adott kérdés kártyáján jelenik meg.

**Töltés, üres állapot, akadálymentesség**

33. **Adott** egy lista oldal, amelynek adatai még töltődnek, **amikor** megnyitom,
    **akkor** helykitöltő látszik, az üres lista üzenete pedig nem.
34. **Adott** üres űrlaplista, **amikor** az *Űrlapok* oldalon vagyok, **akkor** „Még
    nincs űrlap.” üzenet és egy **Új űrlap** gomb látszik.
35. **Adott** bármely oldal, **amikor** a csak ikonból álló gombokat nézem, **akkor**
    mindegyiknek van nem üres `aria-label`-je.
36. *[kézi]* **Adott** bármely oldal mindkét módban, **amikor** billentyűzettel
    (Tab / Shift+Tab / Enter / Szóköz) navigálok, **akkor** minden vezérlő elérhető, a
    fókusz mindig jól látható, és a megerősítő ablak Esc-re bezárul.

**Egységes elemek**

37. **Adott** a 7 oldal SCSS-e, **amikor** átnézem, **akkor** nincs bennük saját gomb-,
    mező-, táblázat- vagy címkestílus: csak elrendezés (Flexbox, térköz, szélesség), és
    nincs `display: grid`.

**Menüjelvények**

38. **Adott** belépett admin 2 piszkozattal és 1 kiküldött űrlappal, **amikor** megnézem
    a menüt, **akkor** az *Űrlapok* pont mellett „2” jelvény látszik, „2 piszkozat”
    akadálymentes felirattal. 0 piszkozatnál nincs jelvény.
39. **Adott** belépett kolléga 2 kitöltendő és 1 beküldött kitöltéssel, **amikor**
    megnézem a keretet, **akkor** az *Értékeléseim* jelvénye „2”. Egy kitöltés beküldése
    után, a listára visszaérve, „1”.
40. **Adott** belépett admin, **amikor** a jelvény számának lekérése hibával tér vissza,
    **akkor** nincs jelvény, nincs hibaüzenet, és a menüpontok működnek.

**Felugró visszajelzések**

41. **Adott** a szerkesztő érvényes adatokkal, **amikor** mentek, **akkor** „Piszkozat
    mentve” toast jelenik meg, és a részletek oldalára kerülök, mint eddig.
42. **Adott** egy piszkozat, **amikor** kiküldöm, **akkor** „Űrlap kiküldve” toast jelenik
    meg. Törlés megerősítése után „Piszkozat törölve” toast jelenik meg.
43. **Adott** egy kitöltendő értékelés, **amikor** sikeresen beküldöm, **akkor**
    „Értékelés beküldve” toast jelenik meg, és az *Értékeléseim* listára kerülök.
44. **Adott** egy sikertelen művelet (pl. beküldés hiányzó kötelező válasszal), **amikor**
    megtörténik, **akkor** nem jelenik meg toast, a hibaüzenet a mostani helyén
    látszik.

**Animációk és várakozás**

45. **Adott** egy mentés, kiküldés, törlés, beküldés vagy belépés, amelynek a kérése még
    folyamatban van, **amikor** megnézem a gombot, **akkor** töltő állapotban van és le
    van tiltva. Egy második kattintás nem indít újabb kérést.
46. **Adott** a szerkesztő, **amikor** törlök egy kérdést, **akkor** a kártya kilépő
    animációs osztályt kap, és az animáció után eltűnik a DOM-ból; a kérdés a mentett
    adatból is kimarad.
47. *[kézi]* **Adott** bármely oldal, **amikor** oldalt váltok, listát töltök be,
    panelt nyitok vagy témát váltok, **akkor** az animációk a fent leírt módon,
    150–300 ms alatt lefutnak, és közben is lehet kattintani.
48. *[kézi]* **Adott** a rendszerben bekapcsolt „mozgás csökkentése”, **amikor**
    végignézem az oldalakat, **akkor** mozgó (beúszó, csúszó) animáció nincs.

**Üres állapot és mobil kitöltés**

49. **Adott** üres lista az *Űrlapok*, *Csapattagok* vagy *Értékeléseim* oldalon,
    **amikor** megnyitom, **akkor** az üzenet mellett ikon is látszik.
50. *[kézi]* **Adott** egy hosszú kitöltés 375 px széles ablakban, **amikor** görgetek,
    **akkor** a **Beküldés** gomb végig látszik a képernyő alján, és nem takar ki
    kérdést vagy beviteli mezőt.

## Nincs benne (out of scope)

- Új funkció, új adat, új API-végpont, API-változás.
- Grafikonok és grafikonkönyvtár (a `0003` dashboard dönt róluk).
- Hibák megjelenítése toastban (a hibák helyben maradnak).
- Megerősítés kérése a **Kiküldés** vagy a **Beküldés** előtt (ez viselkedésváltozás
  lenne).
- Saját logó, arculati kézikönyv.
- Személyhez (fiókhoz) kötött téma-beállítás a szerveren.
- Automatikus vizuális vagy böngészős (end-to-end) tesztek; ezekhez új eszköz kellene.
- Többnyelvűség.
- Az intentben „később”-re tett tételek: „Mentve X perce” jelzés, szűrés és keresés,
  idővonal nézet, nyomtatható vagy exportálható összegzés, frissítés gomb.
- A sötét mód automatikus követése a rendszerbeállítás szerint (elvetve).

## Nyitott kérdések

Nincs. Eldöntve (2026-10-27):

1. Kiemelőszín: indigó.
2. Betűtípus: Manrope (`@fontsource-variable/manrope`), az app saját csomagjából.
3. Mobil töréspont: 768 px.
4. A kolléga mobilon nem kap alsó menüsávot (csak egy menüpontja van); az adminnál
   megjelenik.
5. Az új függőségek (`primeng`, `@primeuix/themes`, `@angular/cdk`, `primeicons`,
   `@fontsource-variable/manrope`) jóváhagyva.
6. Az intent nice-to-have listájából a jelvények, a toastok, az animált megerősítés, az
   ikonos üres állapotok és a mobilon rögzített Beküldés gomb bekerül; a többi később.
