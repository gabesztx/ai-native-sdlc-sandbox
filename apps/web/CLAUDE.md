# apps/web — Angular szabályok

A gyökér `CLAUDE.md` kiegészítése a frontendhez.

Csak az itt megadott szabályok alapján generálj kódot; ne találj ki
mintát, könyvtárat vagy struktúrát.

## Stack

Angular 22, TypeScript strict, SCSS, RxJS 7.2+, Vitest. **Zoneless**: nincs `zone.js`;
az állapotváltozást signal jelzi, nem `setTimeout` vagy manuális change detection.

## PrimeNG licenc

- A PrimeNG a PrimeUI License alá tartozik (személyes Community kulcs). A kulcs az
  `apps/web/primeui-license.key` fájlban van, gitignore-olva. Soha ne commitold, és ne
  írd be kódba.
- Az `npm start` és az `npm run build` `--define PRIMEUI_LICENSE=...` kapcsolóval adja be
  a kulcsot; a `core/theme/primeui-license.ts` olvassa ki, és a `providePrimeNG({ license })`
  kapja meg.
- Kulcs nélkül is működik minden, csak megjelenik az „Invalid PrimeUI License” jelzés. A
  jelzést elrejteni vagy a licencellenőrzést kiiktatni tilos (a licenc tiltja).

## Angular CLI

- Új Angular-elemet mindig az Angular CLI-vel hozz létre, ne kézzel:
  `npx ng generate component features/<feature>/pages/<nev>` (komponensnél
  `features/<feature>/components/<nev>`, szolgáltatásnál
  `npx ng generate service features/<feature>/services/<nev>`; ugyanígy `guard`, `pipe`,
  `directive`). A parancsot az `apps/web` mappában futtasd.
- Angular-csomag hozzáadása: `npx ng add <csomag>`; frissítés: `npx ng update`.
- Az `angular.json` sémabeállításai miatt a generált komponens már OnPush, SCSS és
  `.component` fájlnevű; utána csak a szabályok szerinti tartalmat írd bele.

## Mappastruktúra

```
src/app/
  core/                         app-szintű, egyetlen példány
    api/                        az openapi.json-ból generált kliens (gitignored) — kézzel ne szerkeszd
    auth/  http/  layout/  theme/
  features/<feature>/           pl. features/forms/, features/reviews/, features/dashboard/
    pages/<oldal>/              route-célpont komponensek
    components/<komponens>/     csak ehhez a feature-höz tartozó komponensek
    services/                   a feature szolgáltatásai (a generált service-ek fölött)
    <feature>.routes.ts         a feature route-jai
  shared/
    components/                 több feature-ben is használt komponensek
    pipes/                      közös pipe-ok
    models/                     csak felületi típusok (pl. szűrőállapot) — API-típus soha
    animations/                 közös @keyframes és animáció-osztályok
```

- A `core/`, a `shared/` és a feature gyökerében nincs gazdátlan helperfájl; a feature
  gyökerében csak a `<feature>.routes.ts` áll. Egyetlen komponens helpere a komponens
  mappájába kerül.
- Feature nem importál másik feature-ből. Ha két feature ugyanazt az adatot kéri, mindkettő
  a saját szolgáltatásán keresztül hívja a generált service-t.

## Komponensek

- `standalone: true`-t ne írd ki (alapértelmezett). NgModule-t ne használj.
- Mindig OnPush. Angular 22-ben ez az alapértelmezett, ezért a CLI nem írja ki; nem is
  kell kiírni. `ChangeDetectionStrategy.Eager` tilos.
- Mindig külön fájlok: `.component.ts`, `.component.html`, `.component.scss`. Soha
  inline template vagy inline style.
- `styleUrl` (egyes szám), nem `styleUrls`.
- Selector előtag nélkül, kebab-case (pl. `user-profile`).
- DI: `inject()`, nem konstruktor.
- Signal API: `input()`, `output()`, `model()` — nem `@Input()` / `@Output()`.
- Control flow: `@if`, `@for (… ; track item.id)`, `@switch` — nem `*ngIf` / `*ngFor`.
- `@let`: ha egy blokkon belül ugyanazt a signal- vagy metódushívást többször olvasnád.
- `@defer (on immediate)` a fő tartalom alatti, nehéz részekhez (külön lazy chunk),
  `@placeholder`-rel. Más trigger (`on idle`, `on viewport`) nem, mert jsdom-ban a tesztek
  nem látják a tartalmat.
- Host-kötés a `@Component` / `@Directive` `host: {}` mezőjével; `@HostBinding` /
  `@HostListener` nem.

### Minta

```
user-profile/
  user-profile.component.ts
  user-profile.component.html
  user-profile.component.scss
```

```typescript
@Component({
  selector: 'user-profile',
  templateUrl: './user-profile.component.html',
  styleUrl: './user-profile.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserProfileComponent {
  private userService = inject(UserService);
  public userId = input.required<number>();
  public name = computed(() => this.user()?.name ?? '');
  public onSave(): void { ... }
  private loadUser(): void { ... }
}
```

## Láthatóság

- `public`: minden, amit a template használ (az `input()` / `output()` implicit public).
- `private`: minden, amit csak az osztály belül használ.
- `protected` és `readonly` osztálytagon: ne.

## API-kliens és modellek

- Az API-típusok és hívások a generált kliensből (`core/api/`) jönnek. API-típust kézzel
  ne írj, ne másolj le.
- A generátor (`ng-openapi-gen`, `services: true`, `promises: false`) tagenként egy
  service-t ad (pl. `FormControllerService`), műveletenként egy Observable-t visszaadó
  metódussal (pl. `getForm({ formId })`).
- Generált service-t csak szolgáltatás injektál (`features/<feature>/services/` vagy
  `core/`). Komponens a feature-szolgáltatást használja, a generált service-t és a
  `HttpClient`-et soha.
- A generátor belső részleteire (`fn/`, `RequestBuilder`, `StrictHttpResponse`,
  `rootUrl`, `.body`) kézzel írt kód nem hivatkozik.

```typescript
@Service()
export class FormsService {
  private formApi = inject(FormControllerService);
  public get(formId: number): Observable<FormDetail> {
    return this.formApi.getForm({ formId });
  }
}
```

## Signals és reaktivitás

- Lokális UI-állapot: `signal()` — nem `BehaviorSubject`.
- Számított érték: `computed()`.
- `effect()` csak valódi side effecthez (DOM, localStorage) — adatszinkronra nem.
- Observable → signal: `toSignal(obs$, { initialValue: [] })`.
- Signal → Observable: `toObservable(mySignal)`, utána `.pipe(...)`.
- Oldal adata paraméter nélkül (lista): `rxResource`. A hibát a streamben `catchError`
  kezeli, így a resource nem kerül hibaállapotba.
- Route-paramétertől függő oldal adata: `toSignal(toObservable(this.id).pipe(switchMap(...)))`,
  a módosítás után frissülő értékhez `linkedSignal`. Itt **ne** `rxResource`: az
  betöltés közben pending taskot tart, és a `whenStable()`-t váró tesztek elakadnak.
- Az összetartozó adat (pl. űrlap és válaszai) egy láncban, egy signalban jön, hogy a
  kérések sorrendje determinisztikus legyen.
- Számot váró route-paraméter: `input.required({ transform: numberAttribute })`; a
  komponensben nincs `Number(...)`.

Keresés — signal → observable → HTTP → signal:

```typescript
public query = signal('');
public results = toSignal(
  toObservable(this.query).pipe(
    debounceTime(300),
    distinctUntilChanged(),
    switchMap(q => this.api.search(q)),
    catchError(() => of([]))
  ),
  { initialValue: [] }
);
```

Lista `rxResource`-szal:

```typescript
private formsResource = rxResource({ stream: () => this.formsService.list().pipe(catchError(() => of([]))) });
public forms = computed(() => this.formsResource.value());
public loading = computed(() => this.formsResource.isLoading());
```

Route-paramétertől függő adat:

```typescript
public id = input.required({ transform: numberAttribute });
private loaded = toSignal(toObservable(this.id).pipe(switchMap((id) => this.reviewsService.get(id))));
```

## RxJS

**Importok:** mindent `'rxjs'`-ből — nem `'rxjs/operators'`-ból.

**Leiratkozás:** `takeUntilDestroyed()` — nem manuális Subject teardown.

```typescript
this.data$.pipe(takeUntilDestroyed()).subscribe(...);
this.data$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(...);
```

Az első injection contextben, a második pl. `ngOnInit`-ben.

**Melyik flattening operátor mikor:**

| Operátor | Mikor |
|---|---|
| `switchMap` | HTTP keresés — az előző kérést megszakítja |
| `mergeMap` | Párhuzamos, független kérések |
| `concatMap` | Sorban egymás után (pl. szekvenciális mentés) |
| `exhaustMap` | Form submit — futó kérés alatt az újat figyelmen kívül hagyja |

**Szabályok:**
- `map()` adattranszformációhoz, `tap()` csak side effecthez (pl. naplózás).
- `shareReplay(1)`, ha több subscriber van ugyanarra az Observable-re.
- `firstValueFrom(obs$)`, ha Promise kell — nem `toPromise()`.
- `retry({ count: 3, delay: 1000 })` — nem `retryWhen()`.
- Soha ne `subscribe()` egy `subscribe()`-on belül — lapítsd ki operátorral.

**Hibakezelés:**
- Csak a pipe-ban, `catchError()`-ral. A `subscribe()` `error` callbackje tilos.
- Csak `subscribe(fn)` vagy `subscribe({ next: fn })` forma.
- Kezelhető hibánál `catchError()`-ból fallback Observable-t adj vissza, ne
  `throwError()`-t.

## Űrlapok és routing

- Űrlap: **Signal Forms** (`@angular/forms/signals`). Reactive Forms (`FormGroup`,
  `FormControl`, `FormArray`, `FormBuilder`, `ReactiveFormsModule`) és template-driven
  forms nem.
- A modell egy `signal` (vagy betöltött adatból `linkedSignal`), az űrlap `form(model, schema)`.
- Kötés: `<form [formRoot]="editor" (submit)="onSave()">` és `[formField]="editor.topic"`;
  PrimeNG mezőkön is.
- Validáció a sémában: `required`, `maxLength`, `applyEach` tömbelemekre, `validate`
  egyedi szabályra. A `maxlength` attribútumot és a `[invalid]` kötést a `[formField]`
  mellé nem írjuk; a PrimeNG hibás állapota `[class.p-invalid]`
  (`p-inputnumber`-en `[inputStyleClass]` is).
- Beküldés: érvénytelen űrlapnál `this.editor().markAsTouched()`, és nincs kérés.
- Dinamikus listában minden elem kap egy kliens oldali `key`-t, ez a `track` kulcsa.

```typescript
private model = signal({ topic: '', questions: [] as QuestionDraft[] });
public editor = form(this.model, (path) => {
  required(path.topic);
  maxLength(path.topic, 200);
  applyEach(path.questions, (question) => required(question.text));
});
```

- Funkcionális route guard. Feature-enként `<feature>.routes.ts` `loadComponent`-tel; az
  `app.routes.ts` `loadChildren`-nel tölti be.

## Stílus

- Mindig SCSS — nem CSS, nem inline style.
- Layout: Flexbox, Grid nem.

## Animációk

### Alapelv
- `@angular/animations` deprecated (Angular 20.2 óta) — ne használd.
- Animáció natív CSS-sel: `@keyframes`, `transition`, `@starting-style`.
- DOM-belépés/kilépés: az Angular compiler `animate.enter` / `animate.leave`
  template-szintaxisa (nem direktíva).
- Újrahasználható animációk: `src/app/shared/animations/`.

### `animate.enter` és `animate.leave`
- `animate.enter="css-class"` — DOM-ba kerüléskor alkalmazza az osztályt, a végén
  eltávolítja.
- `animate.leave="css-class"` — eltávolítás előtt alkalmazza, a végén az Angular
  eltávolítja az elemet.
- Dinamikus osztály: `[animate.enter]="mySignal()"`.
- Több osztály: szóközzel elválasztott string vagy string tömb.
- Használható `@if`, `@for`, `@defer` blokkokban és host bindingként.

### CSS-alapú állapotanimációk
- Állapotváltás CSS osztállyal: `[class.open]="isOpen()"`.
- Átmenet `transition` property-vel SCSS-ben — nem Angular trigger/state DSL-lel.
- DOM-ra kerüléskor induló transitionhöz `@starting-style`.
- `height: auto` animálása: `interpolate-size: allow-keywords` + `transition`.

### Újrahasználható animációk
- Közös `@keyframes` és animáció-osztályok: `src/app/shared/animations/`.
- Komponens-SCSS-ben csak komponens-specifikus animáció; általános keyframe-et ne
  duplikálj.

### JavaScript-alapú animációk
- Ha CSS nem elég (komplex gesztus, fizika-alapú mozgás), JS könyvtár (GSAP, Motion
  One) használható — **új függőség, előtte jóváhagyás kell**.
- `effect()` használható animáció-triggerelésre, ha signal-alapú DOM-koordináció kell —
  de csak valódi side effectként.

### Tilos
- `trigger()`, `state()`, `animate()`, `transition()` az animations DSL-ből.
- `BrowserAnimationsModule` / `provideAnimations()`.
- `[@trigger]` szintaxis templateben.
- Animációs logika a komponens `.ts` fájljában.
- Inline DOM-stílusmanipuláció eseménykezelőben (pl.
  `(error)="$event.target.style.display='none'"`) — signal + metódus, és `@if`.

## Kommentek

Ne írj kommentet a kódba (se `//`, se `/* */`, se JSDoc, se TODO). A kód beszédes
nevekkel és kis függvényekkel legyen olvasható. Ez a frontendre felülírja a gyökér
`CLAUDE.md` kódkomment-szabályát.

## Tesztek

- Vitest; a tesztfájl a tesztelt fájl mellett van: `*.spec.ts`.
- Viselkedést tesztelj (amit a felhasználó lát és tesz, a komponens publikus API-ja),
  ne belső megvalósítást.
- HTTP-t valódi hívás helyett `provideHttpClientTesting()` / `HttpTestingController`
  helyettesíti. Oldal-komponenst a `src/testing/http-test-setup.ts` `createPage`
  függvényével hozz létre; az egy change detectiont is lefuttat, így az `rxResource`
  kérése azonnal kimegy.
- Ha a teszt egy spec elfogadási kritériumát fedi, a teszt neve hivatkozzon rá (pl.
  `AC24: beküldés után Beküldve állapot`).

## Amit a lint ellenőriz

Az `npm run check` elbukik, ha sérül: `any` típus; hiányzó `public` / `private`;
`protected`; import `rxjs/operators`-ból vagy `@angular/animations`-ből; OnPush
hiánya; inline template / style; `*ngIf` / `*ngFor`; `@Input()` / `@Output()`;
`@HostBinding()` / `@HostListener()`; Reactive Forms import; import a generált kliens
`fn/` mappájából; komponensben `HttpClient` vagy generált service import; feature-ök
közötti import. A pontos szabálylista: `eslint.config.js`.

## Anti-patterns (gyors lista)

❌ komment · ❌ `readonly` / `protected` tag · ❌ inline template/style · ❌ CSS SCSS
helyett · ❌ `standalone: true` · ❌ `@Input` / `@Output` · ❌ `*ngIf` / `*ngFor` ·
❌ NgModule · ❌ `any` · ❌ konstruktoros DI · ❌ `BehaviorSubject` UI-állapotra ·
❌ nested `subscribe()` · ❌ `takeUntil(this.destroy$)` · ❌ `toPromise()`, `retryWhen()`,
`repeatWhen()` · ❌ `'rxjs/operators'` · ❌ `tap()` transzformációra · ❌ `subscribe`
`error` ága, `subscribe({ next, error })` · ❌ `throwError()` kezelhető hibánál ·
❌ `@angular/animations`, `trigger()` / `state()` DSL, `provideAnimations()`, `[@trigger]` ·
❌ animációs logika `.ts`-ben · ❌ közös keyframe komponens-SCSS-ben · ❌ inline DOM
stílusmanipuláció eseménykezelőben · ❌ kézzel írt API-típus · ❌ Grid layout ·
❌ Reactive Forms · ❌ `@HostBinding` / `@HostListener` · ❌ generált `fn/` import ·
❌ generált service vagy `HttpClient` komponensben · ❌ feature-közi import ·
❌ `rxResource` route-paraméteres oldalon · ❌ `@defer` `on idle` / `on viewport` ·
❌ gazdátlan helperfájl a `core/` / `shared/` / feature gyökerében
