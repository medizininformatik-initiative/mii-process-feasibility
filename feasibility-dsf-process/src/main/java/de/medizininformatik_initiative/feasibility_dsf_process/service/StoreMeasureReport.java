package de.medizininformatik_initiative.feasibility_dsf_process.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.*;

public class StoreMeasureReport extends AbstractServiceDelegate implements InitializingBean
{

	private static final Logger logger = LoggerFactory.getLogger(StoreMeasureReport.class);

	public StoreMeasureReport(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		MeasureReport measureReport = (MeasureReport) execution.getVariable(VARIABLE_MEASURE_REPORT);
		Measure associatedMeasure = (Measure) execution.getVariable(VARIABLE_MEASURE);

		addReadAccessTag(measureReport);
		referenceZarsMeasure(measureReport, associatedMeasure);

		IdType measureReportId = storeMeasureReport(measureReport);
		logger.debug("Stored MeasureReport {}", measureReportId);

		execution.setVariable(VARIABLE_MEASURE_REPORT_ID, measureReportId.getValue());
	}

    private void addReadAccessTag(MeasureReport measureReport)
    {
        String identifier = getLeadingTaskFromExecutionVariables().getRequester().getIdentifier().getValue();
        getReadAccessHelper().addOrganization(measureReport, identifier);
    }

	private void referenceZarsMeasure(MeasureReport measureReport, Measure zarsMeasure) {
		measureReport.setMeasure(zarsMeasure.getUrl());
	}

	private IdType storeMeasureReport(MeasureReport measureReport)
	{
		return getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn().create(measureReport);
	}
}
