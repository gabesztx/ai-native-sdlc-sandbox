# Review szabályok

Közös mérce minden reviewernek — embernek, a `/review`-nak és a reviewer subagenteknek.
A cél egy rövid lista, amin egy ember tud dolgozni, nem egy kimerítő felsorolás.

## Súlyosság

### Important — merge előtt javítani kell

- Hibás viselkedés: a kód nem azt csinálja, amit a `spec.md` / `plan.md` mond.
- Hatókör-eltérés: a `plan.md` által nem lefedett változás, vagy kihagyott plan-lépés.
- Security: injection (stringből összefűzött SQL, szűretlen HTML), hiányzó
  input-validáció, secret vagy személyes adat a kódban vagy a logban.
- Adat: sémaváltozás új Flyway migráció nélkül, vagy meglévő migráció szerkesztése.
- Szerződés: megváltozott API, de a `contracts/openapi.json` nincs újragenerálva, vagy
  kézzel írt kliens a generált helyett.
- Tesztek: új viselkedés teszt nélkül, átmenés kedvéért meggyengített vagy törölt teszt,
  vagy olyan teszt, ami nem tud elbukni.
- A `pnpm check` nem zöld.

### Nit — opcionális, a szerző dönt

- Elnevezés, olvashatóság, kisebb duplikáció, komment megfogalmazása.
- Egyszerűbb alternatíva egy már helyes kódra.

Reviewnként legfeljebb **5 nit**. Ha több van, a hasznosabb 5 marad, és add meg az
összes darabszámát.

## Ne jelezd

- Formázás vagy stílus, amit a linter/formatter már kikényszerít.
- Személyes ízlés konkrét haszon nélkül.
- Hibák olyan kódban, amit a változás nem érintett — kivéve, ha a változás rontja őket.
- Feltételezett problémák konkrét hibaeset nélkül.

## Találat formátuma

Találatonként egy sor:

```
[Important|Nit] path/to/file:line — mi a hiba, és a konkrét eset, amikor elromlik.
```

Minden Important találat megnevez egy konkrét hibaesetet (bemenet → hibás eredmény).
Ha nem tudsz megnevezni ilyet, akkor nem Important.

## Review kimenet

1. Verdikt: `APPROVE` (nincs Important találat) vagy `CHANGES REQUESTED`.
2. Important találatok, a legsúlyosabb elöl.
3. Nitek (max 5), utána `összesen N nit`.
4. Mit néztél át: a diff tartománya, és melyik `spec.md` / `plan.md` alapján.
