package de.medizininformatik_initiative.feasibility_dsf_process.service;

import de.medizininformatik_initiative.feasibility_dsf_process.Obfuscator;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.MeasureReport;
import org.springframework.beans.factory.InitializingBean;

import java.util.Objects;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;

public class ObfuscateEvaluationResult extends AbstractServiceDelegate
        implements InitializingBean {

    private final Obfuscator<Integer> obfuscator;

    public ObfuscateEvaluationResult(Obfuscator<Integer> obfuscator, ProcessPluginApi api) {
        super(api);
        this.obfuscator = obfuscator;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Objects.requireNonNull(obfuscator, "obfuscator");
    }

    @Override
    protected void doExecute(DelegateExecution execution, Variables variables) {
        var measureReport = (MeasureReport) variables.getResource(VARIABLE_MEASURE_REPORT);
        variables.setResource(VARIABLE_MEASURE_REPORT, obfuscateFeasibilityCount(measureReport));
    }

    private MeasureReport obfuscateFeasibilityCount(MeasureReport measureReport) {
        var obfuscatedFeasibilityCount = obfuscator.obfuscate(measureReport.getGroupFirstRep().
                getPopulationFirstRep().getCount());

        var obfuscatedMeasureReport = measureReport.copy();
        obfuscatedMeasureReport.getGroupFirstRep().getPopulationFirstRep().setCount(obfuscatedFeasibilityCount);
        return obfuscatedMeasureReport;
    }
}
