# CareManagement

api-service-caremanagement. Owns the care errands, and makes every Lifecare read and write on behalf of the
financial assistance processes.

## Used by

The workers in `operaton-workers-caremanagement`: `create-care-errand`, `update-errand`, `add-errand-decision`,
`create-actualisation`, `prepare-normberakning`, `commit-normberakning`, `create-application-normberakning`,
`check-payment-status` and `fetch-lifecare-supplements`. See [architecture.md](../architecture.md).

## Calls

All under `/{municipalityId}/{namespace}/errands`.

|                         Call                          |                      Worker                       |
|-------------------------------------------------------|---------------------------------------------------|
| `POST /`                                              | `create-care-errand`                              |
| `PATCH /{errandId}`                                   | `update-errand`                                   |
| `POST /{errandId}/decisions`                          | `add-errand-decision`                             |
| `POST /financial-assistance/actualisation`            | `create-actualisation`                            |
| `POST /financial-assistance/calculation/prepare`      | `prepare-normberakning`                           |
| `POST /financial-assistance/calculation/commit`       | `commit-normberakning`                            |
| `POST /financial-assistance/calculation/from-application` | `create-application-normberakning`            |
| `POST /financial-assistance/payment-status`           | `check-payment-status`                            |
| `POST /{errandId}/rpa-tasks`                          | `fetch-lifecare-supplements`, action `FETCH_SUPPLEMENTS` |

## Authentication

OAuth2 client credentials, registration `care-management`.

## Code and configuration

- Client: `operaton-workers-caremanagement/src/main/java/se/sundsvall/operaton/workers/caremanagement/`, models
  generated from `operaton-workers-caremanagement/src/main/resources/integrations/api-caremanagement.yml`.
- Configuration: `integration.care-management` in `application.yml`.

## Behaviour worth knowing

- Every call carries `X-Sent-By: operaton; type=custom`, and care-management records `operaton` as the actor in
  the errand's event log. Only this client sends the header. See `configuration/CareManagementConfiguration.java`.
- `check-payment-status` makes no payment. A caseworker pays in Lifecare, and the worker only reads whether it has
  happened.
- `fetch-lifecare-supplements` only queues the RPA task. A robot writes the result back to the errand through
  care-management.

TODO: the Drakel models also use the topic `create-errand-notification`, which no worker on `main` serves. See
[todo/decisions-needed.md](../todo/decisions-needed.md).
