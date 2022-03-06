package de.medizininformatik_initiative.feasibility_dsf_process.service;

import de.medizininformatik_initiative.feasibility_dsf_process.FeasibilityCountObfuscator;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.MeasureReport;
import org.springframework.beans.factory.InitializingBean;

import java.util.Objects;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;

public class ObfuscateEvaluationResult extends AbstractServiceDelegate implements InitializingBean {

    private final FeasibilityCountObfuscator obfuscator;

    public ObfuscateEvaluationResult(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
                                     ReadAccessHelper readAccessHelper, FeasibilityCountObfuscator obfuscator) {
        super(clientProvider, taskHelper, readAccessHelper);
        this.obfuscator = obfuscator;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Objects.requireNonNull(obfuscator, "obfuscator");
    }

    @Override
    protected void doExecute(DelegateExecution execution) {
        var measureReport = (MeasureReport) execution.getVariable(VARIABLE_MEASURE_REPORT);
        execution.setVariable(VARIABLE_MEASURE_REPORT, obfuscateFeasibilityCount(measureReport));
    }

    private MeasureReport obfuscateFeasibilityCount(MeasureReport measureReport) {
        var obfuscatedFeasibilityCount = obfuscator.obfuscate(measureReport.getGroupFirstRep().
                getPopulationFirstRep().getCount());

        var obfuscatedMeasureReport = measureReport.copy();
        obfuscatedMeasureReport.getGroupFirstRep().getPopulationFirstRep().setCount(obfuscatedFeasibilityCount);
        return obfuscatedMeasureReport;
    }
}
