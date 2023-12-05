package de.medizininformatik_initiative.feasibility_dsf_process.service;

import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Resource;

import java.util.List;

public class FeasibilityResourceCleaner {

    private static final String CQL_CONTENT_TYPE = "text/cql";

    public void cleanLibrary(Library library) {
        stripMeta(library);
        stripNonCqlAttachments(library);
    }

    public void cleanMeasure(Measure measure) {
        stripMeta(measure);
    }

    private void stripMeta(Resource resource) {
        resource.setMeta(new Meta());
    }

    private void stripNonCqlAttachments(Library library) {
        var cqlAttachment = library.getContent()
                .stream()
                .filter(a -> a.getContentType().equalsIgnoreCase(CQL_CONTENT_TYPE))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Library content of type `%s` is missing.".formatted(CQL_CONTENT_TYPE)));

        library.setContent(List.of(cqlAttachment));
    }
}
