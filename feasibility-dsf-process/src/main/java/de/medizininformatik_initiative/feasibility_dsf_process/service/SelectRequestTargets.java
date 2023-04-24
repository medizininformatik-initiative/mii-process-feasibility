package de.medizininformatik_initiative.feasibility_dsf_process.service;

import com.google.common.collect.ImmutableMap;
import de.medizininformatik_initiative.feasibility_dsf_process.variables.ConstantsFeasibility;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.Targets;
import org.highmed.dsf.fhir.variables.TargetsValues;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGETS;
import static org.hl7.fhir.instance.model.api.IBaseBundle.LINK_NEXT;

public class SelectRequestTargets extends AbstractServiceDelegate {

    private static final Logger logger = LoggerFactory.getLogger(SelectRequestTargets.class);

    private final OrganizationProvider organizationProvider;
    private final EndpointProvider endpointProvider;


    public SelectRequestTargets(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
            ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider,
                                EndpointProvider endpointProvider) {
        super(clientProvider, taskHelper, readAccessHelper);
        this.organizationProvider = organizationProvider;
        this.endpointProvider = endpointProvider;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();
        Objects.requireNonNull(organizationProvider, "organizationProvider");
        Objects.requireNonNull(endpointProvider, "endpointProvider");
    }

    @Override
    protected void doExecute(DelegateExecution execution) {
        var targets = getAllActiveOrganizations()
                .filter(Organization::hasEndpoint)
                .filter(Organization::hasIdentifier)
                .map(organization -> {
                    String organizationIdentifier = organization.getIdentifierFirstRep().getValue();
                    return Target
                            .createBiDirectionalTarget(organization.getIdentifierFirstRep().getValue(),
                                    endpointProvider.getFirstDefaultEndpoint(organizationIdentifier).get().getId(),
                                    endpointProvider.getFirstDefaultEndpointAddress(organizationIdentifier).get(),
                                    UUID.randomUUID().toString());
                })
                .collect(Collectors.toList());

        targets.forEach(t -> logger.debug(t.getOrganizationIdentifierValue()));

        execution.setVariable(BPMN_EXECUTION_VARIABLE_TARGETS, TargetsValues.create(new Targets(targets)));
        Task task = getCurrentTaskFromExecutionVariables(execution);
        execution.setVariable("measure-id",
                getFhirWebserviceClientProvider().getLocalBaseUrl() + "/" + getMeasureId(task));
    }

    private String getMeasureId(Task task) {
        Optional<Reference> measureRef = getTaskHelper()
                .getFirstInputParameterReferenceValue(task, ConstantsFeasibility.CODESYSTEM_FEASIBILITY,
                        ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE);
        if (measureRef.isPresent()) {
            return measureRef.get().getReference();
        } else {
            logger.error("Task {} is missing the measure reference.", task.getId());
            throw new RuntimeException("Missing measure reference.");
        }
    }

    /**
     * Workaround till {@link OrganizationProvider#getRemoteOrganizations()} is fixed to fetch all resultset pages
     *
     * @return all active {@link Organization}s
     */
    @Deprecated
    private Stream<Organization> getAllActiveOrganizations() {
        return getActiveOrganizations()
                .filter(o -> o.getActive())
                .filter(o -> !o.getIdentifier().stream()
                        .anyMatch(i -> organizationProvider.getLocalIdentifier().getSystem().equals(i.getSystem())
                                && organizationProvider.getLocalIdentifierValue().equals(i.getValue())));
    }

    private Stream<Organization> getActiveOrganizations() {
        Map<String, List<String>> queryParameters = new HashMap<String, List<String>>();
        queryParameters.put("active", Collections.singletonList("true"));

        return fetchAll(queryParameters);
    }

    private Stream<Organization> fetchAll(Map<String, List<String>> queryParameters) {
        Bundle searchResult = getFhirWebserviceClientProvider().getLocalWebserviceClient()
                .searchWithStrictHandling(Organization.class, queryParameters);
        Stream<Organization> organizations = toOrganization(searchResult);
        int page = 1;
        while (searchResult.getLink(LINK_NEXT) != null) {
            page++;
            searchResult = getFhirWebserviceClientProvider().getLocalWebserviceClient()
                    .searchWithStrictHandling(Organization.class, ImmutableMap.<String, List<String>>builder()
                            .putAll(queryParameters).put("_page", List.of(Integer.toString(page))).build());
            organizations = Stream.concat(organizations, toOrganization(searchResult));
        }
        return organizations;
    }

    private Stream<Organization> toOrganization(Bundle searchResult) {
        Objects.requireNonNull(searchResult, "searchResult");

        return searchResult.getEntry().stream().filter(BundleEntryComponent::hasResource)
                .filter(e -> e.getResource() instanceof Organization).map(e -> (Organization) e.getResource());
    }
}
