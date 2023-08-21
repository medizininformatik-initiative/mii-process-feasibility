package de.medizininformatik_initiative.feasibility_dsf_process;

import de.medizininformatik_initiative.feasibility_dsf_process.client.flare.FlareWebserviceClientSpringConfig;
import de.medizininformatik_initiative.feasibility_dsf_process.client.store.StoreClientSpringConfig;
import de.medizininformatik_initiative.feasibility_dsf_process.spring.config.BaseConfig;
import de.medizininformatik_initiative.feasibility_dsf_process.spring.config.EnhancedFhirWebserviceClientProviderConfig;
import de.medizininformatik_initiative.feasibility_dsf_process.spring.config.EvaluationConfig;
import de.medizininformatik_initiative.feasibility_dsf_process.spring.config.FeasibilityConfig;
import dev.dsf.bpe.v1.ProcessPluginDefinition;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FeasibilityProcessPluginDefinition implements ProcessPluginDefinition {

    public static final String VERSION = "1.0.0.0";
    private static final LocalDate RELEASE_DATE = LocalDate.of(2023, 7, 3);

    @Override
    public String getName() {
        return "mii-process-feasibility";
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public LocalDate getReleaseDate() {
        return RELEASE_DATE;
    }

    @Override
    public List<String> getProcessModels() {
        return List.of("bpe/feasibilityRequest.bpmn", "bpe/feasibilityExecute.bpmn");
    }

    @Override
    public List<Class<?>> getSpringConfigurations() {
        return List.of(BaseConfig.class, StoreClientSpringConfig.class, FeasibilityConfig.class,
                EnhancedFhirWebserviceClientProviderConfig.class, EvaluationConfig.class,
                FlareWebserviceClientSpringConfig.class);
    }

    @Override
    public Map<String, List<String>> getFhirResourcesByProcessId() {
        var aExe = "fhir/ActivityDefinition/feasibilityExecute.xml";
        var aReq = "fhir/ActivityDefinition/feasibilityRequest.xml";

        var cF = "fhir/CodeSystem/feasibility.xml";

        var sExtDic = "fhir/StructureDefinition/feasibility-extension-dic.xml";

        var sMeasure = "fhir/StructureDefinition/feasibility-measure.xml";
        var sMeasureReport = "fhir/StructureDefinition/feasibility-measure-report.xml";
        var sLibrary = "fhir/StructureDefinition/feasibility-library.xml";

        var sTExe = "fhir/StructureDefinition/feasibility-task-execute.xml";
        var sTReq = "fhir/StructureDefinition/feasibility-task-request.xml";
        var sTResS = "fhir/StructureDefinition/feasibility-task-single-dic-result.xml";

        var vF = "fhir/ValueSet/feasibility.xml";

        return Map.of(
                "medizininformatik-initiativede_feasibilityExecute",
                Arrays.asList(aExe, sTExe, sTResS, vF, cF, sMeasure, sMeasureReport, sLibrary),
                "medizininformatik-initiativede_feasibilityRequest",
                Arrays.asList(aReq, sTReq, sExtDic, vF, cF, sMeasure, sMeasureReport, sLibrary));
    }
}
