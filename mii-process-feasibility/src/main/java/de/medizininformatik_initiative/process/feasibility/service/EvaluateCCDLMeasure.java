package de.medizininformatik_initiative.process.feasibility.service;

import de.medizininformatik_initiative.process.feasibility.client.flare.FlareWebserviceClient;
import de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_LIBRARY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_MEASURE;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.VARIABLE_REQUESTER_PARENT_ORGANIZATION;

public class EvaluateCCDLMeasure extends AbstractServiceDelegate implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(EvaluateCCDLMeasure.class);
    private static final String STRUCTURED_QUERY_CONTENT_TYPE = "application/json";

    private final Map<String, FlareWebserviceClient> flareClients;
    private Map<String, Set<String>> networkStores;

    public EvaluateCCDLMeasure(Map<String, Set<String>> networkStores,
            Map<String, FlareWebserviceClient> flareClients, ProcessPluginApi api) {
        super(api);
        this.networkStores = networkStores;
        this.flareClients = flareClients;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Objects.requireNonNull(flareClients, "flareClients");
    }

    @Override
    protected void doExecute(DelegateExecution execution, Variables variables)
            throws IOException, InterruptedException {
        logger.info("doExecute evaluate CCDL measure");

        var library = (Library) variables.getResource(VARIABLE_LIBRARY);
        var measureId = ((Measure) variables.getResource(VARIABLE_MEASURE)).getIdElement().getValue();
        var requesterParentOrganization = variables.getString(VARIABLE_REQUESTER_PARENT_ORGANIZATION);
        var taskId = api.getTaskHelper().getLocalVersionlessAbsoluteUrl(variables.getStartTask());


        var structuredQuery = getStructuredQuery(library, measureId, taskId);
        var feasibility = getFeasibility(requesterParentOrganization, structuredQuery, measureId, taskId);

        variables.setInteger(ConstantsFeasibility.VARIABLE_MEASURE_RESULT_CCDL, feasibility);
    }

    private byte[] getStructuredQuery(Library library, String measureId, String taskId) {
        return library.getContent().stream()
                .filter(c -> c.getContentType().equalsIgnoreCase(STRUCTURED_QUERY_CONTENT_TYPE))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Library of Measure %s is missing content of type '%s' [task: %s]".formatted(measureId,
                                STRUCTURED_QUERY_CONTENT_TYPE, taskId)))
                .getData();
    }

    private int getFeasibility(String requesterParentOrganization, byte[] structuredQuery, String measureId,
                               String taskId)
            throws IOException, InterruptedException {
        var results = Optional.ofNullable(networkStores.get(requesterParentOrganization))
                .orElse(Set.of())
                .stream()
                .filter(storeId -> flareClients.containsKey(storeId))
                .collect(Collectors.toMap(id -> id, id -> flareClients.get(id)))
                .entrySet()
                .parallelStream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> {
                    try {
                        logger.debug("Start evaluating Measure {} at store '{}' [task: {}]", measureId, e.getKey(),
                                taskId);
                        var start = System.currentTimeMillis();
                        var result = e.getValue().requestFeasibility(structuredQuery);
                        logger.debug("Finished evaluating Measure {} at store '{}' in {} [task: {}]", measureId,
                                e.getKey(), "%.3fs".formatted((System.currentTimeMillis() - start) / 1000.0d), taskId);
                        return result;
                    } catch (IOException | InterruptedException err) {
                        logger.error(
                                "Error evaluating Measure %s at store '%s' with flare base url '%s' [task: %s]"
                                        .formatted(measureId, e.getKey(), e.getValue().getFlareBaseUrl(), taskId),
                                err);
                        return -1;
                    }
                }))
                .entrySet()
                .stream()
                .collect(Collectors.partitioningBy(e -> e.getValue() >= 0));
        if (results.getOrDefault(false, List.of()).isEmpty()) {
            return results.getOrDefault(true, List.of()).stream()
                    .map(Map.Entry::getValue)
                    .reduce(0, (a, b) -> a + b);
        } else {
            throw new IllegalStateException(
                    "Error(s) executing feasibility query for Measure %s [task: %s]".formatted(measureId, taskId));
        }
    }
}
