package com.ghiloufi.aicode.core.domain.port.output;

import com.ghiloufi.aicode.core.domain.model.AgentConfiguration;
import com.ghiloufi.aicode.core.domain.model.ChangeRequestIdentifier;
import com.ghiloufi.aicode.core.domain.model.LocalAnalysisResult;
import com.ghiloufi.aicode.core.domain.model.RepositoryIdentifier;
import java.nio.file.Path;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LocalAnalysisPort {

  Mono<Path> cloneRepository(
      RepositoryIdentifier repository,
      ChangeRequestIdentifier changeRequest,
      AgentConfiguration.DockerConfig config);

  Mono<LocalAnalysisResult> runFullAnalysis(
      RepositoryIdentifier repository,
      ChangeRequestIdentifier changeRequest,
      AgentConfiguration configuration);

  Mono<Void> cleanup(Path repositoryPath);

  Flux<String> streamAnalysisLogs(Path repositoryPath);
}
