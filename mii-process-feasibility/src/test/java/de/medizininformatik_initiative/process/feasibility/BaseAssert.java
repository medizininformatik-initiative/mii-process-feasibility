package de.medizininformatik_initiative.process.feasibility;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Condition;
import org.hl7.fhir.r4.model.Base;

public class BaseAssert extends AbstractAssert<BaseAssert, Base> {

    protected BaseAssert(Base actual) {
        super(actual, BaseAssert.class);
    }

    private static Condition<Base> deepEqualTo(Base expected) {
        return new Condition<>(actual -> actual.equalsDeep(expected), "deep equal to " + expected);
    }

    public BaseAssert isDeepEqualTo(Base expected) {
        return is(deepEqualTo(expected));
    }
}
