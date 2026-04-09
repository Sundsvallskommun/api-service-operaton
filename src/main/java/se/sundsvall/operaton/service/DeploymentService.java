package se.sundsvall.operaton.service;

import java.io.IOException;
import java.util.List;
import org.operaton.bpm.engine.RepositoryService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.operaton.api.model.DeploymentResponse;

import static se.sundsvall.operaton.service.mapper.OperatonMapper.toDeploymentResponse;
import static se.sundsvall.operaton.service.mapper.OperatonMapper.toDeploymentResponses;

@Service
public class DeploymentService {

	private static final String DEPLOYMENT_FAILED = "Failed to deploy BPMN file: %s";

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
		} catch (final IOException e) {
			throw Problem.internalServerError(DEPLOYMENT_FAILED.formatted(e.getMessage()));
		}
	}

	public List<DeploymentResponse> getDeployments() {
		final var deployments = repositoryService.createDeploymentQuery()
			.orderByDeploymentTime()
			.desc()
			.list();
		return toDeploymentResponses(deployments);
	}
}
