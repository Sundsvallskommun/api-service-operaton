package se.sundsvall.operaton.decision.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.operaton.decision.api.model.DecisionDefinitionResponse;
import se.sundsvall.operaton.decision.api.model.DecisionDefinitionsResponse;
import se.sundsvall.operaton.decision.service.DmnService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.http.ResponseEntity.ok;

@Tag(name = "Decisions", description = "Manage deployed DMN decision definitions")
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
class DecisionResource {

	private final DmnService decisionService;

	DecisionResource(final DmnService decisionService) {
		this.decisionService = decisionService;
	}

	@Operation(summary = "List the latest version of all deployed decision definitions", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
	})
	@GetMapping(value = "/decision-definitions", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<DecisionDefinitionsResponse> getDecisionDefinitions(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId) {

		return ok(decisionService.getDecisionDefinitions());
	}

	@Operation(summary = "Get a specific decision definition by ID", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	@GetMapping(value = "/decision-definitions/{id}", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<DecisionDefinitionResponse> getDecisionDefinition(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "id", description = "Decision definition id", example = "approve-loan:1:5") @PathVariable final String id) {

		return ok(decisionService.getDecisionDefinition(id));
	}

	@Operation(summary = "Get the DMN XML for a specific decision definition", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation"),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	@GetMapping(value = "/decision-definitions/{id}/xml", produces = APPLICATION_XML_VALUE)
	ResponseEntity<byte[]> getDecisionDefinitionXml(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "id", description = "Decision definition id", example = "approve-loan:1:5") @PathVariable final String id) {

		return ok(decisionService.getDecisionModel(id));
	}
}
