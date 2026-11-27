# Decisions needed

Behaviour that looks wrong but needs a decision before anyone changes it.

## This branch and `main` serve different topics

This branch adds `prepare-income-basis` and `create-errand-notification`, which the financial assistance models in
Drakel use. It removes `prepare-normberakning`, `commit-normberakning`, `create-application-normberakning`,
`fetch-lifecare-supplements`, `fetch-financial-aid-basis` and `evaluate-income-regelverk`, which are still on
`main`. A model built for one fails at the missing task on an engine built from the other. The pull requests that
would have merged this branch, #31 and #32, were closed without merging.

Decide whether this branch goes to `main`, and whether any model still running on `main` needs the removed topics.

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
put on the route on 2026-10-01. `application.yml` no longer creates an admin user, so an environment that wants one
sets it itself.

Decide what each environment exposes.
