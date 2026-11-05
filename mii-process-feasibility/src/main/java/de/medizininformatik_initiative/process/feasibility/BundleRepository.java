package de.medizininformatik_initiative.process.feasibility;

import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hl7.fhir.r4.model.Bundle.BundleType.TRANSACTION;

/**
 * Generalisiertes BundleRepository-Nutzung für den DSF-FHIR
 *
 * @author <a href="mailto:dieter.busch@uni-bielefeld.de">Dieter Busch</a>
 */
public interface BundleRepository {
    Logger logger = LoggerFactory.getLogger(BundleRepository.class);

    Bundle storeBundle(Bundle bundle);

    default Bundle storeTransactionBundle(MetadataResource... metadataResources) {
        Bundle bundle = new Bundle().setType(TRANSACTION);
        for (MetadataResource mr : metadataResources) {
            logger.info("Store `{}` `{}` `{}`", mr.getResourceType().name(), mr.getId(), mr.getUrl());
            bundle.addEntry().setResource(mr).getRequest()
                    .setMethod(Bundle.HTTPVerb.POST)
                    .setUrl(mr.getResourceType().name());
        }
        return storeBundle(bundle);
    }


    default String findIdPartInFirstBundleResourceType(Bundle transactionResponse, ResourceType resourceType) {
        try {
            return transactionResponse.getEntry().stream()
                    .filter(e -> e.getResource().getResourceType() == resourceType)
                    .findFirst().filter(bundleEntryComponent ->
                            bundleEntryComponent.getResource() != null &&
                                    bundleEntryComponent.getResource().getIdElement() != null)
                    .map(bundleEntryComponent -> bundleEntryComponent.getResource().getIdElement().getIdPart())
                    .orElse(null);
        } catch (Exception e) {
            logger.error("General Exception: " + e.getMessage(), e);
        }
        return null;
    }
}
