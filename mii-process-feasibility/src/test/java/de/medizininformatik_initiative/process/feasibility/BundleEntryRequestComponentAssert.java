package de.medizininformatik_initiative.process.feasibility;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Condition;
import org.hl7.fhir.r4.model.Bundle;


public class BundleEntryRequestComponentAssert extends AbstractAssert<BundleEntryRequestComponentAssert,
        Bundle.BundleEntryRequestComponent> {

    protected BundleEntryRequestComponentAssert(Bundle.BundleEntryRequestComponent actual) {
        super(actual, BundleEntryRequestComponentAssert.class);
    }

    public BundleEntryRequestComponentAssert hasMethod(Bundle.HTTPVerb method) {
        return has(method(method));
    }

    private static Condition<Bundle.BundleEntryRequestComponent> method(Bundle.HTTPVerb method) {
        return new Condition<>(bundle -> bundle.getMethod() == method, "of method " + method);
    }

    public BundleEntryRequestComponentAssert hasUrl(String url) {
        return has(url(url));
    }

    private static Condition<Bundle.BundleEntryRequestComponent> url(String url) {
        return new Condition<>(bundle -> bundle.getUrl().equals(url), "of URL " + url);
    }
}
