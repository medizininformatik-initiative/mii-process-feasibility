package de.medizininformatik_initiative.process.feasibility.spring.config;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.medizininformatik_initiative.process.feasibility.*;
import de.medizininformatik_initiative.process.feasibility.client.flare.FlareWebserviceClient;
import de.medizininformatik_initiative.process.feasibility.listener.SetCorrelationKeyListener;
import de.medizininformatik_initiative.process.feasibility.message.SendDicRequest;
import de.medizininformatik_initiative.process.feasibility.message.SendDicResponse;
import de.medizininformatik_initiative.process.feasibility.service.*;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.service.FhirWebserviceClientProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.*;

@Configuration
public class FeasibilityConfig {

    @Autowired
    private ProcessPluginApi api;

    public FeasibilityConfig() {
    }

    @Bean
    public EnhancedFhirWebserviceClientProvider enhancedFhirClientProvider(
            @Qualifier("clientProvider") FhirWebserviceClientProvider fhirClientProvider) {
        return new EnhancedFhirWebserviceClientProviderImpl(fhirClientProvider);
    }

    @Bean
    public Obfuscator<Integer> feasibilityCountObfuscator() {
        return new FeasibilityCachingLaplaceCountObfuscator(DEFAULT_OBFUSCATION_LAPLACE_SENSITIVITY,
                DEFAULT_OBFUSCATION_LAPLACE_EPSILON);
    }

    //
    // process requestFeasibility implementations
    //

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SelectRequestTargets selectRequestTargets(FeasibilitySettings feasibilitySettings) {
        return new SelectRequestTargets(api, feasibilitySettings.general().requestTaskTimeout());
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
    public EvaluateRequestRate requestRateLimiters(FeasibilitySettings feasibilitySettings) {
        return new EvaluateRequestRate(api,
                feasibilitySettings.networks().entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey(),
                                e -> Optional.ofNullable(e.getValue().rateLimit())
                                        .map(r -> new RateLimit(r.count(), r.interval()))
                                        .orElse(new RateLimit(DEFAULT_RATE_LIMIT_COUNT,
                                                DEFAULT_RATE_LIMIT_DURATION)))));
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public RateLimitExceededTaskRejecter rateLimitExceededTaskRejecter() {
        return new RateLimitExceededTaskRejecter(api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SetupEvaluationSettings setupEvaluationSettings(FeasibilitySettings feasibilitySettings) {
        return new SetupEvaluationSettings(feasibilitySettings, api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public DownloadFeasibilityResources downloadFeasibilityResources(
            EnhancedFhirWebserviceClientProvider enhancedFhirClientProvider) {
        return new DownloadFeasibilityResources(enhancedFhirClientProvider, api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public StoreFeasibilityResources storeFeasibilityResources(Map<String, Set<String>> networkStores,
                                                               Map<String, IGenericClient> storeClients) {
        return new StoreFeasibilityResources(networkStores, storeClients, api, new FeasibilityResourceCleaner());
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public EvaluateCQLMeasure evaluateCqlMeasure(Map<String, Set<String>> networkStores,
                                                 Map<String, IGenericClient> storeClients) {
        return new EvaluateCQLMeasure(networkStores, storeClients, api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public EvaluateCCDLMeasure evaluateStructureQueryMeasure(Map<String, Set<String>> networkStores,
                                                             Map<String, FlareWebserviceClient> flareClients) {
        return new EvaluateCCDLMeasure(networkStores, flareClients, api);
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
    public MergeMeasureResults mergeMeasureResults() {
        return new MergeMeasureResults(api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public FeasibilityProcessPluginDeploymentStateListener deploymentStateListener(Map<String, IGenericClient> storeClients,
                                                                                   Map<String, FlareWebserviceClient> flareClients) {
        return new FeasibilityProcessPluginDeploymentStateListener(storeClients, flareClients);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public StoreFeasibilityResourcesLocally storeFeasibilityResourcesLocally(){
        return new StoreFeasibilityResourcesLocally(api);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public AggregateMeasureReports aggregateMeasureReports() {
        return new AggregateMeasureReports(api);
    }
}
