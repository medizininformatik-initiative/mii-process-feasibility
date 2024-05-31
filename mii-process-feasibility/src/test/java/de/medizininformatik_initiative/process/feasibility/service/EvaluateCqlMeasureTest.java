package de.medizininformatik_initiative.process.feasibility.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_ID;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE_REPORT;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EvaluateCqlMeasureTest {

    private static final String MEASURE_POPULATION = "http://terminology.hl7.org/CodeSystem/measure-population";
    private static final String INITIAL_POPULATION = "initial-population";
    private static final String MEASURE_ID = "id-145128";

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private IGenericClient storeClient;

    @Mock
    private DelegateExecution execution;

    @Mock
    private Variables variables;

    @InjectMocks
    private EvaluateCqlMeasure service;

    @BeforeEach
    public void setUp() {
        when(variables.getString(VARIABLE_MEASURE_ID)).thenReturn(MEASURE_ID);
    }

    @Test
    public void testDoExecute() {
        var report = new MeasureReport();
        var code = new CodeableConcept();
        var coding = code.getCodingFirstRep();
        coding.setSystem(MEASURE_POPULATION);
        coding.setCode(INITIAL_POPULATION);
        report.getGroupFirstRep().getPopulationFirstRep().setCode(code).setCount(0);
        when(evaluateMeasure()).thenReturn(report);

        service.doExecute(execution, variables);

        verify(variables).setResource(VARIABLE_MEASURE_REPORT, report);
    }

    private MeasureReport evaluateMeasure() {
        return storeClient.operation().onInstance("Measure/" + MEASURE_ID)
                .named("evaluate-measure")
                .withParameter(ArgumentMatchers.<Class<org.hl7.fhir.r4.model.Parameters>>any(), eq("periodStart"), any(DateType.class))
                .andParameter(eq("periodEnd"), any(DateType.class))
                .useHttpGet()
                .returnResourceType(MeasureReport.class)
                .execute();
    }

    @Test
    void testFailsOnEmptyMeasureReport() {
        when(evaluateMeasure()).thenReturn(new MeasureReport());

        assertThatThrownBy(() -> service.doExecute(execution, variables))
                .hasMessage("Missing MeasureReport group");
    }

    @Test
    void testFailsOnMissingPopulation() {
        var report = new MeasureReport();
        report.getGroupFirstRep().setCode(new CodeableConcept().setText("foo"));
        when(evaluateMeasure()).thenReturn(report);

        assertThatThrownBy(() -> service.doExecute(execution, variables))
                .hasMessage("Missing MeasureReport population");
    }

    @Test
    void testFailsOnMissingPopulationCode() {
        var report = new MeasureReport();
        report.getGroupFirstRep().getPopulationFirstRep().setCount(0);
        when(evaluateMeasure()).thenReturn(report);

        assertThatThrownBy(() -> service.doExecute(execution, variables))
                .hasMessage("Missing MeasureReport population code");
    }

    @Test
    void testFailsOnWrongPopulationCode() {
        var report = new MeasureReport();
        report.getGroupFirstRep().getPopulationFirstRep().setCode(new CodeableConcept().setText("foo"));
        when(evaluateMeasure()).thenReturn(report);

        assertThatThrownBy(() -> service.doExecute(execution, variables))
                .hasMessage("Missing MeasureReport initial-population code");
    }

    @Test
    void testFailsOnMissingPopulationCount() {
        var report = new MeasureReport();
        var code = new CodeableConcept();
        var coding = code.getCodingFirstRep();
        coding.setSystem(MEASURE_POPULATION);
        coding.setCode(INITIAL_POPULATION);
        report.getGroupFirstRep().getPopulationFirstRep().setCode(code);
        when(evaluateMeasure()).thenReturn(report);

        assertThatThrownBy(() -> service.doExecute(execution, variables))
                .hasMessage("Missing MeasureReport population count");
    }
}
