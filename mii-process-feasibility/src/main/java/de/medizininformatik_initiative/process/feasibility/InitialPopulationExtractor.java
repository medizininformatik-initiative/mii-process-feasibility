package de.medizininformatik_initiative.process.feasibility;

import org.hl7.fhir.r4.model.MeasureReport;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_MEASURE_POPULATION;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION;
import static java.lang.String.format;

/**
 * Outsourced extractInitialPopulation function for generalized use
 *
 * @author <a href="mailto:dieter.busch@uni-bielefeld.de">Dieter Busch</a>
 */
public interface InitialPopulationExtractor {
    default MeasureReport.MeasureReportGroupPopulationComponent extractInitialPopulation(MeasureReport measureReport) {
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
