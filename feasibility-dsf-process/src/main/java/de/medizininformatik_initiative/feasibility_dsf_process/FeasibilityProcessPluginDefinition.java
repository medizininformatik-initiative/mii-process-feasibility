package de.medizininformatik_initiative.feasibility_dsf_process;

import de.medizininformatik_initiative.feasibility_dsf_process.client.flare.FlareWebserviceClientSpringConfig;
import de.medizininformatik_initiative.feasibility_dsf_process.client.store.StoreClientSpringConfig;
import de.medizininformatik_initiative.feasibility_dsf_process.spring.config.BaseConfig;
import de.medizininformatik_initiative.feasibility_dsf_process.spring.config.EnhancedFhirWebserviceClientProviderConfig;
import de.medizininformatik_initiative.feasibility_dsf_process.spring.config.EvaluationConfig;
import de.medizininformatik_initiative.feasibility_dsf_process.spring.config.FeasibilityConfig;
import dev.dsf.bpe.v1.ProcessPluginDefinition;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;

public class FeasibilityProcessPluginDefinition implements ProcessPluginDefinition {

    public final String version;
    private final LocalDate releaseDate;

    public FeasibilityProcessPluginDefinition() {
        try (InputStream input = FeasibilityProcessPluginDefinition.class.getClassLoader()
                .getResourceAsStream("app.properties")) {
            checkNotNull(input);
            Properties props = new Properties();
            props.load(input);

            this.version = props.getProperty("build.version");
            this.releaseDate = LocalDate.parse(props.getProperty("build.date"));
        } catch (IOException e) {
            throw new IllegalStateException("Could not load application properties.", e);
        }
    }

    @Override
    public String getName() {
        return "mii-process-feasibility";
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public LocalDate getReleaseDate() {
        return releaseDate;
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
