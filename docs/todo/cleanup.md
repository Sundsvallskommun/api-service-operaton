# Cleanup

Code and documentation that work but should be simpler or more accurate.

## Code

- `WorkersAutoConfiguration` scans all of `se.sundsvall.operaton.workers` and excludes `messaging` and
  `supportmanagement`, but not `caremanagement` or `financialaid`. Those two are found by both scans. Either exclude
  every worker subpackage or scan only `framework`, `api` and `service`.
  Code: `operaton-workers/src/main/java/se/sundsvall/operaton/workers/WorkersAutoConfiguration.java`.

## Documentation

- `README.md` lists 4 of the 15 topics under Available Worker Topics. The live catalog is `GET /topics` and the
  overview is in `docs/architecture.md`, so the table should go or point there.
- `README.md` names Messaging and SupportManagement under Dependencies, but not CareManagement or FinancialAid.
- `README.md` Configuration does not describe `integration.care-management`, `integration.financial-aid`,
  `operaton.cors.allowed-origins` or the `scheduler.<topic>.cron` key every worker reads.
- `docs/README.md` has no link to the Confluence service page.
