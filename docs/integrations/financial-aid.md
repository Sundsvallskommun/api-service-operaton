# FinancialAid

api-service-financial-aid. Collects a person's data from the agencies behind SSBTEK (af, csn, fk, skv, so, tns, miv)
into one financial-aid basis.

## Used by

`fetch-financial-aid-basis` in `operaton-workers-financialaid`. See [architecture.md](../architecture.md).

## Calls

|                                       Call                                       |              Purpose               |
|----------------------------------------------------------------------------------|------------------------------------|
| `GET /{municipalityId}/financial-aid?personalNumber=…&fromDate=…&toDate=…`        | The basis for one person and period |

## Authentication

OAuth2 client credentials, registration `financial-aid`.

## Code and configuration

- Client: `operaton-workers-financialaid/src/main/java/se/sundsvall/operaton/workers/financialaid/`, models
  generated from `operaton-workers-financialaid/src/main/resources/integrations/api-financialaid.yml`.
- Configuration: `integration.financial-aid` in `application.yml`.

## Behaviour worth knowing

- The answer is stored as a JSON string variable, by default `financialAidBasis`. The input `outputVariable` names
  another variable, so one model can fetch both applicant and co-applicant with the same topic.
- A blank `personalNumber` gives an empty basis without a call, so a model can run the co-applicant fetch whether or
  not there is one.
- The basis is read again by `evaluate-income-regelverk`, which classifies the incomes with the engine's DMN
  decisions. That worker calls no other service. Code: `operaton-workers-financialaid/.../rules/`.
- A String variable is stored in a `varchar(4000)` column. A basis longer than that fails the task. In the Drakel
  models a single worker therefore fetches and classifies in memory, under the topic `prepare-income-basis`, which no
  worker on `main` serves. See [todo/decisions-needed.md](../todo/decisions-needed.md).
