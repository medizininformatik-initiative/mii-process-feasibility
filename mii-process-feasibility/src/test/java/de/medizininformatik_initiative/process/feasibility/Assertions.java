package de.medizininformatik_initiative.process.feasibility;

import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Bundle;

public interface Assertions {

    static BaseAssert assertThat(Base actual) {
        return new BaseAssert(actual);
    }

    static BundleAssert assertThat(Bundle actual) {
        return new BundleAssert(actual);
    }

    static BundleEntryRequestComponentAssert assertThat(Bundle.BundleEntryRequestComponent actual) {
        return new BundleEntryRequestComponentAssert(actual);
    }
}
