# Operaton

_Provides an Operaton BPMN process engine with APIs for managing deployments, process instances and a self-documenting catalog of external task workers (send-email, send-sms, create-errand) that can be used as building blocks in BPMN process models._

## Getting Started

### Prerequisites

- **Java 25 or higher**
- **Maven**
- **MariaDB**
- **Git**
- **[Dependent Microservices](#dependencies)**

### Installation

1. **Clone the repository:**

   ```bash
   git clone git@github.com:Sundsvallskommun/api-service-operaton.git
   cd api-service-operaton
   ```
2. **Configure the application:**

   Before running the application, you need to set up configuration settings.
   See [Configuration](#Configuration)

   **Note:** Ensure all required configurations are set; otherwise, the application may fail to start.

3. **Ensure dependent services are running:**

   If this microservice depends on other services, make sure they are up and accessible. See [Dependencies](#dependencies) for more details.

4. **Build and run the application:**

   ```bash
   mvn spring-boot:run
   ```

## Dependencies

This microservice depends on the following services:

- **Messaging**
  - **Purpose:** Sends email and SMS on behalf of BPMN processes via the `send-email` and `send-sms` external task workers.
  - **Repository:** [api-service-messaging](https://github.com/Sundsvallskommun/api-service-messaging)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.
- **SupportManagement**
  - **Purpose:** Creates errands from BPMN processes via the `create-errand` external task worker.
  - **Repository:** [api-service-support-management](https://github.com/Sundsvallskommun/api-service-support-management)
  - **Setup Instructions:** Refer to its documentation for installation and configuration steps.

Ensure that these services are running and properly configured before starting this microservice.

## API Documentation

Access the API documentation via Swagger UI:

- **Swagger UI:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

## Usage

### API Endpoints

Refer to the [API Documentation](#api-documentation) for detailed information on available endpoints. The service exposes four endpoint groups, all scoped by `{municipalityId}`:

- **Deployments** (`/{municipalityId}/deployments`) — upload and list BPMN/DMN files
- **Processes** (`/{municipalityId}/process-definitions`, `/{municipalityId}/process-instances`) — start and inspect process instances
- **Topics** (`/{municipalityId}/topics`) — catalog of available external task workers with their input/output variable contracts

### Example Request

```bash
curl -X GET http://localhost:8080/2281/topics
```

### Available Worker Topics

External task workers are registered automatically at startup via the `@TopicWorker` annotation and become available as reusable building blocks in BPMN process models. The live catalog is always reachable at `GET /{municipalityId}/topics`.

|      Topic      |               Purpose               |                                                  Input variables                                                  | Output variables |
|-----------------|-------------------------------------|-------------------------------------------------------------------------------------------------------------------|------------------|
| `send-email`    | Send email via the Messaging API    | `municipalityId`, `emailAddress`, `subject`, `message`, `senderName`, `senderAddress`                             | `messageId`      |
| `send-sms`      | Send SMS via the Messaging API      | `municipalityId`, `mobileNumber`, `message`, `sender`                                                             | `messageId`      |
| `create-errand` | Create errand in SupportManagement  | `municipalityId`, `namespace`, `title`, `priority`, `status`, `reporterUserId`, `category`, `type`, `description` | `errandId`       |
| `log-message`   | Example worker that logs a variable | `message`                                                                                                         | —                |

## Configuration

Configuration is crucial for the application to run successfully. Ensure all necessary settings are configured in `application.yml`.

### Key Configuration Parameters

- **Server Port:**

  ```yaml
  server:
    port: 8080
  ```
- **Database Settings:**

  ```yaml
  spring:
    datasource:
      url: jdbc:mariadb://localhost:3306/your_database
      username: your_db_username
      password: your_db_password
  ```
- **Operaton Admin User:**

  ```yaml
  operaton.bpm:
    admin-user:
      id: demo
      password: demo
      firstName: Demo
  ```
- **External Service URLs:**

  ```yaml
  integration:
    messaging:
      url: https://your_service_url/messaging
      connect-timeout: 5
      read-timeout: 30
    support-management:
      url: https://your_service_url/supportmanagement
      connect-timeout: 5
      read-timeout: 30

  spring:
    security:
      oauth2:
        client:
          provider:
            messaging:
              token-uri: https://token_url
            support-management:
              token-uri: https://token_url
          registration:
            messaging:
              client-id: the-client-id
              client-secret: the-client-secret
            support-management:
              client-id: the-client-id
              client-secret: the-client-secret
  ```
- **Scheduler:**

  ```yaml
  scheduler:
    logger-worker:
      cron: "*/5 * * * * *"
  ```

### Database Initialization

The project is set up with [Flyway](https://github.com/flyway/flyway) for database migrations. Flyway is enabled by default and will populate the required schema (including the `shedlock` table used by scheduled workers) on application startup.

```yaml
spring:
  flyway:
    enabled: true
```

**Note:** The Operaton engine requires the database to use `READ_COMMITTED` transaction isolation. When sharing a MariaDB instance across services where changing the global isolation level is not possible, either set it per-session via the JDBC URL (`?sessionVariables=tx_isolation='READ-COMMITTED'`) or skip the check with:

```yaml
operaton.bpm:
  generic-properties:
    properties:
      skip-isolation-level-check: true
```

### Additional Notes

- **Application Profiles:**

  Use Spring profiles (`dev`, `prod`, etc.) to manage different configurations for different environments.

- **Logging Configuration:**

  Adjust logging levels if necessary.

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](https://github.com/Sundsvallskommun/.github/blob/main/.github/CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the [MIT License](LICENSE).

## Code status

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-operaton&metric=alert_status)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-operaton)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-operaton&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-operaton)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-operaton&metric=security_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-operaton)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-operaton&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-operaton)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-operaton&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-operaton)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-operaton&metric=bugs)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-operaton)

---

© 2026 Sundsvalls kommun
