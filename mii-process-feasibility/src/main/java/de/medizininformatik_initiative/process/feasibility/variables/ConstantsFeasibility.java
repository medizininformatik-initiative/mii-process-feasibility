package de.medizininformatik_initiative.process.feasibility.variables;

import java.time.LocalDate;
import java.util.Date;

import static java.time.ZoneOffset.UTC;

public interface ConstantsFeasibility {
    String VARIABLE_MEASURE = "measure";
    String VARIABLE_LIBRARY = "library";
    String VARIABLE_MEASURE_ID = "measure-id";
    String VARIABLE_MEASURE_REPORT = "measure-report";
    String VARIABLE_MEASURE_REPORT_ID = "measure-report-id";
    String VARIABLE_MEASURE_REPORT_MAP = "measure-report-map";
    String VARIABLE_EVALUATION_STRATEGY = "evaluation-strategy";
    String VARIABLE_EVALUATION_OBFUSCATION = "evaluation-obfuscation";
    String VARIABLE_EVALUATION_OBFUSCATION_LAPLACE_SENSITIVITY = "evaluation-obfuscation-laplace-sensitivity";
    String VARIABLE_EVALUATION_OBFUSCATION_LAPLACE_EPSILON = "evaluation-obfuscation-laplace-epsilon";
    String VARIABLE_REQUEST_RATE_BELOW_LIMIT = "request-rate-below-limit";

    String CODESYSTEM_FEASIBILITY = "http://medizininformatik-initiative.de/fhir/CodeSystem/feasibility";
    String CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE = "measure-reference";
    String CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REPORT_REFERENCE = "measure-report-reference";
    String CODESYSTEM_MEASURE_POPULATION = "http://terminology.hl7.org/CodeSystem/measure-population";
    String CODESYSTEM_MEASURE_POPULATION_VALUE_INITIAL_POPULATION = "initial-population";

    String EXTENSION_DIC_URI = "http://medizininformatik-initiative.de/fhir/StructureDefinition/dic";

    String FEASIBILITY_REQUEST_PROCESS_ID = "medizininformatik-initiativede_feasibilityRequest";
    String FEASIBILITY_EXECUTE_PROCESS_ID = "medizininformatik-initiativede_feasibilityExecute";

    Date MEASURE_REPORT_PERIOD_START = Date.from(LocalDate.of(1900, 1, 1).atStartOfDay().toInstant(UTC));
    Date MEASURE_REPORT_PERIOD_END = Date.from(LocalDate.of(2200, 1, 1).atStartOfDay().toInstant(UTC));
    String MEASURE_REPORT_TYPE_POPULATION = "population";

    int CLIENT_TIMEOUT_DEFAULT = 300000;

    String HEADER_PREFER = "Prefer";
    String HEADER_PREFER_RESPOND_ASYNC = "respond-async";
    public String MEASURE_POPULATION = "http://terminology.hl7.org/CodeSystem/measure-population";
    public String INITIAL_POPULATION = "initial-population";
}
