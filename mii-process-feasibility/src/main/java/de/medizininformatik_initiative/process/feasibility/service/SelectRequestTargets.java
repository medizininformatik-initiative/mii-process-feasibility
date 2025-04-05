package de.medizininformatik_initiative.process.feasibility.service;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.constants.CodeSystems;
import dev.dsf.bpe.v2.constants.NamingSystems;
import dev.dsf.bpe.v2.variables.Variables;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE;

public class SelectRequestTargets implements ServiceTask {

    private static final Logger logger = LoggerFactory.getLogger(SelectRequestTargets.class);

    @Override
    public void execute(ProcessPluginApi api, Variables variables) {

        var organizationProvider = api.getOrganizationProvider();
        var client = api.getDsfClientProvider().getLocalDsfClient();
        var parentIdentifier = NamingSystems.OrganizationIdentifier.withValue("medizininformatik-initiative.de");
        var memberOrganizationRole = CodeSystems.OrganizationRole.dic();
        var targets = organizationProvider
                .getOrganizations(parentIdentifier, memberOrganizationRole)
                .stream()
                .filter(Organization::hasEndpoint)
                .filter(Organization::hasIdentifier)
                .map(organization -> {
                    var organizationIdentifier = organization.getIdentifierFirstRep();
                    var path = URI.create(organization.getEndpointFirstRep().getReference()).getPath();
                    var endpoint = client.read(Endpoint.class, path.substring(path.lastIndexOf("/") + 1));
                    return variables.createTarget(organizationIdentifier.getValue(),
                            endpoint.getIdentifierFirstRep().getValue(),
                            endpoint.getAddress(),
                            UUID.randomUUID().toString());
                })
                .collect(Collectors.toList());
        targets.forEach(t -> logger.debug(t.getOrganizationIdentifierValue()));
        variables.setTargets(variables.createTargets(targets));
        variables.setString("measure-id",
                api.getDsfClientProvider().getLocalDsfClient().getBaseUrl()
                        + getMeasureId(api, variables.getStartTask()));
    }

    private String getMeasureId(ProcessPluginApi api, Task task) {

        Optional<Reference> measureRef = api.getTaskHelper()
                .getFirstInputParameterValue(task, CODESYSTEM_FEASIBILITY,
                        CODESYSTEM_FEASIBILITY_VALUE_MEASURE_REFERENCE, Reference.class);

        if (measureRef.isPresent()) {
            return measureRef.get().getReference();
        } else {
            logger.error("Task is missing the measure reference [task: {}]",
                    api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task));
            throw new RuntimeException("Missing measure reference.");
        }
    }
}
