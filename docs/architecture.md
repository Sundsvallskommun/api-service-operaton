# Architecture

api-service-operaton runs an embedded Operaton BPMN and DMN engine for the Sundsvall microservice landscape. Clients
deploy process and decision models at runtime, start and steer process instances, and evaluate decisions through a
REST API. The service tasks in those models are external tasks, and the workers that serve them run in this service
too, calling other api-service-* services.

TODO: list the services that call this one. Known from the Drakel work: api-service-caremanagement starts the
financial assistance processes and correlates messages to them.

## Modules

|                Module                |                                                                                      Role                                                                                      |
|--------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `operaton-app`                       | The Spring Boot application. Pulls in every other module, holds `application.yml`, the Flyway migrations and the integration tests                                             |
| `operaton-core`                      | The engine (`operaton-bpm-spring-boot-starter-rest`) and the optional CORS configuration                                                                                       |
| `operaton-deployment`                | `/deployments`: upload, list and delete BPMN and DMN files                                                                                                                     |
| `operaton-process`                   | `/process-definitions`, `/process-instances`, `/message`                                                                                                                       |
| `operaton-decision`                  | `/decision-definitions`: read and evaluate DMN decisions                                                                                                                       |
| `operaton-workers`                   | The worker framework (`AbstractTopicWorker`, `@TopicWorker`, `TopicRegistry`), the topic catalog at `/topics` and the example `log-message` worker                              |
| `operaton-workers-<integration>`     | One module per external system: its Feign client, its configuration and the workers that call it. See [integrations/](integrations/)                                           |
| `operaton-webapp`                    | Operaton Cockpit, Tasklist and Admin (`operaton-bpm-spring-boot-starter-webapp`). Leave the dependency out of `operaton-app` to run without them                               |

Each module registers itself through `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
and a `*AutoConfiguration` class that component-scans its own package.

## Cross-cutting rules

- **Every path is scoped by `{municipalityId}`, but the engine is shared.** The id is validated and then not used.
  Deployments, definitions and instances are global to the engine.
- **Service tasks are external tasks.** A model names a topic, and the worker for that topic polls the engine for it.
  Nothing in a model calls Java code directly.
- **A topic has exactly one worker.** `TopicRegistry` fails startup when two `@TopicWorker` beans declare the same
  topic.
- **A failed task becomes an incident at once.** `AbstractTopicWorker.processTasks` reports the failure with zero
  retries, so the instance stops at that task and shows up in Cockpit. A missing or wrongly typed required variable
  fails the same way.
- **Request and response bodies are never logged.** `PayloadLoggerSilencer` in `operaton-app` turns dept44's payload
  logger off, since the workers carry personnummer and agency data.
- **Calls to care-management are signed as the engine.** They carry `X-Sent-By: operaton; type=custom`, which
  care-management writes into its errand event log. See [care-management](integrations/care-management.md).
- **The engine requires `READ_COMMITTED`.** The Hikari pool sets it, see Configuration in `README.md`.

## Endpoints

All paths are under `/{municipalityId}`.

|                         Endpoint                          |                                         Engine call                                          |
|-----------------------------------------------------------|----------------------------------------------------------------------------------------------|
| `POST /deployments`                                       | `RepositoryService.createDeployment`, one file per deployment                                |
| `GET /deployments`, `DELETE /deployments/{id}`            | Deployment query, `deleteDeployment` (`cascade=true` also deletes running instances)         |
| `GET /process-definitions`, `GET /process-definitions/{id}`, `GET /process-definitions/{id}/xml` | Latest version of each definition, one definition, its BPMN XML |
| `POST /process-instances`                                 | `RuntimeService.startProcessInstanceByKey` with business key and variables                    |
| `GET /process-instances`, `GET /process-instances/{id}`   | Active instances, one instance                                                               |
| `POST /process-instances/{id}/variables`                  | `setVariables` and `removeVariables`                                                         |
| `POST /message`                                           | Message correlation by name and business key to every waiting instance. 404 when none waits  |
| `GET /decision-definitions`, `GET /decision-definitions/{id}`, `GET /decision-definitions/{id}/xml` | Latest version of each decision, one decision, its DMN XML |
| `POST /decision-definitions/evaluate`                     | `DecisionService.evaluateDecisionByKey` on the latest version                                |
| `GET /topics`, `GET /topics/{topic}`, `GET /topics/templates` | `TopicRegistry`: the worker catalog, and the same as bpmn-js element templates           |

The starter also serves Operaton's own REST API and, with `operaton-webapp`, Cockpit. TODO: confirm the paths
(`/engine-rest`, `/operaton/app`) and what protects them in each environment.

## Worker topics

`GET /{municipalityId}/topics` lists the input and output variables of each topic.

|               Topic                |                                Calls                                 |                                     Does                                      |
|------------------------------------|----------------------------------------------------------------------|-------------------------------------------------------------------------------|
| `log-message`                      | Nothing                                                              | Logs a variable. Example worker                                               |
| `send-email`, `send-sms`           | [Messaging](integrations/messaging.md)                               | Sends an email or SMS                                                         |
| `create-support-errand`            | [SupportManagement](integrations/support-management.md)              | Creates an errand                                                             |
| `create-care-errand`, `update-errand`, `add-errand-decision` | [CareManagement](integrations/care-management.md) | Creates or updates an errand, records a decision on it                  |
| `create-actualisation`             | CareManagement                                                       | Creates the Lifecare actualisation for a financial assistance errand          |
| `prepare-normberakning`            | CareManagement                                                       | Reports whether the incomes are complete and records the income warnings      |
| `commit-normberakning`             | CareManagement                                                       | Creates the Lifecare normberäkning after a decision                           |
| `create-application-normberakning` | CareManagement                                                       | Creates the Lifecare normberäkning from the application                       |
| `check-payment-status`             | CareManagement                                                       | Reads whether the Lifecare payment has been made                              |
| `fetch-lifecare-supplements`       | CareManagement                                                       | Queues an RPA fetch of Lifecare journal notes and documents                   |
| `fetch-financial-aid-basis`        | [FinancialAid](integrations/financial-aid.md)                        | Fetches a person's SSBTEK basis into a JSON variable                          |
| `evaluate-income-regelverk`        | The engine's own DMN decisions                                       | Classifies SSBTEK incomes with `Decision_inkomstRalista` and `Decision_inkomstTroskel` |

### Adding a worker

1. Put it in the module of the system it calls, or in a new `operaton-workers-<integration>` module added to
   `operaton-app/pom.xml`.
2. Extend `AbstractTopicWorker` and implement `handle`, which returns the output variables.
3. Annotate the class with `@TopicWorker` (topic, description, input and output variables) and `@Component`.
4. Call `processTasks()` from a method annotated with `@Dept44Scheduled`, with the cron read from
   `scheduler.<topic>.cron`.

Each worker fetches up to 10 tasks per run and locks them for 60 seconds. ShedLock, through `@Dept44Scheduled`, keeps
two instances of the service from polling the same topic at once.

## Deploying models

The repository holds no models. `META-INF/processes.xml` is empty and no `.bpmn` or `.dmn` file is on the classpath,
so nothing deploys at startup. Models reach the engine through `POST /{municipalityId}/deployments`, or from a modeler
through the engine's own REST API.

- Deploying a file whose process or decision key already exists creates a new version. New instances start on the
  latest version and running instances stay on theirs until someone migrates them.
- The deployment name matters to tooling that looks deployments up by name, not to the engine.
- Deleting a decision from a DMN file and deploying the file again does not remove it from the engine. The
  older deployment still holds it, and it stays the latest version of that key.
- `evaluate-income-regelverk` evaluates `Decision_inkomstRalista` and `Decision_inkomstTroskel` by key, so a DMN with
  those keys must be deployed before the worker runs.
