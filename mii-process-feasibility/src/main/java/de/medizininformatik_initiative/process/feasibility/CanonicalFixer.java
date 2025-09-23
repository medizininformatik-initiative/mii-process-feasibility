package de.medizininformatik_initiative.process.feasibility;

import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;

import java.util.ArrayList;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Outsourced fixCanonical function for generalized use
 *
 * @author <a href="mailto:dieter.busch@uni-bielefeld.de">Dieter Busch</a>
 */
public interface CanonicalFixer {
    Pattern MEASURE_URL_PATTERN = Pattern.compile("(.+)/Measure/(.+)");
    Pattern LIBRARY_URL_PATTERN = Pattern.compile("urn:uuid:(.+)");

    default void fixCanonical(Measure measure, Library library) {
        var measureUrlMatcher = MEASURE_URL_PATTERN.matcher(measure.getUrl());
        var libraryUrlMatcher = LIBRARY_URL_PATTERN.matcher(library.getUrl());
        if (measureUrlMatcher.find() && libraryUrlMatcher.find()) {
            var base = measureUrlMatcher.group(1);
            var libraryId = libraryUrlMatcher.group(1);
            var libraryUrl = base + "/Library/" + libraryId;
            measure.setLibrary(new ArrayList<>());
            measure.addLibrary(libraryUrl);
            library.setUrl(libraryUrl);
            library.setName(libraryId);
            library.setVersion("1.0.0");
            if (library.getContent() != null && !library.getContent().isEmpty()) {
                var data = new String(library.getContent().get(0).getData(), UTF_8);
                var rest = data.split("\n", 2)[1];
                var newData = "library \"%s\" version '1.0.0'\n".formatted(libraryId) + rest;
                library.getContent().get(0).setData(newData.getBytes(UTF_8));
            }
        }
    }
}
