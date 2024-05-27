package de.medizininformatik_initiative.process.feasibility;

import com.google.common.base.Strings;
import de.medizininformatik_initiative.process.feasibility.client.flare.FlareWebserviceClientSpringConfig;
import de.medizininformatik_initiative.process.feasibility.client.store.StoreClientSpringConfig;
import de.medizininformatik_initiative.process.feasibility.service.SelectRequestTargets;
import de.medizininformatik_initiative.process.feasibility.spring.config.BaseConfig;
import de.medizininformatik_initiative.process.feasibility.spring.config.EnhancedFhirWebserviceClientProviderConfig;
import de.medizininformatik_initiative.process.feasibility.spring.config.EvaluationConfig;
import de.medizininformatik_initiative.process.feasibility.spring.config.FeasibilityConfig;
import dev.dsf.bpe.v1.ProcessPluginDefinition;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.FEASIBILITY_EXECUTE_PROCESS_ID;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.FEASIBILITY_REQUEST_PROCESS_ID;

public class FeasibilityProcessPluginDefinition implements ProcessPluginDefinition {


    private static final Logger logger = LoggerFactory.getLogger(SelectRequestTargets.class);

    public final String version;
    private final LocalDate releaseDate;
    private final String organizationIdentifierValue;
    private static final String ORG_ID_REPLACEMENT = "$organizationIdentifierValue$";

    public FeasibilityProcessPluginDefinition() {
        try (var input = FeasibilityProcessPluginDefinition.class.getClassLoader()
                .getResourceAsStream("app.properties")) {
            checkNotNull(input);
            var props = new Properties();
            props.load(input);
            this.version = props.getProperty("build.version").replaceFirst("-.*$", "");
            this.releaseDate = LocalDate.parse(props.getProperty("build.date"));
        } catch (IOException e) {
            throw new IllegalStateException("Could not load application properties.", e);
        }
        String oiv = System.getenv("DE_MEDIZININFORMATIK_INITIATIVE_FEASIBILITY_DSF_PROCESS_FEASIBILITY_ORGANIZATION_IEDNTIFIER_VALUE");
        logger.info("FeasibilityProcessPluginDefinition from ENV: " + oiv);
        if (Strings.isNullOrEmpty(oiv))
            oiv = "medizininformatik-initiative.de";
        logger.info("FeasibilityProcessPluginDefinition.organizationIdentifierValue: " + oiv);
        this.organizationIdentifierValue = oiv;
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

    private String getOrganizationIdentifierValue() {
        return organizationIdentifierValue;
    }

    @Override
    public Map<String, List<String>> getFhirResourcesByProcessId() {
   //     var aExe = "fhir/ActivityDefinition/feasibilityExecute.xml";
        var aExe = "/opt/bpe/process/feasibilityExecute.xml";


//        try {
//
//            Path path2 = Paths.get(getClass().getClassLoader().getResource("").getPath());
//            logger.info("FeasibilityProcessPluginDefinition.getFhirResourcesByProcessId: " + path2.toAbsolutePath());
//            String content2 = Files.readString(path2, Charset.defaultCharset());
//
//            logger.info("FeasibilityProcessPluginDefinition.getFhirResourcesByProcessId /: "
//                    + content2.substring(0, 20));
//        } catch (Exception e) {
//            logger.info("null",e);
//        }
//        try {
//            Path path = Paths.get(getClass().getClassLoader().getResource(aExe).getPath());
//
//            String content = Files.readString(path, Charset.defaultCharset());
//
//
//            logger.info("FeasibilityProcessPluginDefinition.getFhirResourcesByProcessId read content feasibilityExecute.xml: "
//                    + content.substring(0, 20));
//            logger.info("FeasibilityProcessPluginDefinition.getFhirResourcesByProcessId start replacing and writeStringToFile");
//            FileUtils.writeStringToFile(new File("feasibilityExecute.xml"),
//                    content.replaceAll(ORG_ID_REPLACEMENT, organizationIdentifierValue),
//                    Charset.defaultCharset());
//
//        } catch (Exception e) {
//            logger.info("null",e);
//        }

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
                FEASIBILITY_EXECUTE_PROCESS_ID,
                Arrays.asList(aExe, sTExe, sTResS, vF, cF, sMeasure, sMeasureReport, sLibrary),
                FEASIBILITY_REQUEST_PROCESS_ID,
                Arrays.asList(aReq, sTReq, sTResS, sExtDic, vF, cF, sMeasure, sMeasureReport, sLibrary)
        );
    }
}
