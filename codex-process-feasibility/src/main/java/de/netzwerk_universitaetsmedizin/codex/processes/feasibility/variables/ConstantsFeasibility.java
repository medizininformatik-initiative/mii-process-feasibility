package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.variables;

public interface ConstantsFeasibility {
    String VARIABLE_MEASURE = "measure";
    String VARIABLE_LIBRARY = "library";
    String VARIABLE_MEASURE_ID = "measure-id";
    String VARIABLE_MEASURE_REPORT = "measure-report";
    String VARIABLE_MEASURE_REPORT_ID = "measure-report-id";
    String VARIABLE_MEASURE_REPORT_MAP = "measure-report-map";

    String CODESYSTEM_FEASIBILITY = "https://www.netzwerk-universitaetsmedizin.de/fhir/CodeSystem/feasibility";
    String CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE = "measure-reference";
    String CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE = "measure-report-reference";

    String EXTENSION_DIC_URI = "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/dic";
}
