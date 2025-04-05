package de.medizininformatik_initiative.process.feasibility.spring.config;

import de.medizininformatik_initiative.process.feasibility.EvaluationSettingsProvider;
import de.medizininformatik_initiative.process.feasibility.FeasibilityCachingLaplaceCountObfuscator;
import de.medizininformatik_initiative.process.feasibility.FeasibilityProcessPluginDeploymentStateListener;
import de.medizininformatik_initiative.process.feasibility.Obfuscator;
import de.medizininformatik_initiative.process.feasibility.RateLimit;
import de.medizininformatik_initiative.process.feasibility.client.flare.FlareWebserviceClient;
import de.medizininformatik_initiative.process.feasibility.client.listener.SetCorrelationKeyListener;
import de.medizininformatik_initiative.process.feasibility.message.SendDicRequest;
import de.medizininformatik_initiative.process.feasibility.message.SendDicResponse;
import de.medizininformatik_initiative.process.feasibility.service.DownloadFeasibilityResources;
import de.medizininformatik_initiative.process.feasibility.service.DownloadMeasureReport;
import de.medizininformatik_initiative.process.feasibility.service.EvaluateCqlMeasure;
import de.medizininformatik_initiative.process.feasibility.service.EvaluateRequestRate;
import de.medizininformatik_initiative.process.feasibility.service.EvaluateStructuredQueryMeasure;
import de.medizininformatik_initiative.process.feasibility.service.FeasibilityResourceCleaner;
import de.medizininformatik_initiative.process.feasibility.service.LogReceiveTimeout;
import de.medizininformatik_initiative.process.feasibility.service.ObfuscateEvaluationResult;
import de.medizininformatik_initiative.process.feasibility.service.RateLimitExceededTaskRejecter;
import de.medizininformatik_initiative.process.feasibility.service.SelectRequestTargets;
import de.medizininformatik_initiative.process.feasibility.service.SelectResponseTarget;
import de.medizininformatik_initiative.process.feasibility.service.SetupEvaluationSettings;
import de.medizininformatik_initiative.process.feasibility.service.StoreFeasibilityResources;
import de.medizininformatik_initiative.process.feasibility.service.StoreLiveResult;
import de.medizininformatik_initiative.process.feasibility.service.StoreMeasureReport;
import dev.dsf.bpe.v2.documentation.ProcessDocumentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class FeasibilityConfig {

    private final EvaluationSettingsProvider evaluationSettingsProvider;
    private final FlareWebserviceClient flareWebserviceClient;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "The FHIR server connection ID to use for the store client. This has to be one of the connection id's used in the BPE setup.")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.store.connection.id:#{null}}")
    private String connectionId;
    
    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "To enable asynchronous request pattern when executing measure set to `true`")
    @Value("${de.medizininformatik.initiative.feasibility_dsf_process.store.request.async.enabled:false}")
    private boolean asyncRequestEnabled;
    
    public FeasibilityConfig(EvaluationSettingsProvider evaluationSettingsProvider,
                             FlareWebserviceClient flareWebserviceClient) {
        this.evaluationSettingsProvider = evaluationSettingsProvider;
        this.flareWebserviceClient = flareWebserviceClient;
    }

    @Bean
    public Obfuscator<Integer> feasibilityCountObfuscator() {
        return new FeasibilityCachingLaplaceCountObfuscator(
                evaluationSettingsProvider.resultObfuscationLaplaceSensitivity(),
                evaluationSettingsProvider.resultObfuscationLaplaceEpsilon());
    }

    //
    // process requestFeasibility implementations
    //

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SelectRequestTargets selectRequestTargets() {
        return new SelectRequestTargets();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SendDicRequest sendDicRequests() {
        return new SendDicRequest();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public DownloadMeasureReport downloadMeasureReport() {
        return new DownloadMeasureReport();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public StoreLiveResult storeLiveResult() {
        return new StoreLiveResult();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public LogReceiveTimeout logReceiveTimeout() {
        return new LogReceiveTimeout();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SetCorrelationKeyListener setCorrelationKeyListener() {
        return new SetCorrelationKeyListener();
    }

    //
    // process executeFeasibility implementations
    //

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public EvaluateRequestRate requestRateLimiter() {
        return new EvaluateRequestRate(new RateLimit(evaluationSettingsProvider.getRateLimitCount(),
                evaluationSettingsProvider.getRateLimitTimeIntervalDuration()));
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RateLimitExceededTaskRejecter rateLimitExceededTaskRejecter() {
        return new RateLimitExceededTaskRejecter();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SetupEvaluationSettings setupEvaluationSettings() {
        return new SetupEvaluationSettings(evaluationSettingsProvider);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public DownloadFeasibilityResources downloadFeasibilityResources() {
        return new DownloadFeasibilityResources();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public StoreFeasibilityResources storeFeasibilityResources() {
        return new StoreFeasibilityResources(new FeasibilityResourceCleaner(), connectionId);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public EvaluateCqlMeasure evaluateCqlMeasure() {
        return new EvaluateCqlMeasure(asyncRequestEnabled, connectionId);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public EvaluateStructuredQueryMeasure evaluateStructureQueryMeasure() {
        return new EvaluateStructuredQueryMeasure(flareWebserviceClient);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ObfuscateEvaluationResult obfuscateEvaluationResult(Obfuscator<Integer> feasibilityCountObfuscator) {
        return new ObfuscateEvaluationResult(feasibilityCountObfuscator);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public StoreMeasureReport storeMeasureReport() {
        return new StoreMeasureReport();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SelectResponseTarget selectResponseTarget() {
        return new SelectResponseTarget();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SendDicResponse sendDicResponse() {
        return new SendDicResponse();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public FeasibilityProcessPluginDeploymentStateListener deploymentStateListener() {
        return new FeasibilityProcessPluginDeploymentStateListener(evaluationSettingsProvider.evaluationStrategy(),
                flareWebserviceClient);
    }
}
