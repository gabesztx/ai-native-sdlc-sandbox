# apps/web — Angular szabályok

A gyökér `CLAUDE.md` kiegészítése a frontendhez.

Csak az itt megadott szabályok alapján generálj kódot; ne találj ki
mintát, könyvtárat vagy struktúrát.

## Stack

Angular 22, TypeScript strict, SCSS, RxJS 7.2+, Vitest. **Zoneless**: nincs `zone.js`;
az állapotváltozást signal jelzi, nem `setTimeout` vagy manuális change detection.

## Angular CLI

- Új Angular-elemet mindig az Angular CLI-vel hozz létre, ne kézzel:
  `npx ng generate component features/<feature>/<nev>` (ugyanígy `service`, `guard`,
  `pipe`, `directive`). A parancsot az `apps/web` mappában futtasd.
- Angular-csomag hozzáadása: `npx ng add <csomag>`; frissítés: `npx ng update`.
- Az `angular.json` sémabeállításai miatt a generált komponens már OnPush, SCSS és
  `.component` fájlnevű; utána csak a szabályok szerinti tartalmat írd bele.

## Mappastruktúra

```
src/app/
  core/                 app-szintű szolgáltatások, guardok, interceptorok
  features/<feature>/   oldalak és csak ehhez a feature-höz tartozó komponensek
                        (pl. features/forms/, features/reviews/, features/dashboard/)
  shared/components/    több feature-ben is használt komponensek
  shared/models/        csak felületi típusok (pl. szűrőállapot) — API-típus soha
  shared/animations/    közös @keyframes és animáció-osztályok
  api/                  az openapi.json-ból generált kliens — kézzel ne szerkeszd
```

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

- Az API-típusok és hívások a generált kliensből (`api/`) jönnek. API-típust kézzel
  ne írj, ne másolj le.
- Komponens nem hív közvetlenül `HttpClient`-et; a generált klienst vagy egy feature-
  szolgáltatást használ.

## Signals és reaktivitás

- Lokális UI-állapot: `signal()` — nem `BehaviorSubject`.
- Számított érték: `computed()`.
- `effect()` csak valódi side effecthez (DOM, localStorage) — adatszinkronra nem.
- Observable → signal: `toSignal(obs$, { initialValue: [] })`.
- Signal → Observable: `toObservable(mySignal)`, utána `.pipe(...)`.
- HTTP + signal-alapú újratöltés: `rxResource` (az Angular 22-beli státuszát a váz
  készítésekor ellenőrizni kell).

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

Reaktív adatlekérés `rxResource`-szal:

```typescript
public userId = signal(1);
public user = rxResource({
  params: () => ({ id: this.userId() }),
  stream: ({ params }) => this.api.getUser(params.id),
});
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

- Reactive Forms, typed controls (`FormControl<string>`); template-driven forms nem.
- Funkcionális route guard; lazy loading `loadComponent`-tel.

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
  helyettesíti.
- Ha a teszt egy spec elfogadási kritériumát fedi, a teszt neve hivatkozzon rá (pl.
  `AC24: beküldés után Beküldve állapot`).

## Amit a lint ellenőriz

Az `npm run check` elbukik, ha sérül: `any` típus; hiányzó `public` / `private`;
`protected`; import `rxjs/operators`-ból vagy `@angular/animations`-ből; OnPush
hiánya; inline template / style; `*ngIf` / `*ngFor`; `@Input()` / `@Output()`.
A pontos szabálylistát az app váza állítja be.

## Anti-patterns (gyors lista)

❌ komment · ❌ `readonly` / `protected` tag · ❌ inline template/style · ❌ CSS SCSS
helyett · ❌ `standalone: true` · ❌ `@Input` / `@Output` · ❌ `*ngIf` / `*ngFor` ·
❌ NgModule · ❌ `any` · ❌ konstruktoros DI · ❌ `BehaviorSubject` UI-állapotra ·
❌ nested `subscribe()` · ❌ `takeUntil(this.destroy$)` · ❌ `toPromise()`, `retryWhen()`,
`repeatWhen()` · ❌ `'rxjs/operators'` · ❌ `tap()` transzformációra · ❌ `subscribe`
`error` ága, `subscribe({ next, error })` · ❌ `throwError()` kezelhető hibánál ·
❌ `@angular/animations`, `trigger()` / `state()` DSL, `provideAnimations()`, `[@trigger]` ·
❌ animációs logika `.ts`-ben · ❌ közös keyframe komponens-SCSS-ben · ❌ inline DOM
stílusmanipuláció eseménykezelőben · ❌ kézzel írt API-típus · ❌ Grid layout
