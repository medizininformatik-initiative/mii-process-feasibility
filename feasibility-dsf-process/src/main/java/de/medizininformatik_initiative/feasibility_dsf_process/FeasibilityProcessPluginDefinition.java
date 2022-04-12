package de.medizininformatik_initiative.feasibility_dsf_process;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.feasibility_dsf_process.client.flare.FlareWebserviceClientSpringConfig;
import de.medizininformatik_initiative.feasibility_dsf_process.spring.config.EnhancedFhirWebserviceClientProviderConfig;
import de.medizininformatik_initiative.feasibility_dsf_process.spring.config.EvaluationConfig;
import de.medizininformatik_initiative.feasibility_dsf_process.spring.config.FeasibilityConfig;
import de.medizininformatik_initiative.feasibility_dsf_process.spring.config.StoreConfig;
import org.highmed.dsf.bpe.ProcessPluginDefinition;
import org.highmed.dsf.fhir.resources.AbstractResource;
import org.highmed.dsf.fhir.resources.ActivityDefinitionResource;
import org.highmed.dsf.fhir.resources.CodeSystemResource;
import org.highmed.dsf.fhir.resources.ResourceProvider;
import org.highmed.dsf.fhir.resources.StructureDefinitionResource;
import org.highmed.dsf.fhir.resources.ValueSetResource;
import org.springframework.core.env.PropertyResolver;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class FeasibilityProcessPluginDefinition implements ProcessPluginDefinition {

    public static final String VERSION = "0.1.0";

    @Override
    public String getName() {
        return "feasibility-dsf-process";
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public Stream<String> getBpmnFiles() {
        return Stream.of("bpe/requestSimpleFeasibility.bpmn", "bpe/executeSimpleFeasibility.bpmn");
    }

    @Override
    public Stream<Class<?>> getSpringConfigClasses() {
        return Stream.of(StoreConfig.class, FeasibilityConfig.class, EnhancedFhirWebserviceClientProviderConfig.class,
                EvaluationConfig.class, FlareWebserviceClientSpringConfig.class);
    }

    @Override
    public ResourceProvider getResourceProvider(FhirContext fhirContext, ClassLoader classLoader,
                                                PropertyResolver propertyResolver) {
        var aExe = ActivityDefinitionResource.file("fhir/ActivityDefinition/executeSimpleFeasibility.xml");
        var aReq = ActivityDefinitionResource.file("fhir/ActivityDefinition/requestSimpleFeasibility.xml");

        var cF = CodeSystemResource.file("fhir/CodeSystem/feasibility.xml");

        var sExtDic = StructureDefinitionResource
                .file("fhir/StructureDefinition/feasibility-extension-dic.xml");

        var sMeasure = StructureDefinitionResource
                .file("fhir/StructureDefinition/feasibility-measure.xml");
        var sMeasureReport = StructureDefinitionResource
                .file("fhir/StructureDefinition/feasibility-measure-report.xml");
        var sLibrary = StructureDefinitionResource
                .file("fhir/StructureDefinition/feasibility-library.xml");

        var sTExe = StructureDefinitionResource
                .file("fhir/StructureDefinition/feasibility-task-execute-simple-feasibility.xml");
        var sTReq = StructureDefinitionResource
                .file("fhir/StructureDefinition/feasibility-task-request-simple-feasibility.xml");
        var sTResS = StructureDefinitionResource
                .file("fhir/StructureDefinition/feasibility-task-single-dic-result-simple-feasibility.xml");

        var vF = ValueSetResource.file("fhir/ValueSet/feasibility.xml");

        Map<String, List<AbstractResource>> resourcesByProcessKeyAndVersion = Map.of(
                "wwwnetzwerk-universitaetsmedizinde_executeSimpleFeasibility/" + VERSION,
                Arrays.asList(aExe, sTExe, sTResS, vF, cF, sMeasure, sMeasureReport, sLibrary),
                "wwwnetzwerk-universitaetsmedizinde_requestSimpleFeasibility/" + VERSION,
                Arrays.asList(aReq, sTReq, sExtDic, vF, cF, sMeasure, sMeasureReport, sLibrary));

        return ResourceProvider.read(VERSION,
                () -> fhirContext.newXmlParser().setStripVersionsFromReferences(false),
                classLoader, propertyResolver, resourcesByProcessKeyAndVersion);
    }
}
