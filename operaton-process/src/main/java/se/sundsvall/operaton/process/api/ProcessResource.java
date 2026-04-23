package se.sundsvall.operaton.process.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.operaton.process.api.model.ModifyVariablesRequest;
import se.sundsvall.operaton.process.api.model.ProcessDefinitionResponse;
import se.sundsvall.operaton.process.api.model.ProcessDefinitionsResponse;
import se.sundsvall.operaton.process.api.model.ProcessInstanceResponse;
import se.sundsvall.operaton.process.api.model.ProcessInstancesResponse;
import se.sundsvall.operaton.process.api.model.StartProcessInstanceRequest;
import se.sundsvall.operaton.process.service.ProcessService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.http.ResponseEntity.noContent;
import static org.springframework.http.ResponseEntity.ok;

@Tag(name = "Processes", description = "Manage deployed process definitions and running process instances")
@Validated
@RestController
@RequestMapping("/{municipalityId}")
@ApiResponse(responseCode = "400",
	description = "Bad Request",
	content = @Content(
		mediaType = APPLICATION_PROBLEM_JSON_VALUE,
		schema = @Schema(oneOf = {
			Problem.class, ConstraintViolationProblem.class
		})))
@ApiResponse(responseCode = "500",
	description = "Internal Server Error",
	content = @Content(
		mediaType = APPLICATION_PROBLEM_JSON_VALUE,
		schema = @Schema(implementation = Problem.class)))
class ProcessResource {

	private final ProcessService processService;

	ProcessResource(final ProcessService processService) {
		this.processService = processService;
	}

	@Operation(summary = "List the latest version of all deployed process definitions, optionally filtered by name", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
	})
	@GetMapping(value = "/process-definitions", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<ProcessDefinitionsResponse> getProcessDefinitions(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "name", description = "Filter by process definition name (latest version that matches)") @RequestParam(name = "name", required = false) final String name) {

		return ok(processService.getProcessDefinitions(name));
	}

	@Operation(summary = "Get a specific process definition by ID", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	@GetMapping(value = "/process-definitions/{id}", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<ProcessDefinitionResponse> getProcessDefinition(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "id", description = "Process definition id", example = "send-email-process:1:4") @PathVariable final String id) {

		return ok(processService.getProcessDefinition(id));
	}

	@Operation(summary = "Get the BPMN XML for a specific process definition", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation"),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	@GetMapping(value = "/process-definitions/{id}/xml", produces = APPLICATION_XML_VALUE)
	ResponseEntity<byte[]> getProcessDefinitionXml(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "id", description = "Process definition id", example = "send-email-process:1:4") @PathVariable final String id) {

		return ok(processService.getProcessModel(id));
	}

	@Operation(summary = "Start a new process instance by process definition key, optionally with variables", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
	})
	@PostMapping(value = "/process-instances", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	ResponseEntity<ProcessInstanceResponse> startProcessInstance(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Valid @RequestBody final StartProcessInstanceRequest request) {

		return ok(processService.startProcessInstance(request));
	}

	@Operation(summary = "List active process instances", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
	})
	@GetMapping(value = "/process-instances", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<ProcessInstancesResponse> getProcessInstances(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId) {

		return ok(processService.getProcessInstances());
	}

	@Operation(summary = "Get status and details for a specific process instance", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	@GetMapping(value = "/process-instances/{id}", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<ProcessInstanceResponse> getProcessInstance(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "id", description = "Process instance id") @PathVariable final String id) {

		return ok(processService.getProcessInstance(id));
	}

	@Operation(summary = "Modify variables (add, update, delete) on a running process instance", responses = {
		@ApiResponse(responseCode = "204", description = "Variables modified"),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	@PostMapping(value = "/process-instances/{id}/variables", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> modifyProcessInstanceVariables(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "id", description = "Process instance id") @PathVariable final String id,
		@Valid @RequestBody final ModifyVariablesRequest request) {

		processService.modifyProcessInstanceVariables(id, request);
		return noContent().build();
	}
}
