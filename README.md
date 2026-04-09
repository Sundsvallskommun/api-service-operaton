# Operaton

_This service provides an Operaton BPMN process engine with APIs for managing deployments, process instances and
external task topics_

## Getting Started

### Prerequisites

- **Java 25 or higher**
- **Maven**
- **Git**

### Installation

1. **Clone the repository:**

   ```bash
   git clone https://github.com/Sundsvallskommun/api-service-operaton.git
   cd api-service-operaton
   ```
2. **Configure the application:**

   Before running the application, you need to set up configuration settings.
   See [Configuration](#configuration)

   **Note:** Ensure all required configurations are set; otherwise, the application may fail to start.

3. **Build and run the application:**

   ```bash
   mvn spring-boot:run
   ```

## API Documentation

Access the API documentation via Swagger UI:

- **Swagger UI:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

## Usage

### API Endpoints

See the [API Documentation](#api-documentation) for detailed information on available endpoints.

### Example Request

```bash
curl -X 'GET' \
  'http://localhost:8080/2281/deployments'
```

## Configuration

Configuration is crucial for the application to run successfully. Ensure all necessary settings are configured in
`application.yml`.

### Key Configuration Parameters

- **Server Port:**

  ```yaml
  server:
    port: 8080
  ```
- **Operaton Admin User:**

  ```yaml
  operaton.bpm:
    admin-user:
      id: demo
      password: demo
      firstName: Demo
  ```
- **Scheduler:**

  ```yaml
  scheduler:
    logger-worker:
      cron: "*/5 * * * * *"
  ```

### Additional Notes

- **Application Profiles:**

  Use Spring profiles (`dev`, `prod`, etc.) to manage different configurations for different environments.

- **Logging Configuration:**

  Adjust logging levels if necessary.

## Contributing

Contributions are welcome! Please
see [CONTRIBUTING.md](https://github.com/Sundsvallskommun/.github/blob/main/.github/CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the [MIT License](LICENSE).

## Status

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-operaton&metric=alert_status)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-operaton)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-operaton&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-operaton)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-operaton&metric=security_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-operaton)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-operaton&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-operaton)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-operaton&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-operaton)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-operaton&metric=bugs)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-operaton)

---

Copyright (c) 2026 Sundsvalls kommun
