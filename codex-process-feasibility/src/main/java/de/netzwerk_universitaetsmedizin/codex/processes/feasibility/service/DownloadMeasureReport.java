package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.Objects;
import java.util.Optional;

public class DownloadMeasureReport extends AbstractServiceDelegate implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(DownloadMeasureReport.class);

    private final OrganizationProvider organizationProvider;

    public DownloadMeasureReport(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
                                 OrganizationProvider organizationProvider) {
        super(clientProvider, taskHelper);
        this.organizationProvider = organizationProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Objects.requireNonNull(organizationProvider, "organizationProvider");
    }

    @Override
    protected void doExecute(DelegateExecution execution) {
        Task task = getCurrentTaskFromExecutionVariables();

        IdType measureReportId = getMeasureReportId(task);
        FhirWebserviceClient client = getWebserviceClient(measureReportId);
        MeasureReport measureReport = downloadMeasureReport(client, measureReportId);

        execution.setVariable(ConstantsFeasibility.VARIABLE_MEASURE_REPORT, measureReport);
    }

    private MeasureReport downloadMeasureReport(FhirWebserviceClient client, IdType measureReportId) {
        logger.debug("Download MeasureReport with ID {} from {}", measureReportId.getIdPart(), client.getBaseUrl());
        return client.read(MeasureReport.class, measureReportId.getIdPart());
    }

    private IdType getMeasureReportId(Task task) {
        Optional<Reference> measureRef = getTaskHelper()
                .getFirstInputParameterReferenceValue(task, ConstantsFeasibility.CODESYSTEM_FEASIBILITY,
                        ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE);
        if (measureRef.isPresent()) {
            return new IdType(measureRef.get().getReference());
        } else {
            logger.error("Task {} is missing the measure report reference.", task.getId());
            throw new RuntimeException("Missing measure report reference.");
        }
    }

    private FhirWebserviceClient getWebserviceClient(IdType measureReportId) {
        if (measureReportId.getBaseUrl() == null || measureReportId.getBaseUrl()
                .equals(getFhirWebserviceClientProvider().getLocalBaseUrl())) {
            return getFhirWebserviceClientProvider().getLocalWebserviceClient();
        } else {
            return getFhirWebserviceClientProvider().getRemoteWebserviceClient(measureReportId.getBaseUrl());
        }
    }
}
