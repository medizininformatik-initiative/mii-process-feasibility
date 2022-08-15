package de.medizininformatik_initiative.feasibility_dsf_process.spring.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.medizininformatik_initiative.feasibility_dsf_process.EnhancedFhirWebserviceClientProvider;
import de.medizininformatik_initiative.feasibility_dsf_process.EnhancedFhirWebserviceClientProviderImpl;
import de.medizininformatik_initiative.feasibility_dsf_process.EvaluationSettingsProvider;
import de.medizininformatik_initiative.feasibility_dsf_process.FeasibilityCountObfuscator;
import de.medizininformatik_initiative.feasibility_dsf_process.client.flare.FlareWebserviceClient;
import de.medizininformatik_initiative.feasibility_dsf_process.message.SendDicRequest;
import de.medizininformatik_initiative.feasibility_dsf_process.message.SendDicResponse;
import de.medizininformatik_initiative.feasibility_dsf_process.service.*;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeasibilityConfig {

    private final FhirWebserviceClientProvider fhirClientProvider;
    private final IGenericClient storeClient;
    private final OrganizationProvider organizationProvider;
    private final EndpointProvider endpointProvider;
    private final TaskHelper taskHelper;
    private final ReadAccessHelper readAccessHelper;
    private final FhirContext fhirContext;
    private final EvaluationSettingsProvider evaluationSettingsProvider;
    private final FlareWebserviceClient flareWebserviceClient;

    public FeasibilityConfig(@Qualifier("clientProvider") FhirWebserviceClientProvider fhirClientProvider,
                             @Qualifier("store-client") IGenericClient storeClient,
                             OrganizationProvider organizationProvider,
                             EndpointProvider endpointProvider,
                             TaskHelper taskHelper,
                             ReadAccessHelper readAccessHelper,
                             FhirContext fhirContext,
                             EvaluationSettingsProvider evaluationSettingsProvider,
                             FlareWebserviceClient flareWebserviceClient) {
        this.fhirClientProvider = fhirClientProvider;
        this.storeClient = storeClient;
        this.organizationProvider = organizationProvider;
        this.endpointProvider = endpointProvider;
        this.taskHelper = taskHelper;
        this.readAccessHelper = readAccessHelper;
        this.fhirContext = fhirContext;
        this.evaluationSettingsProvider = evaluationSettingsProvider;
        this.flareWebserviceClient = flareWebserviceClient;
    }

    @Bean
    public EnhancedFhirWebserviceClientProvider enhancedFhirClientProvider() {
        return new EnhancedFhirWebserviceClientProviderImpl(fhirClientProvider);
    }

    @Bean
    public FeasibilityCountObfuscator feasibilityCountObfuscator() {
        var randomNumberGenerator = new FeasibilityCountObfuscator.ObfuscationRandomNumberGenerator();
        return new FeasibilityCountObfuscator(randomNumberGenerator);
    }

    //
    // process requestFeasibility implementations
    //

    @Bean
    public SelectRequestTargets selectRequestTargets() {
        return new SelectRequestTargets(fhirClientProvider, taskHelper, readAccessHelper, organizationProvider, endpointProvider);
    }

    @Bean
    public SendDicRequest sendDicRequest() {
        return new SendDicRequest(fhirClientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
    }

    @Bean
    public DownloadMeasureReport downloadMeasureReport(EnhancedFhirWebserviceClientProvider enhancedFhirClientProvider) {
        return new DownloadMeasureReport(enhancedFhirClientProvider, taskHelper, readAccessHelper, organizationProvider);
    }

    @Bean
    public StoreLiveResult storeLiveResult() {
        return new StoreLiveResult(fhirClientProvider, taskHelper, readAccessHelper);
    }

    @Bean
    public PrepareForFurtherEvaluation prepareForFurtherEvaluation() {
        return new PrepareForFurtherEvaluation(fhirClientProvider, taskHelper, readAccessHelper);
    }

    @Bean
    public AggregateMeasureReports aggregateMeasureReports() {
        return new AggregateMeasureReports(fhirClientProvider, taskHelper, readAccessHelper);
    }

    //
    // process executeFeasibility implementations
    //

    @Bean
    public SetupEvaluationSettings setupEvaluationSettings() {
        return new SetupEvaluationSettings(fhirClientProvider, taskHelper, readAccessHelper, evaluationSettingsProvider);
    }

    @Bean
    public DownloadFeasibilityResources downloadFeasibilityResources(
            EnhancedFhirWebserviceClientProvider enhancedFhirClientProvider) {
        return new DownloadFeasibilityResources(enhancedFhirClientProvider, taskHelper, readAccessHelper,
                organizationProvider);
    }

    @Bean
    public StoreFeasibilityResources storeFeasibilityResources() {
        return new StoreFeasibilityResources(fhirClientProvider, taskHelper, readAccessHelper, storeClient);
    }

    @Bean
    public EvaluateCqlMeasure evaluateCqlMeasure() {
        return new EvaluateCqlMeasure(fhirClientProvider, taskHelper, readAccessHelper, storeClient);
    }

    @Bean
    public EvaluateStructuredQueryMeasure evaluateStructureQueryMeasure() {
        return new EvaluateStructuredQueryMeasure(fhirClientProvider, taskHelper, readAccessHelper, flareWebserviceClient);
    }

    @Bean
    public ObfuscateEvaluationResult obfuscateEvaluationResult(FeasibilityCountObfuscator feasibilityCountObfuscator) {
        return new ObfuscateEvaluationResult(fhirClientProvider, taskHelper, readAccessHelper,
                feasibilityCountObfuscator);
    }

    @Bean
    public StoreMeasureReport storeMeasureReport() {
        return new StoreMeasureReport(fhirClientProvider, taskHelper, readAccessHelper);
    }

    @Bean
    public SelectResponseTarget selectResponseTarget() {
        return new SelectResponseTarget(fhirClientProvider, taskHelper, readAccessHelper, organizationProvider,
                endpointProvider);
    }

    @Bean
    public SendDicResponse sendDicResponse() {
        return new SendDicResponse(fhirClientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
    }
}
