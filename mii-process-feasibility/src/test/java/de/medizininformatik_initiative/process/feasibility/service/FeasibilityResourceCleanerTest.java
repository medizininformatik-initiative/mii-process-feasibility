package de.medizininformatik_initiative.process.feasibility.service;

import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.medizininformatik_initiative.process.feasibility.service.TestUtil.CQL_CONTENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.condition.MappedCondition.mappedCondition;
import static org.assertj.core.data.Index.atIndex;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeasibilityResourceCleanerTest {

    private FeasibilityResourceCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner = new FeasibilityResourceCleaner();
    }

    @Test
    void cleanLibrary() {
        var library = new Library();
        library.getMeta().setSource("source-172322");
        library.setContent(List.of(new Attachment().setContentType("application/json"),
                new Attachment().setContentType("text/cql")));

        cleaner.cleanLibrary(library);

        assertTrue(library.getMeta().isEmpty());
        assertThat(library.getContent())
                .hasSize(1)
                .has(mappedCondition(Attachment::getContentType, CQL_CONTENT_TYPE), atIndex(0));
    }

    @Test
    public void cleanLibrary_FailsIfCqlContentIsMissing() {
        var library = new Library();

        assertThatThrownBy(() -> cleaner.cleanLibrary(library))
                .hasMessage("Library content of type `text/cql` is missing.");
    }

    @Test
    void cleanMeasure() {
        var measure = new Measure();

        cleaner.cleanMeasure(measure);

        assertTrue(measure.getMeta().isEmpty());
    }
}
