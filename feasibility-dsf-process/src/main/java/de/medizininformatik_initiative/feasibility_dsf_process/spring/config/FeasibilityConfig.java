package de.medizininformatik_initiative.feasibility_dsf_process.spring.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.medizininformatik_initiative.feasibility_dsf_process.EnhancedFhirWebserviceClientProvider;
import de.medizininformatik_initiative.feasibility_dsf_process.EnhancedFhirWebserviceClientProviderImpl;
import de.medizininformatik_initiative.feasibility_dsf_process.EvaluationSettingsProvider;
import de.medizininformatik_initiative.feasibility_dsf_process.EvaluationStrategy;
import de.medizininformatik_initiative.feasibility_dsf_process.FeasibilityCachingLaplaceCountObfuscator;
import de.medizininformatik_initiative.feasibility_dsf_process.FeasibilityProcessPluginDeploymentStateListener;
import de.medizininformatik_initiative.feasibility_dsf_process.Obfuscator;
import de.medizininformatik_initiative.feasibility_dsf_process.RateLimit;
import de.medizininformatik_initiative.feasibility_dsf_process.client.flare.FlareWebserviceClient;
import de.medizininformatik_initiative.feasibility_dsf_process.client.listener.SetCorrelationKeyListener;
import de.medizininformatik_initiative.feasibility_dsf_process.message.SendDicRequest;
import de.medizininformatik_initiative.feasibility_dsf_process.message.SendDicResponse;
import de.medizininformatik_initiative.feasibility_dsf_process.service.DownloadFeasibilityResources;
import de.medizininformatik_initiative.feasibility_dsf_process.service.DownloadMeasureReport;
import de.medizininformatik_initiative.feasibility_dsf_process.service.EvaluateCqlMeasure;
import de.medizininformatik_initiative.feasibility_dsf_process.service.EvaluateRequestRate;
import de.medizininformatik_initiative.feasibility_dsf_process.service.EvaluateStructuredQueryMeasure;
import de.medizininformatik_initiative.feasibility_dsf_process.service.FeasibilityResourceCleaner;
import de.medizininformatik_initiative.feasibility_dsf_process.service.LogReceiveTimeout;
import de.medizininformatik_initiative.feasibility_dsf_process.service.ObfuscateEvaluationResult;
import de.medizininformatik_initiative.feasibility_dsf_process.service.RateLimitExceededTaskRejecter;
import de.medizininformatik_initiative.feasibility_dsf_process.service.SelectRequestTargets;
import de.medizininformatik_initiative.feasibility_dsf_process.service.SelectResponseTarget;
import de.medizininformatik_initiative.feasibility_dsf_process.service.SetupEvaluationSettings;
import de.medizininformatik_initiative.feasibility_dsf_process.service.StoreFeasibilityResources;
import de.medizininformatik_initiative.feasibility_dsf_process.service.StoreLiveResult;
import de.medizininformatik_initiative.feasibility_dsf_process.service.StoreMeasureReport;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.service.FhirWebserviceClientProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class FeasibilityConfig {

    private final IGenericClient storeClient;

    @Autowired private final FhirContext fhirContext;
    @Autowired private ProcessPluginApi api;

    private final EvaluationSettingsProvider evaluationSettingsProvider;
    private final FlareWebserviceClient flareWebserviceClient;

    public FeasibilityConfig(@Qualifier("store-client") IGenericClient storeClient,
                             FhirContext fhirContext,
                             EvaluationSettingsProvider evaluationSettingsProvider,
                             FlareWebserviceClient flareWebserviceClient) {
        this.storeClient = storeClient;
        this.fhirContext = fhirContext;
        this.evaluationSettingsProvider = evaluationSettingsProvider;
        this.flareWebserviceClient = flareWebserviceClient;
    }

    @Bean
    public EnhancedFhirWebserviceClientProvider enhancedFhirClientProvider(@Qualifier("clientProvider") FhirWebserviceClientProvider fhirClientProvider) {
        return new EnhancedFhirWebserviceClientProviderImpl(fhirClientProvider);
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
        return new SelectRequestTargets(api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SendDicRequest sendDicRequests() {
        return new SendDicRequest(api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public DownloadMeasureReport downloadMeasureReport(EnhancedFhirWebserviceClientProvider enhancedFhirClientProvider) {
        return new DownloadMeasureReport(enhancedFhirClientProvider, api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public StoreLiveResult storeLiveResult() {
        return new StoreLiveResult(api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public LogReceiveTimeout logReceiveTimeout() {
        return new LogReceiveTimeout(api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SetCorrelationKeyListener setCorrelationKeyListener() {
        return new SetCorrelationKeyListener(api);
    }

    //
    // process executeFeasibility implementations
    //

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public EvaluateRequestRate requestRateLimiter() {
        return new EvaluateRequestRate(new RateLimit(evaluationSettingsProvider.getRateLimitCount(),
                evaluationSettingsProvider.getRateLimitTimeIntervalDuration()), api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RateLimitExceededTaskRejecter rateLimitExceededTaskRejecter() {
        return new RateLimitExceededTaskRejecter(api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SetupEvaluationSettings setupEvaluationSettings() {
        return new SetupEvaluationSettings(evaluationSettingsProvider, api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public DownloadFeasibilityResources downloadFeasibilityResources(
            EnhancedFhirWebserviceClientProvider enhancedFhirClientProvider) {
        return new DownloadFeasibilityResources(enhancedFhirClientProvider, api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public StoreFeasibilityResources storeFeasibilityResources() {
        return new StoreFeasibilityResources(storeClient, api, new FeasibilityResourceCleaner());
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public EvaluateCqlMeasure evaluateCqlMeasure() {
        return new EvaluateCqlMeasure(storeClient, api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public EvaluateStructuredQueryMeasure evaluateStructureQueryMeasure() {
        return new EvaluateStructuredQueryMeasure(flareWebserviceClient, api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ObfuscateEvaluationResult obfuscateEvaluationResult(Obfuscator<Integer> feasibilityCountObfuscator) {
        return new ObfuscateEvaluationResult(feasibilityCountObfuscator, api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public StoreMeasureReport storeMeasureReport() {
        return new StoreMeasureReport(api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SelectResponseTarget selectResponseTarget() {
        return new SelectResponseTarget(api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SendDicResponse sendDicResponse() {
        return new SendDicResponse(api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public FeasibilityProcessPluginDeploymentStateListener deploymentStateListener() {
        return new FeasibilityProcessPluginDeploymentStateListener(
                EvaluationStrategy
                        .fromStrategyRepresentation(evaluationSettingsProvider.evaluationStrategyRepresentation()),
                storeClient, flareWebserviceClient);
    }
}
