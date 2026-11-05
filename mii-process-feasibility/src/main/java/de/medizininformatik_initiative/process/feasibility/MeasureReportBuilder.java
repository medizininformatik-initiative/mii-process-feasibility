package de.medizininformatik_initiative.process.feasibility;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Period;

import java.util.Date;
import java.util.List;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.*;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION;
import static org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus.COMPLETE;
import static org.hl7.fhir.r4.model.MeasureReport.MeasureReportType.SUMMARY;

/**
 * Outsourced buildMeasureReport function for generalized use
 *
 * @author <a href="mailto:dieter.busch@uni-bielefeld.de">Dieter Busch</a>
 */
public interface MeasureReportBuilder {
    default MeasureReport buildMeasureReport(String measureRef, int feasibility) {
        var measureReport = new MeasureReport()
                .setStatus(COMPLETE)
                .setType(SUMMARY)
                .setDate(new Date())
                .setMeasure(measureRef)
                .setPeriod(new Period()
                        .setStart(MEASURE_REPORT_PERIOD_START)
                        .setEnd(MEASURE_REPORT_PERIOD_END));

        var populationGroup = new MeasureReport.MeasureReportGroupPopulationComponent()
                .setCount(feasibility)
                .setCode(new CodeableConcept()
                        .addCoding(new Coding()
                                .setSystem(CODESYSTEM_MEASURE_POPULATION)
                                .setCode(CODESYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION)));

        measureReport.getGroup()
                .add(new MeasureReport.MeasureReportGroupComponent()
                        .setPopulation(List.of(populationGroup)));

        return measureReport;
    }
}
