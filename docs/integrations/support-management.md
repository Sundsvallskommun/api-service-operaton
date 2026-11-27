# SupportManagement

api-service-support-management. Holds support errands.

## Used by

`create-support-errand` in `operaton-workers-supportmanagement`. See [architecture.md](../architecture.md).

## Calls

|                     Call                     |         Worker          |
|----------------------------------------------|-------------------------|
| `POST /{municipalityId}/{namespace}/errands` | `create-support-errand` |

## Authentication

OAuth2 client credentials, registration `support-management`.

## Code and configuration

- Client: `operaton-workers-supportmanagement/src/main/java/se/sundsvall/operaton/workers/supportmanagement/`,
  models generated from `operaton-workers-supportmanagement/src/main/resources/integrations/api-support-management.yaml`.
- Configuration: `integration.support-management` in `application.yml`.

## Behaviour worth knowing

- The created errand's id is written to `errandId`.

