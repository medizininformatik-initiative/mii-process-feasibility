package de.medizininformatik_initiative.process.feasibility.service;

import de.medizininformatik_initiative.process.feasibility.MeasureReportIterator;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Target;
import dev.dsf.bpe.v1.variables.Targets;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;

public class AggregateMeasureReports extends AbstractServiceDelegate implements MeasureReportIterator {
    private static final Logger logger = LoggerFactory.getLogger(AggregateMeasureReports.class);

    public AggregateMeasureReports(ProcessPluginApi api) {
        super(api);
    }

    @Override
    protected void doExecute(DelegateExecution delegateExecution, Variables variables) throws Exception {
        logger.info("doExecute store and aggregate measure reports");

        // Die Reports rausholen. Ggf. die Tasks aus StoreLiveResult holen.
        Targets targets = variables.getTargets();

        AtomicInteger i = new AtomicInteger(0);

        MeasureReport firstMeasureReport = null;
        MeasureReport.MeasureReportGroupPopulationComponent populationComponent = null;

        for (Target target : targets.getEntries()) {
            String correlationKey = target.getCorrelationKey();
            Resource measure = variables.getResource("subMeasure_" + correlationKey);
            if (measure instanceof MeasureReport measureReport) {
                populationComponent = extractInitialPopulation(measureReport);

                i.addAndGet(populationComponent.getCount());

                if (firstMeasureReport == null)
                    firstMeasureReport = measureReport;
            }
        }

        if (firstMeasureReport != null) {
            populationComponent.setCount(i.get());
            firstMeasureReport.setId((String) null);
        }

        variables.setResource(VARIABLE_MEASURE_REPORT, firstMeasureReport);
    }


}
