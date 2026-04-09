package se.sundsvall.operaton.api;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.operaton.api.model.DeploymentResponse;
import se.sundsvall.operaton.api.model.DeploymentsResponse;
import se.sundsvall.operaton.service.DeploymentService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.http.ResponseEntity.ok;

@Tag(name = "Deployments", description = "Deploy and manage BPMN process definitions and DMN decision tables")
@Validated
@RestController
@RequestMapping("/{municipalityId}/deployments")
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
class DeploymentResource {

	private final DeploymentService deploymentService;

	DeploymentResource(final DeploymentService deploymentService) {
		this.deploymentService = deploymentService;
	}

	@Operation(summary = "Deploy a BPMN process definition or DMN decision table", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
	})
	@PostMapping(consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
	ResponseEntity<DeploymentResponse> deploy(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(description = "Deployment name") @RequestParam final String name,
		@Parameter(description = "BPMN (.bpmn) or DMN (.dmn) file to deploy") @RequestPart final MultipartFile file) {

		return ok(deploymentService.deploy(name, file));
	}

	@Operation(summary = "List all deployments", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
	})
	@GetMapping(produces = APPLICATION_JSON_VALUE)
	ResponseEntity<DeploymentsResponse> getDeployments(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId) {

		return ok(deploymentService.getDeployments());
	}
}
