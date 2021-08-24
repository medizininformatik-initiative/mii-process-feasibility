package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.spring.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.EnhancedFhirWebserviceClientProvider;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.EnhancedFhirWebserviceClientProviderImpl;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.EvaluationSettingsProvider;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.FeasibilityCountObfuscator;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.FlareWebserviceClient;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.message.SendDicRequest;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.message.SendDicResponse;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.AggregateMeasureReports;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.DownloadFeasibilityResources;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.DownloadMeasureReport;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.EvaluateCqlMeasure;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.EvaluateStructuredQueryMeasure;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.ObfuscateEvaluationResult;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.PrepareForFurtherEvaluation;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.SelectRequestTargets;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.SelectResponseTarget;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.SetupEvaluationSettings;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.StoreFeasibilityResources;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.StoreLiveResult;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.StoreMeasureReport;
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
                             @Qualifier("store") IGenericClient storeClient,
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
        return new FeasibilityCountObfuscator();
    }

    //
    // process requestSimpleFeasibility implementations
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
    // process executeSimpleFeasibility implementations
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
