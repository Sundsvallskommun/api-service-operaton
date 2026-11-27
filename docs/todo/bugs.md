# Bugs

## An invalid model is answered with 500

`DeploymentService.deploy` turns every `ProcessEngineException` into 500 "Failed to deploy BPMN file", also when the
file does not parse or fails the engine's validation. That is the client's error, not the service's. The message
also says BPMN for a DMN file. `DeploymentIT` test02 asserts the 500 today.

Code: `operaton-deployment/src/main/java/se/sundsvall/operaton/deployment/service/DeploymentService.java`.

Done when an invalid BPMN or DMN file answers 400 with the engine's message, and test02 asserts that.
