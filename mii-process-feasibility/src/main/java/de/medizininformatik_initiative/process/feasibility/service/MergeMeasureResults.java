package de.medizininformatik_initiative.process.feasibility.service;

import de.medizininformatik_initiative.process.feasibility.MeasureReportBuilder;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Measure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.Optional;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.*;

public class MergeMeasureResults extends AbstractServiceDelegate implements InitializingBean, MeasureReportBuilder {
    private static final Logger logger = LoggerFactory.getLogger(MergeMeasureResults.class);

    public MergeMeasureResults(ProcessPluginApi api) {
        super(api);
    }

    @Override
    protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception {
        logger.info("doExecute merge measure results");

        var measure = (Measure) variables.getResource(VARIABLE_MEASURE);
        var cqlResult = Optional.ofNullable(variables.getInteger(VARIABLE_MEASURE_RESULT_CQL)).orElse(0);
        var ccdlResult = Optional.ofNullable(variables.getInteger(VARIABLE_MEASURE_RESULT_CCDL)).orElse(0);
        var report = buildMeasureReport(measure.getUrl(), cqlResult + ccdlResult);

        variables.setResource(VARIABLE_MEASURE_REPORT, report);
    }
}
