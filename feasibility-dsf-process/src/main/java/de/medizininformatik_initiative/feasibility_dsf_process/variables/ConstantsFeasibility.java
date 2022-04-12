package de.medizininformatik_initiative.feasibility_dsf_process.variables;

public interface ConstantsFeasibility {
    String VARIABLE_MEASURE = "measure";
    String VARIABLE_LIBRARY = "library";
    String VARIABLE_MEASURE_ID = "measure-id";
    String VARIABLE_MEASURE_REPORT = "measure-report";
    String VARIABLE_MEASURE_REPORT_ID = "measure-report-id";
    String VARIABLE_MEASURE_REPORT_MAP = "measure-report-map";
    String VARIABLE_EVALUATION_STRATEGY = "evaluation-strategy";
    String VARIABLE_EVALUATION_OBFUSCATION = "evaluation-obfuscation";

    String CODESYSTEM_FEASIBILITY = "https://www.netzwerk-universitaetsmedizin.de/fhir/CodeSystem/feasibility";
    String CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE = "measure-reference";
    String CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE = "measure-report-reference";
    String CODESYSTEM_MEASURE_POPULATION = "http://terminology.hl7.org/CodeSystem/measure-population";
    String CODESYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION = "initial-population";

    String EXTENSION_DIC_URI = "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/dic";
}
