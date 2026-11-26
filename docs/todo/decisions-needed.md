# Decisions needed

Behaviour that looks wrong but needs a decision before anyone changes it.

## `main` and the Drakel branch serve different topics

The financial assistance models deployed in Drakel use `prepare-income-basis` and `create-errand-notification`.
No worker on `main` serves either, so those models stop at that task on an engine built from `main`. In the other
direction, `prepare-normberakning`, `commit-normberakning`, `create-application-normberakning`,
`fetch-lifecare-supplements` and `evaluate-income-regelverk` are on `main` but the current Drakel models no longer
use them.

Decide whether the Drakel workers come to `main`, and which of the older ones go. TODO: the Drakel code is on a
`drakel` branch that is not fetched here; check it on GitHub.

## Where the models live

No BPMN or DMN file is kept here, and the financial assistance models exist only in the Drakel engine and a working
copy in the team handover folder. The engine has been the master since 2026-10-05, edited through a modeler. Decide on
a repository for them and how it deploys.

## Every deploy makes a new version

`POST /deployments` does not turn on the engine's duplicate filtering, so deploying an unchanged file still creates a
new version. A pipeline that deploys every model on every run would bump every version each time. The endpoint also
takes one file, so a BPMN file and the DMN it calls cannot go out in one deployment.

Code: `operaton-deployment/src/main/java/se/sundsvall/operaton/deployment/service/DeploymentService.java`.

## `municipalityId` is validated and then ignored

Every path takes `{municipalityId}`, but deployments, definitions, instances and topics are global. Two municipalities
on the same engine see and can delete each other's models. Decide whether the engine is shared or separated per
municipality, for example with Operaton's tenant id.

## What protects the engine's own REST API and Cockpit

dept44's security chain permits every request, so the starter's REST API and the webapps answer to anyone who
reaches the service. In Drakel `/engine-rest` and `/2281/deployments` were open to the internet until basic auth was
put on the route on 2026-10-01. `application.yml` also creates the admin user `demo` with password `demo` by
default.

Decide what each environment exposes, and whether the default admin user stays.
