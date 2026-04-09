package se.sundsvall.operaton.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.operaton.api.model.ElementTemplate;
import se.sundsvall.operaton.api.model.TopicDescription;
import se.sundsvall.operaton.service.TopicService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.ok;

@Tag(name = "Topics", description = "Catalog of available external task worker topics. Each topic represents a reusable building block that can be used in BPMN process models")
@Validated
@RestController
@RequestMapping("/{municipalityId}/topics")
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
class TopicResource {

	private final TopicService topicService;

	TopicResource(final TopicService topicService) {
		this.topicService = topicService;
	}

	@Operation(summary = "List all available external task topics with their input/output variable contracts", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
	})
	@GetMapping(produces = APPLICATION_JSON_VALUE)
	ResponseEntity<List<TopicDescription>> getTopics(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId) {

		return ok(topicService.getTopics());
	}

	@Operation(summary = "Get details for a specific external task topic including expected input/output variables", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true),
		@ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
	})
	@GetMapping(value = "/{topic}", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<TopicDescription> getTopic(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Parameter(name = "topic", description = "Topic name", example = "send-email") @PathVariable final String topic) {

		return ok(topicService.getTopic(topic));
	}

	@Operation(summary = "List all available external task topics as bpmn-js element templates for use in a BPMN editor", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
	})
	@GetMapping(value = "/templates", produces = APPLICATION_JSON_VALUE)
	ResponseEntity<List<ElementTemplate>> getElementTemplates(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId) {

		return ok(topicService.getElementTemplates());
	}
}
