# Messaging

api-service-messaging. Sends email and SMS.

## Used by

`send-email` and `send-sms` in `operaton-workers-messaging`. See [architecture.md](../architecture.md).

## Calls

|              Call              |    Worker    |
|--------------------------------|--------------|
| `POST /{municipalityId}/email` | `send-email` |
| `POST /{municipalityId}/sms`   | `send-sms`   |

## Authentication

OAuth2 client credentials, registration `messaging`.

## Code and configuration

- Client: `operaton-workers-messaging/src/main/java/se/sundsvall/operaton/workers/messaging/`, models generated from
  `operaton-workers-messaging/src/main/resources/integrations/api-messaging.yaml`.
- Configuration: `integration.messaging` in `application.yml`.

## Behaviour worth knowing

- Both workers write the id Messaging answers with to `messageId`.

