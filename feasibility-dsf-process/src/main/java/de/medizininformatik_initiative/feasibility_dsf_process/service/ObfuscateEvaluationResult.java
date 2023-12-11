package de.medizininformatik_initiative.feasibility_dsf_process.service;

import de.medizininformatik_initiative.feasibility_dsf_process.Obfuscator;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.Period;
import org.springframework.beans.factory.InitializingBean;

import java.util.Objects;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.CODESYSTEM_MEASURE_POPULATION;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.CODESYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.MEASURE_REPORT_PERIOD_END;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.MEASURE_REPORT_PERIOD_START;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static java.lang.String.format;
import static org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.SUMMARY;

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
        var obfuscatedFeasibilityCount = obfuscator.obfuscate(extractInitialPopulation(measureReport).getCount());

        var obfuscatedMeasureReport = new MeasureReport()
                .setStatus(COMPLETE)
                .setType(SUMMARY)
                .setMeasure(measureReport.getMeasure())
                .setPeriod(new Period()
                        .setStart(MEASURE_REPORT_PERIOD_START)
                        .setEnd(MEASURE_REPORT_PERIOD_END));

        obfuscatedMeasureReport.getGroupFirstRep().getPopulationFirstRep()
                .setCode(new CodeableConcept().addCoding(new Coding().setSystem(CODESYSTEM_MEASURE_POPULATION)
                        .setCode(CODESYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION)))
                .setCount(obfuscatedFeasibilityCount);

        return obfuscatedMeasureReport;
    }

    private MeasureReportGroupPopulationComponent extractInitialPopulation(MeasureReport measureReport) {
        return measureReport.getGroupFirstRep().getPopulation().stream()
                .filter((p) -> p.getCode().getCoding().stream()
                        .anyMatch((c) -> c.getSystem().equals(CODESYSTEM_MEASURE_POPULATION) && c.getCode()
                                .equals(CODESYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION)))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        format("Missing population with coding '%s' in measure report (id '%s').",
                                CODESYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION, measureReport.getId())));
    }
}
