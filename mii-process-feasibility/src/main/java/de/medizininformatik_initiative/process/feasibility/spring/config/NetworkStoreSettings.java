package de.medizininformatik_initiative.process.feasibility.spring.config;

import de.medizininformatik_initiative.process.feasibility.EvaluationStrategy;

import java.net.URI;

public record NetworkStoreSettings(String id, EvaluationStrategy evaluationStrategy, Store store) {

    public record Basic(String username, String password) {
    };

    public record Auth(String bearerToken, Basic basic) {
    };

    public record Proxy(URI host, Integer port) {
    };

    public record Store(Integer timeout, Proxy proxy, Auth auth) {
    };
}
