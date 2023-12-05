package de.medizininformatik_initiative.process.feasibility;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Condition;
import org.hl7.fhir.r4.model.Bundle;

public class BundleAssert extends AbstractAssert<BundleAssert, Bundle> {

    protected BundleAssert(Bundle actual) {
        super(actual, BundleAssert.class);
    }

    public BundleAssert hasType(String type) {
        return has(type(type));
    }

    private static Condition<Bundle> type(String type) {
        return new Condition<>(bundle -> bundle.getType().toCode().equals(type), "of type " + type);
    }
}
