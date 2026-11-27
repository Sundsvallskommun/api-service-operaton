# CareManagement

api-service-caremanagement. Owns the care errands, and makes every Lifecare read and write on behalf of the
financial assistance processes.

## Used by

The workers in `operaton-workers-caremanagement`: `create-care-errand`, `update-errand`, `add-errand-decision`,
`create-actualisation`, `check-payment-status` and `create-errand-notification`. Also `prepare-income-basis` in
`operaton-workers-ekonomisktbistand`. See [architecture.md](../architecture.md).

## Calls

All under `/{municipalityId}/{namespace}/errands`.

|                            Call                             |            Worker            |
|-------------------------------------------------------------|------------------------------|
| `POST /`                                                    | `create-care-errand`         |
| `GET /{errandId}`                                           | `create-errand-notification` |
| `PATCH /{errandId}`                                         | `update-errand`              |
| `POST /{errandId}/decisions`                                | `add-errand-decision`        |
| `POST /{errandId}/notifications`                            | `create-errand-notification` |
| `POST /financial-assistance/actualisation`                  | `create-actualisation`       |
| `GET /financial-assistance/{errandId}/household-identifiers` | `prepare-income-basis`      |
| `POST /financial-assistance/calculation/prepare`            | `prepare-income-basis`       |
| `POST /financial-assistance/payment-status`                 | `check-payment-status`       |

## Authentication

OAuth2 client credentials, registration `care-management`.

## Code and configuration

- Client: `operaton-workers-caremanagement/src/main/java/se/sundsvall/operaton/workers/caremanagement/`, models
  generated from `operaton-workers-caremanagement/src/main/resources/integrations/api-caremanagement.yml`.
- Configuration: `integration.care-management` in `application.yml`.

## Behaviour worth knowing

- Every call carries `X-Sent-By: operaton; type=custom`, and care-management records `operaton` as the actor in
  the errand's event log. Only this client sends the header. See `configuration/CareManagementConfiguration.java`.
- `check-payment-status` makes no payment. Draken registers the payments in Lifecare, and care-management reads
  whether the errand's own payments have gone out. Another errand's payment for the same person and month does not
  count. The worker takes the errand from `errandId`, or from the business key when the model maps no `errandId`.
- `check-payment-status` also writes `paymentStatusDetail`, care-management's reason the payments are still pending,
  and `paymentOverdue`, true once care-management's deadline after the decision has passed. A model can then notify
  the caseworker with `create-errand-notification`.
- `create-errand-notification` reads the errand first and sends the notification to its `assignedUserId`, so a
  reassignment while the process waited is honoured. An errand with no assigned caseworker becomes an incident at
  once. The notification text is a process variable and stays in the engine's history, so it must not carry personal
  data or case content.
- `prepare-income-basis` reads the household's personal numbers from `household-identifiers` on every run instead of
  taking them as process variables.
