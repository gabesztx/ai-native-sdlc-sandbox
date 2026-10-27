# Spec: Modern Angular struktúra és minták a webes appban

- **ID:** 0005-angular-refactor
- **Státusz:** Approved
- **Intent:** [intent.md](intent.md)
- **Dátum:** 2026-10-27

## Összefoglaló

Az `apps/web` kódja feature-first szerkezetbe kerül: minden feature-nek saját oldal-,
komponens- és szolgáltatás-mappája, valamint saját route-fájlja lesz. A generált kliens
kész Angular service-osztályokat ad, és ezeket csak a szolgáltatások használják. Az adat
`rxResource`-on keresztül jut a komponensekbe, az űrlapok Signal Formsra épülnek, a
templatek pedig `@let`-et és `@defer`-t használnak. A végfelhasználó semmilyen változást
nem lát: ugyanazok az útvonalak, a feliratok és az API-kérések.

## Kiinduló helyzet (mért, 2026-10-27, a 0004 merge után)

- **A generált `fn/` függvényeket öt kézzel írt fájl hívja közvetlenül:** az
  `auth-api.service`, a `team-api.service`, a `forms-api.service`, a
  `reviews-api.service` és a `menu-badges.service`.
- **A Reactive Formsot 3 oldal használja:** belépés, űrlapszerkesztő, kitöltés.
- **12 tesztfájl, 40 teszt, mind zöld.** A tesztek a DOM-on és a `HttpTestingController`-en
  keresztül ellenőriznek, belső service-t nem mockolnak.
- **Egy helyen sincs `@HostBinding` vagy `@HostListener`.**

## Viselkedés

### Végfelhasználó

Változatlan. Minden oldal, útvonal, guard-átirányítás, felirat, hibaüzenet, animáció és
téma pontosan úgy működik, mint a 0004 után. Az app ugyanazokat az API-kéréseket küldi
ugyanazzal a törzzsel.

### Fejlesztő (a kód szerkezete)

**Mappaszerkezet:**

```
src/app/
  core/                       app-szintű, egyetlen példány
    api/                      generált kliens (gitignored, lintből kizárva)
    auth/  http/  layout/  theme/
  features/<feature>/
    pages/<oldal>/            route-célpont komponensek
    components/<komponens>/   csak ehhez a feature-höz tartozó komponensek (ha van)
    services/                 a feature szolgáltatásai
    <feature>.routes.ts       a feature route-jai
  shared/
    components/  pipes/  models/  animations/
```

- **Feature-ök:** `auth`, `forms`, `reviews`, `team`.
- **Nincs gazdátlan fájl.** A feature, a `shared/` és a `core/` gyökerében nincs
  „lebegő” helperfájl. A feature gyökerében csak a `<feature>.routes.ts` állhat.
  - A mostani `confirm-delete.ts`, `initials.ts` és `status-labels.ts` a fenti helyek
    egyikére kerül (szolgáltatás, pipe vagy a használó komponens mappája). A pontos hely
    a plan dolga.
- **A 0004-ben létrejött `shared/ui/` neve `shared/components/` lesz.** Így követi az
  `apps/web/CLAUDE.md`-t és az 1/A döntést.
- **Feature nem importál másik feature-ből.** Ha két feature ugyanazt az adatot kéri (pl. a
  csapattagokat a `forms` és a `team` is), mindkettő a saját szolgáltatásán keresztül hívja
  a generált service-t.

**API-kliens:**

- A generátor service-osztályokat ad Observable visszatérési értékkel (`services: true`,
  `promises: false`).
  - Egy service tagenként (`FormControllerService`, `ReviewControllerService`,
    `AuthControllerService`, `TeamMemberControllerService`).
  - Egy metódus műveletenként, az operationId nevével (pl.
    `getForm({ formId }): Observable<FormDetail>`).
  - Ezt a generátorral kipróbáltam.
- **A generált kliens helye: `src/app/core/api/`.**
- **A kézzel írt kód soha nem hivatkozik a generátor belső részleteire:** `fn/`,
  `RequestBuilder`, `StrictHttpResponse`, `rootUrl`, `.body`.
- **Generált service-t csak szolgáltatás injektál** (feature- vagy core-szolgáltatás),
  komponens nem.
- **Komponens nem használ `HttpClient`-et.**

**Adatlekérés:**

- **Az oldal adatát `rxResource` adja.** Az oldal a resource-ból kapja az értéket, a
  betöltési állapotot és a hibát. A kézi `toObservable → switchMap → subscribe → signal.set`
  lánc megszűnik.
- **Kivétel a belépés oldal csapattag-listája:** az `toSignal` marad (lásd Eldöntött kérdés 1).
  Ugyanígy marad a `toSignal` azoknál a router-eseményekre épülő signaloknál, amelyek nem
  HTTP-lekérések (`shell`, `breakpoint`).
- **Mutáció** (mentés, kiküldés, törlés, beküldés, belépés): a szolgáltatás Observable-t
  ad vissza, és a komponens egyszer iratkozik fel rá. Hibakezelés `catchError`-ral a
  pipe-ban.
- **Route-paraméter:** a számot váró route-paraméter (`id`) `numberAttribute`
  transzformmal érkezik számként, így a komponensben nincs `Number(...)`.

**Űrlapok:** mind a négy űrlap (admin belépés, kolléga belépés, űrlapszerkesztő, kitöltés)
Signal Formsszal készül.
- Ez a `form()`, a `[formField]`, a `form[formRoot]` és a sémás validáció (`required`,
  `maxLength` stb.).
- Reactive Forms (`FormGroup`, `FormControl`, `FormArray`, `ReactiveFormsModule`) nem
  marad a kódban.
- A PrimeNG mezők (szöveg, legördülő, szám, kapcsoló) a `[formField]` direktívával
  kötődnek.
- **Kipróbáltam a belépés oldalon:** a változatlan tesztek zöldek maradtak.

**Template-eszközök:**

- **`@let`:** ha egy blokkon belül ugyanazt a signal- vagy metódushívást többször olvassuk
  (pl. a szerkesztőben a kérdésenkénti hibaüzenetet), az egyszer, `@let`-tel szerepel.
- **`@defer (on immediate)`:** a fő tartalom alatti, nehéz részeknél a kódot külön
  csomagba vágja. Első ilyen hely: az űrlap részletei oldalon a *Beküldött válaszok*
  harmonika.
  - Csak az `on immediate` trigger használható. Kipróbáltam: az `on idle` és az
    `on viewport` változatlan tesztek mellett eltöri az AC45-ös tesztet.
- **`host: {}`:** host-kötés kizárólag a `@Component` / `@Directive` `host` mezőjével;
  `@HostBinding` és `@HostListener` nincs.

**Lint:** az `npm run check` elbukik, ha:
- kézzel írt fájl importál a generált kliens `fn/` mappájából;
- komponens importál a generált kliens service-eiből vagy a `HttpClient`-ből;
- bárhol `ReactiveFormsModule`, `FormGroup`, `FormControl`, `FormArray` vagy `FormBuilder`
  import szerepel;
- `@HostBinding` vagy `@HostListener` szerepel;
- feature másik feature-ből importál.

A szabályok mellett a meglévő lintszabályok is érvényben maradnak. A generált `core/api/`
mappa ki van zárva a lintből.

**`apps/web/CLAUDE.md`:** a következő fejezetek frissülnek az új szerkezetre és mintákra:
- mappastruktúra;
- API-kliens és modellek;
- signals és reaktivitás;
- űrlapok és routing;
- komponensek (`host`, `@let`, `@defer`);
- amit a lint ellenőriz;
- anti-patterns.

A „Reactive Forms” szabály helyére a Signal Forms kerül. Ami nem változik, az marad,
ahogy van.

### Hibaesetek

- **Hibás vagy nem szám route-paraméter** (pl. `/admin/forms/abc`): a viselkedés
  ugyanaz, mint ma. A kérés elmegy, a szerver hibát ad, és az oldal a mostani hibaüzenetet
  mutatja.
- **API-hiba betöltéskor:** az oldal a mostani hibaüzenetet vagy üres állapotot mutatja,
  ugyanazzal a szöveggel.
- **API-hiba mutációkor:** a mezőnkénti és az általános hibaüzenet ugyanott és ugyanazzal a
  szöveggel jelenik meg, mint ma.

## API

Nincs változás. A `contracts/openapi.json` nem változik. Csak a kliens generálásának
beállítása változik (service-osztályok, Observable, új kimeneti mappa).

## Adatmodell

Nincs változás.

## Elfogadási kritériumok

Az intent szerint **új teszt nem készül, és meglévő teszt nem törlődik.** A meglévő
`*.spec.ts` fájlokban csak importútvonal változhat. A kritériumokat ezért ezek ellenőrzik:
a meglévő 40 teszt, az új lintszabályok, a verifier kódkeresése (*[keresés]*) és kézi
ellenőrzés (*[kézi]*).

1. **Adott** a refaktorált kód, **amikor** lefut az `npm run check`, **akkor** zöld, és a
   webes tesztek száma 40, mind sikeres.
2. **Adott** a refaktor előtti és utáni `*.spec.ts` fájlok, **amikor** összevetjük őket
   (`git diff -M`), **akkor** minden fájl megvan, és csak `import` sorok változtak. Nem
   változott állítás, tesztnév vagy `expectOne` URL. A `src/testing/` segédfájlokban is
   csak importútvonal változott, egyetlen kivétellel: a `createPage` egy
   `fixture.detectChanges()` sort kap (Eldöntött kérdés 1). *[keresés]*
3. **Adott** a `src/app` kézzel írt kódja, **amikor** a `fn/`, `RequestBuilder`,
   `StrictHttpResponse` vagy `rootUrl` hivatkozásokat keressük, **akkor** nincs találat.
   *[keresés, lint]*
4. **Adott** bármely `*.component.ts`, **amikor** a generált service-ek vagy a
   `HttpClient` importját keressük, **akkor** nincs találat. *[lint]*
5. **Adott** a generált kliens, **amikor** lefut a `npm run api:client`, **akkor** a
   `src/app/core/api/` mappába generál, benne a `services/` mappával. A régi `src/app/api/`
   mappa nem jön létre. *[keresés]*
6. **Adott** a `src/app/features/` mappa, **amikor** végignézzük, **akkor** minden feature
   (`auth`, `forms`, `reviews`, `team`) tartalmaz `pages/` és `services/` mappát és egy
   `<feature>.routes.ts` fájlt. A gyökérben más fájl nincs. *[keresés]*
7. **Adott** a `src/app/shared/` és a `src/app/core/`, **amikor** végignézzük, **akkor** a
   gyökérben nincs fájl, csak mappák vannak. A `shared/` mappái `components/`, `pipes/`,
   `models/` és `animations/`, és nincs `shared/ui/`. *[keresés]*
8. **Adott** egy feature fájlja, **amikor** másik feature-ből importálna, **akkor** a lint
   elbukik. *[lint]*
9. **Adott** az app, **amikor** a `ReactiveFormsModule`, `FormGroup`, `FormControl`,
   `FormArray` vagy `FormBuilder` importját keressük, **akkor** nincs találat, és mind a
   négy űrlap `[formField]`-del köt. *[lint, keresés]*
10. **Adott** a belépés, a szerkesztő és a kitöltés oldal, **amikor** lefutnak a meglévő
    tesztjeik (AC43, AC44, AC45, AC50 és a hibaüzenet-tesztek), **akkor** változatlan
    állításokkal zöldek. Ez igazolja, hogy a Signal Forms ugyanazt a kérést küldi, és
    ugyanazokat a hibákat mutatja.
11. **Adott** az űrlap részletei, a szerkesztő (szerkesztés módban) és a kitöltés oldal,
    **amikor** betöltenek, **akkor** az adatuk `rxResource`-ból jön. A komponensben nincs
    `toObservable`, és nincs adatbetöltő `subscribe`. *[keresés]*
12. **Adott** a listaoldalak (űrlapok, értékeléseim, csapattagok) és a menüjelvények,
    **amikor** betöltenek, **akkor** az adatuk `rxResource`-ból jön (lásd Eldöntött kérdés
    1). *[keresés]*
13. **Adott** egy `id` route-paramétert váró oldal, **amikor** a komponenst nézzük,
    **akkor** az `id` input `numberAttribute` transzformot használ, és a komponensben
    nincs `Number(...)` hívás az id-re. *[keresés]*
14. **Adott** az app, **amikor** a `@HostBinding` vagy `@HostListener` dekorátort keressük,
    **akkor** nincs találat, és a lint tiltja őket. *[lint]*
15. **Adott** az űrlap részletei oldal, **amikor** kiküldött űrlapot nyitunk meg, **akkor**
    a *Beküldött válaszok* rész `@defer (on immediate)` blokkban van, és a build külön
    lazy chunkot ad hozzá. A meglévő AC45-ös teszt változatlanul zöld. *[keresés, build
    kimenet]*
16. **Adott** a templatek, **amikor** egy blokkon belül ugyanazt a signal- vagy
    metódushívást többször olvasnánk, **akkor** az `@let`-tel egyszer szerepel. *[review]*
17. **Adott** a kliens generálása, **amikor** az `apps/web` lintjét futtatjuk, **akkor** a
    `src/app/core/api/**` ki van zárva, a régi `src/app/api/**` kizárás pedig eltűnt.
    *[keresés]*
18. **Adott** az `apps/web/CLAUDE.md`, **amikor** elolvassuk, **akkor** a mappastruktúra, a
    generált service-ek használata, az `rxResource`, a Signal Forms, a `@let`, a
    `@defer (on immediate)`, a `host: {}` és az új lintszabályok szerepelnek benne. Nem
    szerepel benne Reactive Forms szabály, sem `src/app/api/` hely. *[review]*
19. **Adott** a futó app (`npm run dev:web` + `npm run dev:api`), **amikor** adminként és
    kollégaként végigmegyünk a fő folyamatokon, **akkor** minden ugyanúgy működik, mint a
    0004 után: belépés, űrlap létrehozása, szerkesztése, kiküldése és törlése, a
    kitöltés beküldése, a menüjelvények és a kilépés. *[kézi]*

## Nincs benne (out of scope)

- NgRx vagy más store-könyvtár; új függőség.
- Központi hibakezelő interceptor; a hibakezelés komponensenként, `catchError`-ral marad.
- Fájlnév-konvenció váltása (marad a `.component` suffix).
- Vizuális vagy UI-változás.
- API- vagy szerződésváltozás, és a kézzel írt `ProblemDetail` típus javítása.
- `@defer` más triggerrel (`on idle`, `on viewport` stb.), mert a meglévő teszteket
  módosítani kellene.

## Eldöntött kérdések (2026-10-27)

1. **`rxResource` a paraméter nélküli listáknál: az (a) változat.**
   - A `src/testing/http-test-setup.ts` `createPage` segédfüggvénye egy sort kap:
     `fixture.detectChanges()`. Ez nem `*.spec.ts`, és állítást nem változtat.
   - Így a listaoldalak és a menüjelvények is `rxResource`-ot használnak.
   - Kivétel a belépés oldal csapattag-listája: az `toSignal` marad, mert a tesztje saját
     beállítást használ.
2. **A `shared/ui/` átnevezése `shared/components/`-re: igen.** Így a mappa követi az
   `apps/web/CLAUDE.md`-t és az 1/A döntést.

## Nyitott kérdések

- Nincs.
