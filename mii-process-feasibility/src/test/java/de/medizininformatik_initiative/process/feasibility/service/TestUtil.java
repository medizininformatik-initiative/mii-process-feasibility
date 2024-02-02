package de.medizininformatik_initiative.process.feasibility.service;

import org.assertj.core.api.Condition;

public interface TestUtil {

    Condition<String> CQL_CONTENT_TYPE = new Condition<>("text/cql"::equals, "content-type");
}
