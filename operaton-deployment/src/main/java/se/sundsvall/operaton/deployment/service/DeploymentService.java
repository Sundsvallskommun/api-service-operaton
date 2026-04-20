package se.sundsvall.operaton.deployment.service;

import java.io.IOException;
import org.operaton.bpm.engine.ProcessEngineException;
import org.operaton.bpm.engine.RepositoryService;
import org.operaton.bpm.engine.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.operaton.deployment.api.model.DeploymentResponse;
import se.sundsvall.operaton.deployment.api.model.DeploymentsResponse;

import static se.sundsvall.operaton.deployment.service.mapper.DeploymentMapper.toDeploymentResponse;
import static se.sundsvall.operaton.deployment.service.mapper.DeploymentMapper.toDeploymentsResponse;

@Service
public class DeploymentService {

	private static final String DEPLOYMENT_FAILED = "Failed to deploy BPMN file: %s";
	private static final String DEPLOYMENT_DELETE_FAILED = "Failed to delete deployment '%s': %s";
	private static final String DEPLOYMENT_NOT_FOUND = "Deployment '%s' not found";

	private final RepositoryService repositoryService;

	public DeploymentService(final RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	public DeploymentResponse deploy(final String name, final MultipartFile file) {
		try {
			final var deployment = repositoryService.createDeployment()
				.name(name)
				.addInputStream(file.getOriginalFilename(), file.getInputStream())
				.deploy();
			return toDeploymentResponse(deployment);
		} catch (final IOException | ProcessEngineException e) {
			throw Problem.internalServerError(DEPLOYMENT_FAILED.formatted(e.getMessage()));
		}
	}

	public DeploymentsResponse getDeployments() {
		final var deployments = repositoryService.createDeploymentQuery()
			.orderByDeploymentTime()
			.desc()
			.list();
		return toDeploymentsResponse(deployments);
	}

	public void deleteDeployment(final String deploymentId, final boolean cascade) {
		try {
			repositoryService.deleteDeployment(deploymentId, cascade);
		} catch (final NotFoundException _) {
			throw Problem.notFound(DEPLOYMENT_NOT_FOUND.formatted(deploymentId));
		} catch (final ProcessEngineException e) {
			throw Problem.internalServerError(DEPLOYMENT_DELETE_FAILED.formatted(deploymentId, e.getMessage()));
		}
	}
}
