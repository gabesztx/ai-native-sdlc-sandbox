# Review rules

Shared standard for every reviewer — human, `/review`, and the reviewer subagents.
The goal is a short list a human can act on, not an exhaustive one.

## Severity

### Important — must be fixed before merge

- Wrong behaviour: the code does not do what `spec.md` / `plan.md` says.
- Scope drift: changes not covered by `plan.md`, or plan steps left undone.
- Security: injection (SQL built by string concatenation, unsanitised HTML),
  missing input validation, secrets or PII in code or logs.
- Data: schema change without a new Flyway migration, or an edited existing migration.
- Contract: API changed but `contracts/openapi.json` not regenerated, or a
  hand-written client instead of the generated one.
- Tests: new behaviour without a test, a test weakened or deleted to pass,
  or a test that cannot fail.
- `pnpm check` not passing.

### Nit — optional, author decides

- Naming, readability, small duplication, comment wording.
- Simpler alternative for code that is already correct.

At most **5 nits** per review. If there are more, keep the 5 most useful and give
the total count.

## Do not report

- Formatting or style the linter/formatter already enforces.
- Personal preference with no concrete benefit.
- Issues in code the change did not touch — unless the change makes them worse.
- Speculative problems without a concrete failure scenario.

## Finding format

One line per finding:

```
[Important|Nit] path/to/file:line — what is wrong, and the concrete scenario where it fails.
```

Every Important finding names a concrete failure scenario (input → wrong result).
If you cannot name one, it is not Important.

## Review output

1. Verdict: `APPROVE` (no Important findings) or `CHANGES REQUESTED`.
2. Important findings, most severe first.
3. Nits (max 5), then `N nits total`.
4. What was checked: diff range, and which `spec.md` / `plan.md` was used.
