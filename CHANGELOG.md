# Changelog

Every change to the service, newest date first. One short line per change.

## 2026-11-26

- Add `docs/` with architecture and integration notes, `AGENTS.md` and this changelog.

## 2026-10-09

- Bump dept44-service-parent to 8.0.12.

## 2026-10-08

- Bump dept44-service-parent to 8.0.11.

## 2026-09-29

- Stop creating the `demo` admin user by default.

## 2026-09-25

- Read SSBTEK for the household's children in `prepare-income-basis`.

## 2026-09-24

- Remove the `prepare-normberakning`, `commit-normberakning`, `create-application-normberakning` and
  `fetch-lifecare-supplements` workers.
- Read the household's personal numbers from CareManagement in `prepare-income-basis`.
- Bump Operaton to 2.1.5.

## 2026-09-23

- Add the `create-errand-notification` worker.
- Let `check-payment-status` check the errand's own payments and write `paymentStatusDetail` and `paymentOverdue`.
- Send CareManagement the facts for the dagersättning day check.

## 2026-09-22

- Add the `prepare-income-basis` worker and remove `fetch-financial-aid-basis` and `evaluate-income-regelverk`.
- Fail with the variable's name when a string process variable is longer than 4000 characters.
- Keep the variables in `operaton.history.suppressed-variables` out of the engine's history.
- Apply the seasonal studiehjälp rule for September.

## 2026-09-21

- Retry a failed external task 5 times with backoff before raising an incident.
- Read FK, PM and CSN income correctly from the SSBTEK basis.
- Skip the income rules when SSBTEK could not be read.
- Stop reporting a vanished benefit as a -100 % change.
- Apply the business answers of 2026-09-21 to the income rules.

## 2026-09-17

- Classify how reliable each SSBTEK answer is with `Decision_ssbtekSvarKvalitet`, and stop truncating day counts.
- Compare income sums exactly when the DMN threshold is 0.

## 2026-09-15

- Mark which classified incomes came from the comparison period.

## 2026-09-11

- Read the sub-benefit, amount type, period and days that FK sends.

## 2026-09-03

- Bump dept44-service-parent to 8.0.10.

## 2026-08-27

- Bump Operaton to 2.1.4.

## 2026-08-20

- Bump dept44-service-parent to 8.0.9.

## 2026-07-30

- Bump Operaton to 2.1.3.

## 2026-07-10

- Rework the income rules classes.

## 2026-07-09

- Bump dept44-service-parent to 8.0.8.

## 2026-07-06

- Take the decision key of `POST /decision-definitions/evaluate` as a request parameter.

## 2026-06-26

- Rename the topic `create-errand` to `create-care-errand` for CareManagement and `create-support-errand` for SupportManagement.
- Register the SupportManagement worker, which never started before.
- Fail startup when two workers declare the same topic.

## 2026-06-25

- Remove the `update-errand-parameter` worker.
- Bump Operaton to 2.1.2.

## 2026-06-24

- Add the `create-application-normberakning` worker for the nyansökan process.

## 2026-06-23

- Point `prepare-normberakning` and `commit-normberakning` at CareManagement's `/calculation` paths.
- Let `fetch-lifecare-supplements` queue an RPA fetch through CareManagement.
- Send `X-Sent-By: operaton` on every call to CareManagement.

## 2026-06-17

- Split the normberäkning worker into `prepare-normberakning` and `commit-normberakning`.
- Let `fetch-financial-aid-basis` write to a variable the model names, and skip the call for a blank personnummer.
- Add the `evaluate-income-regelverk` worker, which classifies SSBTEK incomes with the engine's DMN decisions.
- Add `POST /decision-definitions/evaluate`.
- Let `check-payment-status` read the payment status from CareManagement.
- Let `create-actualisation` create the Lifecare actualisation through CareManagement.

## 2026-06-16

- Add the Spin JSON data format so the engine can store typed object variables.
- Add the `create-normberakning`, `create-actualisation` and `check-payment-status` workers.

## 2026-06-10

- Add a Dockerfile that builds from source.

## 2026-05-28

- Bump Operaton to 2.1.1.
- Bump dept44-service-parent to 8.0.7.
- Bump jaxb-impl to 4.0.9.

## 2026-05-07

- Bump jaxb-impl to 4.0.8.

## 2026-05-20

- Add the `fetch-financial-aid-basis` worker (HYDRAN-2167).
- Stop logging request and response bodies.

## 2026-04-30

- Bump Operaton to 2.1.0.

## 2026-04-28

- Add `POST /message` and the `add-errand-decision` and `update-errand-parameter` workers (HYDRAN-2038).

## 2026-04-23

- Add `POST /process-instances/{id}/variables` (HYDRAN-2004).
- Bump jaxb-impl to 4.0.7.

## 2026-04-22

- Add the CareManagement workers `create-errand` and `update-errand` (HYDRAN-1990).

## 2026-04-20

- Split the service into modules (HYDRAN-1958).

## 2026-04-16

- Add DMN deployment and the `/decision-definitions` endpoints (HYDRAN-1959).

## 2026-04-09

- Add the worker framework, the `/topics` catalog and the `send-email`, `send-sms` and `create-errand` workers (HYDRAN-1898).
- Create the service with deployments and process endpoints (HYDRAN-1880).
- Bump dept44-service-parent to 8.0.6.
