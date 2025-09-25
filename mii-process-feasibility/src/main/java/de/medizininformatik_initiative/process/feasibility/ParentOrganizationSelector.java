package de.medizininformatik_initiative.process.feasibility;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.variables.Variables;
import org.hl7.fhir.r4.model.Organization;

import java.util.stream.Collectors;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.HRP;

public interface ParentOrganizationSelector {

    default Organization getRequesterOrganization(Variables variables, ProcessPluginApi api) {
        var orgIdentifier = variables.getStartTask().getRequester().getIdentifier();

        return api.getOrganizationProvider().getOrganization(orgIdentifier)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Task requester organization with identifer '%s' is unknown.".formatted(orgIdentifier)));
    }

    default String getParentOrganizationId(Organization requester, FeasibilitySettings settings, ProcessPluginApi api) {
        var parentIds = settings.networks().keySet();
        return parentIds.parallelStream()
                .collect(Collectors.toMap(p -> p, p -> api.getOrganizationProvider().getOrganizations(p, HRP)))
                .entrySet()
                .stream()
                .filter(e -> e.getValue().stream().anyMatch(o -> o.equalsShallow(requester)))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException(
                        "None of the configured networks %s contain the task requester organization (%s) with role '%s'."
                                .formatted(parentIds, requester.getIdentifierFirstRep().getValue(), HRP.getCode())))
                .getKey();
    }
}
