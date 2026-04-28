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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.dept44.common.validators.annotation.ValidMunicipalityId;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.problem.violations.ConstraintViolationProblem;
import se.sundsvall.operaton.process.api.model.CorrelationMessageRequest;
import se.sundsvall.operaton.process.service.MessageService;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.http.ResponseEntity.noContent;

@Tag(name = "Messages", description = "Correlate BPMN messages to running process instances")
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
class MessageResource {

	private final MessageService messageService;

	MessageResource(final MessageService messageService) {
		this.messageService = messageService;
	}

	@Operation(
		summary = "Correlate a BPMN message to a process instance waiting on a receive task or message catch event",
		description = "Delivers the message to every process instance that matches the supplied messageName and businessKey. "
			+ "If multiple instances share the same businessKey (rare but allowed), all of them are advanced. "
			+ "Returns 404 when no waiting instance matches.",
		responses = {
			@ApiResponse(responseCode = "204", description = "Message correlated to one or more instances"),
			@ApiResponse(responseCode = "404", description = "No process instance waiting for the message", content = @Content(mediaType = APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = Problem.class)))
		})
	@PostMapping(value = "/message", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> correlateMessage(
		@Parameter(name = "municipalityId", description = "Municipality id", example = "2281") @ValidMunicipalityId @PathVariable final String municipalityId,
		@Valid @RequestBody final CorrelationMessageRequest request) {

		messageService.correlate(request);
		return noContent().build();
	}
}
