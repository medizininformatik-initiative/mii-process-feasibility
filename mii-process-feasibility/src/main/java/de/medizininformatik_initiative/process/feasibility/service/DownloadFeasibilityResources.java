package de.medizininformatik_initiative.process.feasibility.service;

import de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.client.dsf.DsfClient;
import dev.dsf.bpe.v2.variables.Variables;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE;

public class DownloadFeasibilityResources implements ServiceTask {

    private static final Logger logger = LoggerFactory.getLogger(DownloadFeasibilityResources.class);

    @Override
    public void execute(ProcessPluginApi api, Variables variables) {
        var task = variables.getStartTask();

        var measureId = getMeasureId(api, task);
        var client = api.getDsfClientProvider().getDsfClient(measureId.getBaseUrl());
        var bundle = getMeasureAndLibrary(api, measureId, client, task);

        variables.setFhirResource(ConstantsFeasibility.VARIABLE_MEASURE, bundle.getEntry().get(0).getResource());
        variables.setFhirResource(ConstantsFeasibility.VARIABLE_LIBRARY, bundle.getEntry().get(1).getResource());
    }

    private IdType getMeasureId(ProcessPluginApi api, Task task) {
        Optional<Reference> measureRef = api.getTaskHelper()
                .getFirstInputParameterValue(task, CODESYSTEM_FEASIBILITY,
                        CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE, Reference.class);
        if (measureRef.isPresent()) {
            return new IdType(measureRef.get().getReference());
        } else {
            logger.error("Task is missing the measure reference [task: {}]",
                    api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task));
            throw new RuntimeException("Missing measure reference.");
        }
    }

    private Bundle getMeasureAndLibrary(ProcessPluginApi api, IdType measureId, DsfClient client, Task task) {
        try {
            var bundle = client.searchWithStrictHandling(Measure.class,
                    Map.of("_id", Collections.singletonList(measureId.getIdPart()), "_include",
                            Collections.singletonList("Measure:depends-on")));

            if (bundle.getEntry().size() < 2) {
                throw new RuntimeException("Returned search-set contained less then two entries");
            } else if (!bundle.getEntry().get(0).hasResource() ||
                    !(bundle.getEntry().get(0).getResource() instanceof Measure)) {
                throw new RuntimeException("Returned search-set did not contain Measure at index 0");
            } else if (!bundle.getEntry().get(1).hasResource() ||
                    !(bundle.getEntry().get(1).getResource() instanceof Library)) {
                throw new RuntimeException("Returned search-set did not contain Library at index 1");
            }

            return bundle;
        } catch (Exception e) {
            logger.warn("Error while reading Measure with id {} including its Library from {}: {} [task: {}]",
                    measureId.getIdPart(), client.getBaseUrl(), e.getMessage(),
                    api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task));
            throw e;
        }
    }
}
