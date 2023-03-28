package de.medizininformatik_initiative.feasibility_dsf_process.service;

import de.medizininformatik_initiative.feasibility_dsf_process.Obfuscator;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.Period;
import org.joda.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.CODESYSTEM_MEASURE_POPULATION;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.CODESYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION;
import static de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus.COMPLETE;
import static org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.SUMMARY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ObfuscateEvaluationResultTest {

    @Captor private ArgumentCaptor<MeasureReport> measureReportCaptor;

    @Mock private FhirWebserviceClientProvider clientProvider;
    @Mock private TaskHelper taskHelper;
    @Mock private ReadAccessHelper readAccessHelper;
    @Mock private DelegateExecution execution;

    private ObfuscateEvaluationResult service;

    @BeforeEach
    public void setUp() {
        var incrementFeasibilityCountObfuscator = new FeasibilityCountIncrementObfuscator();
        service = new ObfuscateEvaluationResult(clientProvider, taskHelper, readAccessHelper,
                incrementFeasibilityCountObfuscator);
    }

    @Test
    public void testDoExecute() throws Exception {
        var feasibilityCount = 5;
        var measureReport = new MeasureReport()
                .setStatus(COMPLETE)
                .setType(SUMMARY)
                .setDate(new Date())
                .setMeasure("http://localhost/fhir/Measure/123456")
                .setPeriod(new Period()
                        .setStart(new LocalDate(1900, 1, 1).toDate())
                        .setEnd(new LocalDate(2100, 1, 1).toDate()));
        var populationGroup = new MeasureReportGroupPopulationComponent()
                .setCount(feasibilityCount)
                .setCode(new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem(CODESYSTEM_MEASURE_POPULATION)
                                .setCode(CODESYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION)));
        measureReport.getGroup()
                .add(new MeasureReportGroupComponent()
                        .setPopulation(List.of(populationGroup)));

        when(execution.getVariable(VARIABLE_MEASURE_REPORT)).thenReturn(measureReport);

        service.execute(execution);
        verify(execution).setVariable(eq(VARIABLE_MEASURE_REPORT), measureReportCaptor.capture());

        var expectedFeasibilityCount = feasibilityCount + 1;
        var expectedObfuscatedMeasureReport = measureReport.copy();
        expectedObfuscatedMeasureReport.getGroupFirstRep().getPopulationFirstRep().setCount(expectedFeasibilityCount);

        var obfuscatedMeasureReport = measureReportCaptor.getValue();
        assertEquals(expectedFeasibilityCount, obfuscatedMeasureReport.getGroupFirstRep().getPopulationFirstRep()
                .getCount());
        assertTrue(expectedObfuscatedMeasureReport.equalsDeep(obfuscatedMeasureReport));
    }

    private static class FeasibilityCountIncrementObfuscator implements Obfuscator<Integer> {
        @Override
        public Integer obfuscate(Integer value) {
            return ++value;
        }
    }
}
