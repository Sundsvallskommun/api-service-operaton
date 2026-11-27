# FinancialAid

api-service-financial-aid. Collects a person's data from the agencies behind SSBTEK (af, csn, fk, skv, so, tns, miv)
into one financial-aid basis.

## Used by

`prepare-income-basis` in `operaton-workers-ekonomisktbistand`. See [architecture.md](../architecture.md).

## Calls

|                                    Call                                    |               Purpose               |
|----------------------------------------------------------------------------|-------------------------------------|
| `GET /{municipalityId}/financial-aid?personalNumber=…&fromDate=…&toDate=…` | The basis for one person and period |

## Authentication

OAuth2 client credentials, registration `financial-aid`.

## Code and configuration

- Client: `operaton-workers-financialaid/src/main/java/se/sundsvall/operaton/workers/financialaid/`, models
  generated from `operaton-workers-financialaid/src/main/resources/integrations/api-financialaid.yml`.
- Income rules: `operaton-workers-financialaid/src/main/java/se/sundsvall/operaton/workers/financialaid/rules/`.
- Configuration: `integration.financial-aid` in `application.yml`.

## Behaviour worth knowing

- The basis never becomes a process variable. `prepare-income-basis` fetches it for the applicant, the co-applicant
  and the household's children, classifies the incomes in memory and posts the result to CareManagement. A basis is
  larger than the 4000 characters a string variable holds, and the engine's history would keep it.
- The income rules evaluate the DMN decisions `Decision_inkomstRalista`, `Decision_inkomstTroskel` and
  `Decision_ssbtekSvarKvalitet`, which are deployed to the engine like any other model. The seasonal studiehjälp rule
  for September is in code, `rules/StudiehjalpSeptemberRule.java`.
- A failed read is retried like any other task failure. When the last attempt also fails, or an income-bearing
  agency answers that it could not deliver, the worker skips the income rules. It still posts to CareManagement, with
  blank incomes and `ssbtekError` true so the previous run's incomes stay, and writes `ssbtekError` for the model to
  branch on.
- A household child that CareManagement names without a personal number is not read. The worker reports it to
  CareManagement as an unhandled income so the caseworker knows the basis is incomplete.

