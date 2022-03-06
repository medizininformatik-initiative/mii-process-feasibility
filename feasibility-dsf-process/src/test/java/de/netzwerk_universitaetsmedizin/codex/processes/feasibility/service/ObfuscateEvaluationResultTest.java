package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.service;

import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.FeasibilityCountObfuscator;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupComponent;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportGroupPopulationComponent;
import org.hl7.fhir.r4.model.Period;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Date;
import java.util.List;

import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_MEASURE_POPULATION;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.CODESYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION;
import static de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus.COMPLETE;
import static org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.SUMMARY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ObfuscateEvaluationResultTest {

    @Captor
    private ArgumentCaptor<MeasureReport> measureReportCaptor;

    @Mock
    private FeasibilityCountObfuscator feasibilityCountObfuscator;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private ObfuscateEvaluationResult service;

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
        when(feasibilityCountObfuscator.obfuscate(feasibilityCount)).thenCallRealMethod();

        service.execute(execution);
        verify(execution).setVariable(eq(VARIABLE_MEASURE_REPORT), measureReportCaptor.capture());

        var expectedFeasibilityCount = 10;
        var expectedObfuscatedMeasureReport = measureReport.copy();
        expectedObfuscatedMeasureReport.getGroupFirstRep().getPopulationFirstRep().setCount(expectedFeasibilityCount);

        var obfuscatedMeasureReport = measureReportCaptor.getValue();
        assertEquals(expectedFeasibilityCount, obfuscatedMeasureReport.getGroupFirstRep().getPopulationFirstRep()
                .getCount());
        assertTrue(expectedObfuscatedMeasureReport.equalsDeep(obfuscatedMeasureReport));
    }
}
