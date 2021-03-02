package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.spring.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.EnhancedFhirWebserviceClientProvider;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.EnhancedFhirWebserviceClientProviderImpl;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.message.SendDicRequest;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.message.SendDicResponse;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.DownloadFeasibilityResources;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.DownloadMeasureReport;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.EvaluateMeasure;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.SelectRequestTargets;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.SelectResponseTarget;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.StoreFeasibilityResources;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.StoreLiveResult;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service.StoreMeasureReport;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
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
    private final TaskHelper taskHelper;
    private final FhirContext fhirContext;

    public FeasibilityConfig(@Qualifier("clientProvider") FhirWebserviceClientProvider fhirClientProvider,
                             @Qualifier("store") IGenericClient storeClient,
                             OrganizationProvider organizationProvider,
                             TaskHelper taskHelper,
                             FhirContext fhirContext) {
        this.fhirClientProvider = fhirClientProvider;
        this.storeClient = storeClient;
        this.organizationProvider = organizationProvider;
        this.taskHelper = taskHelper;
        this.fhirContext = fhirContext;
    }

    @Bean
    public EnhancedFhirWebserviceClientProvider enhancedFhirClientProvider() {
        return new EnhancedFhirWebserviceClientProviderImpl(fhirClientProvider);
    }

    //
    // process requestSimpleFeasibility implementations
    //

    @Bean
    public SelectRequestTargets selectRequestTargets() {
        return new SelectRequestTargets(fhirClientProvider, taskHelper, organizationProvider);
    }

    @Bean
    public SendDicRequest sendDicRequest() {
        return new SendDicRequest(fhirClientProvider, taskHelper, organizationProvider, fhirContext);
    }

    @Bean
    public DownloadMeasureReport downloadMeasureReport() {
        return new DownloadMeasureReport(fhirClientProvider, taskHelper, organizationProvider);
    }
    
    @Bean
    public StoreLiveResult storeLiveResult() {
        return new StoreLiveResult(fhirClientProvider, taskHelper);
    }

    //
    // process executeSimpleFeasibility implementations
    //

    @Bean
    public DownloadFeasibilityResources downloadFeasibilityResources(
            EnhancedFhirWebserviceClientProvider enhancedFhirClientProvider) {
        return new DownloadFeasibilityResources(enhancedFhirClientProvider, taskHelper, organizationProvider);
    }

    @Bean
    public StoreFeasibilityResources storeFeasibilityResources() {
        return new StoreFeasibilityResources(fhirClientProvider, taskHelper, storeClient);
    }

    @Bean
    public EvaluateMeasure evaluateMeasure() {
        return new EvaluateMeasure(fhirClientProvider, taskHelper, storeClient);
    }

    @Bean
    public StoreMeasureReport storeMeasureReport() {
        return new StoreMeasureReport(fhirClientProvider, taskHelper);
    }

    @Bean
    public SelectResponseTarget selectResponseTarget() {
        return new SelectResponseTarget(fhirClientProvider, taskHelper, organizationProvider);
    }

    @Bean
    public SendDicResponse sendDicResponse() {
        return new SendDicResponse(fhirClientProvider, taskHelper, organizationProvider, fhirContext);
    }
}
