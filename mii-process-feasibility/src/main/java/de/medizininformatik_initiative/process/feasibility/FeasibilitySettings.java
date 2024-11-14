package de.medizininformatik_initiative.process.feasibility;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.DEFAULT_RATE_LIMIT_COUNT;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.DEFAULT_RATE_LIMIT_DURATION;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.DEFAULT_REQUEST_TASK_TIMEOUT;
import static java.lang.String.format;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FeasibilitySettings(Map<String, NetworkSettings> networks, Map<String, StoreSettings> stores,
        GeneralSettings general) {

    public FeasibilitySettings {
        networks = networks == null ? Map.of() : networks;
        stores = stores == null ? Map.of() : stores;
        general = general == null ? new GeneralSettings(DEFAULT_REQUEST_TASK_TIMEOUT) : general;
    }

    public List<FeasibilitySettingsError> validate() {
        return Stream.of(
                general.validate(),
                networks.entrySet().stream().flatMap(e -> e.getValue().validate(e.getKey(), stores)),
                stores.entrySet().stream().flatMap(e -> e.getValue().validate(e.getKey())))
                .flatMap(s -> s)
                .toList();
    }

    private static Object visibleValueToString(Object prop) {
        return prop == null || prop.toString().isBlank() ? "" : "'" + prop + "'";
    }

    private static Object passwordToString(Object prop) {
        return prop == null || prop.toString().isBlank() ? "" : "'***'";
    }

    public static FeasibilitySettings defaultSettings() {
        return new FeasibilitySettings(Map.of(), Map.of(), new GeneralSettings(DEFAULT_REQUEST_TASK_TIMEOUT));
    }

    public static record NetworkSettings(Boolean obfuscate, RateLimitSettings rateLimit,
            @JsonAlias("stores") List<String> storeIds) {

        public NetworkSettings {
            obfuscate = obfuscate == null ? true : obfuscate;
            rateLimit = rateLimit == null ? new RateLimitSettings(DEFAULT_RATE_LIMIT_COUNT, DEFAULT_RATE_LIMIT_DURATION)
                    : rateLimit;
        }

        public Stream<FeasibilitySettingsError> validate(String id, Map<String, StoreSettings> stores) {
            Builder<FeasibilitySettingsError> errors = Stream.builder();

            if (rateLimit != null) {
                rateLimit.validate(id);
            }
            if (storeIds == null || storeIds.isEmpty()) {
                errors.add(new FeasibilitySettingsError("Network '%s' has no store set.".formatted(id)));
            } else {
                storeIds.stream()
                        .filter(storeId -> !stores.containsKey(storeId))
                        .map(storeId -> new FeasibilitySettingsError(
                                "Network '%s' references unknown store id '%s'".formatted(id, storeId)))
                        .forEach(errors);
            }
            return errors.build();
        }
    }

    public static record StoreSettings(URL baseUrl, ProxySettings proxy, EvaluationStrategy evaluationStrategy,
            String trustedCACertificates, String clientCertificate, String privateKey, String privateKeyPassword,
            String privateKeyPasswordFile, Integer requestTimeout, BasicAuth basicAuth, BearerAuth bearerAuth,
            @JsonAlias("oAuth") OAuth oAuth) {

        public StoreSettings {
            if ((privateKeyPassword == null || privateKeyPassword.isEmpty()) && privateKeyPasswordFile != null
                    && !privateKeyPasswordFile.isBlank()) {
                try {
                    privateKeyPassword = Files.readString(Paths.get(privateKeyPasswordFile));
                } catch (IOException e) {
                    throw new IllegalArgumentException(
                            "Could not read private key password file '%s'.".formatted(privateKeyPasswordFile),
                            e);
                }
            }
        }

        public Stream<FeasibilitySettingsError> validate(String storeId) {
            Builder<FeasibilitySettingsError> errors = Stream.builder();

            if (baseUrl == null || baseUrl.toString().isBlank()) {
                errors.add(new FeasibilitySettingsError("Store '%s' has no baseUrl set.".formatted(storeId)));
            };
            if (evaluationStrategy == null) {
                errors.add(
                        new FeasibilitySettingsError("Store '%s' has no evaluationStrategy set.".formatted(storeId)));
            }

            if (clientCertificate != null && !clientCertificate.isBlank()) {
                if (privateKey == null || privateKey.isBlank()) {
                    errors.add(new FeasibilitySettingsError(
                            "Private key for client certificate '%s' is missing.".formatted(clientCertificate)));
                }
                if (privateKeyPassword != null && !privateKeyPassword.isEmpty() && privateKeyPasswordFile != null
                        && !privateKeyPasswordFile.isBlank()) {
                    errors.add(new FeasibilitySettingsError(
                            "Store '%s' has privateKeyPassword '***' and privateKeyPasswordFile '%s' set. Only one of these is allowed."
                                    .formatted(storeId, privateKeyPasswordFile)));
                }
            }
            if (requestTimeout != null && requestTimeout <= 0) {
                errors.add(new FeasibilitySettingsError(
                        "Store '%s' has illegal value for requestTimeout: %d".formatted(requestTimeout)));
            }
            List<Record> auths = Stream.of(basicAuth, oAuth, bearerAuth).filter(a -> a != null).toList();
            if (auths.size() > 1) {
                errors.add(new FeasibilitySettingsError(
                        "Store '%s' contains more than one authentication method: %s".formatted(storeId, auths)));
            }
            if (basicAuth != null) {
                basicAuth.validate(storeId).forEach(errors);
            }
            if (bearerAuth != null) {
                bearerAuth.validate(storeId).forEach(errors);
            }
            if (oAuth != null) {
                oAuth.validate(storeId).forEach(errors);
            }
            if (proxy != null) {
                proxy.validate("Store '%s'".formatted(storeId)).forEach(errors);
            }
            return errors.build();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record RateLimitSettings(Integer count, Duration interval) {

        public Stream<FeasibilitySettingsError> validate(String id) {
            Builder<FeasibilitySettingsError> errors = Stream.builder();

            if (count == null) {
                errors.add(new FeasibilitySettingsError("Network '%s' has no count set for rateLimit.".formatted(id)));
            } else if (count <= 0) {
                errors.add(new FeasibilitySettingsError(
                        "Network '%s' has an invalid count set for rateLimit: %d.".formatted(id, count)));
            }
            if (interval == null) {
                errors.add(
                        new FeasibilitySettingsError("Network '%s' has no interval set for rateLimit.".formatted(id)));
            } else if (interval.isZero() || interval.isNegative()) {
                errors.add(new FeasibilitySettingsError(
                        "Network '%s' has an invalid interval set for rateLimit: %s.".formatted(id, interval)));
            }

            return errors.build();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record ProxySettings(String host, Integer port, String username, String password,
            String passwordFile) {

        public ProxySettings {
            if ((password == null || password.isEmpty()) && passwordFile != null && !passwordFile.isBlank()) {
                try {
                    password = Files.readString(Paths.get(passwordFile));
                } catch (IOException e) {
                    throw new IllegalArgumentException(
                            "Could not read password file '%s for proxy.".formatted(passwordFile), e);
                }
            }
        }

        @Override
        public final String toString() {
            return "proxy(host: %s, port: %s, username: %s, password: %s)".formatted(visibleValueToString(host),
                    visibleValueToString(port), visibleValueToString(username), passwordToString(password));
        }

        public Stream<FeasibilitySettingsError> validate(String prefix) {
            Builder<FeasibilitySettingsError> errors = Stream.builder();

            if (host == null || host.isBlank()) {
                errors.add(new FeasibilitySettingsError("%s has no host set for proxy.".formatted(prefix)));
            }
            if (port == null) {
                errors.add(new FeasibilitySettingsError("%s has no port set for proxy.".formatted(prefix)));
            } else if (port <= 0 || port > 65535) {
                errors.add(
                        new FeasibilitySettingsError("%s has invalid port set for proxy: %d.".formatted(prefix, port)));
            }
            if ((username != null && username.isBlank()) || (password != null && password.isEmpty())) {
                String message = prefix;
                if (username == null || username.isBlank()) {
                    message += " has password but no username set for proxy.";
                } else {
                    message += " has username but no password set for proxy.";
                }
                errors.add(new FeasibilitySettingsError(message));
            }
            if (password != null && !password.isEmpty() && passwordFile != null && !passwordFile.isBlank()) {
                errors.add(new FeasibilitySettingsError(
                        "%s has password '***' and passwordFile '%s' set for proxy. Only one of these is allowed."
                                .formatted(prefix, passwordFile)));
            }

            return errors.build();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record OAuth(URL issuerUrl, String clientId, String clientPassword, String clientPasswordFile,
            ProxySettings proxy) {

        public OAuth {
            if ((clientPassword == null || clientPassword.isEmpty()) && clientPasswordFile != null
                    && !clientPasswordFile.isBlank()) {
                try {
                    clientPassword = Files.readString(Paths.get(clientPasswordFile));
                } catch (IOException e) {
                    throw new IllegalArgumentException(
                            "Could not read client password file '%s' for oAuth.".formatted(clientPasswordFile),
                            e);
                }
            }
        }

        @Override
        public final String toString() {
            return "oAuth(issuerUrl: %s, clientId: %s, clientPassword: %s, %s)".formatted(
                    visibleValueToString(issuerUrl),
                    visibleValueToString(clientId), passwordToString(clientPassword), proxy);
        }

        public Stream<? extends FeasibilitySettingsError> validate(String storeId) {
            Builder<FeasibilitySettingsError> errors = Stream.builder();

            if (issuerUrl == null || issuerUrl.toString().isBlank()) {
                errors.add(
                        new FeasibilitySettingsError("Store '%s' has no issuerUrl set for oAuth.".formatted(storeId)));
            }
            if (clientId == null || clientId.isBlank()) {
                errors.add(
                        new FeasibilitySettingsError("Store '%s' has no clientId set for oAuth.".formatted(storeId)));
            }
            if (clientPassword == null || clientPassword.isBlank()) {
                errors.add(new FeasibilitySettingsError(
                        "Store '%s' has no clientPassword set for oAuth.".formatted(storeId)));
            }
            if (proxy != null) {
                proxy.validate(format("Store '%s' oAuth", storeId)).forEach(errors);
            }
            if (clientPassword != null && !clientPassword.isEmpty() && clientPasswordFile != null
                    && !clientPasswordFile.isBlank()) {
                errors.add(new FeasibilitySettingsError(
                        "Store '%s' has clientPassword '***' and clientPasswordFile '%s' set for oAuth. Only one of these is allowed."
                                .formatted(storeId, clientPasswordFile)));
            }
            return errors.build();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record BearerAuth(String token, String tokenFile) {

        public BearerAuth {
            if ((token == null || token.isEmpty()) && tokenFile != null && !tokenFile.isBlank()) {
                try {
                    token = Files.readString(Paths.get(tokenFile));
                } catch (IOException e) {
                    throw new IllegalArgumentException(
                            "Could not read token file '%s' for bearerAuth.".formatted(tokenFile),
                            e);
                }
            }
        }

        @Override
        public final String toString() {
            return "bearerAuth(token: %s)".formatted(passwordToString(token));
        }

        public Stream<? extends FeasibilitySettingsError> validate(String storeId) {
            Builder<FeasibilitySettingsError> errors = Stream.builder();

            if (token == null || token.isBlank()) {
                errors.add(
                        new FeasibilitySettingsError(format("Store '%s' has no token set for bearerAuth.", storeId)));
            }
            if (token != null && !token.isEmpty() && tokenFile != null && !tokenFile.isBlank()) {
                errors.add(new FeasibilitySettingsError(
                        "Store '%s' has token'***' and tokenFile '%s' set for bearerAuth. Only one of these is allowed."
                                .formatted(storeId, tokenFile)));
            }
            return errors.build();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record BasicAuth(String username, String password, String passwordFile) {

        public BasicAuth {
            if ((password == null || password.isEmpty()) && passwordFile != null && !passwordFile.isBlank()) {
                try {
                    password = Files.readString(Paths.get(passwordFile));
                } catch (IOException e) {
                    throw new IllegalArgumentException(
                            "Could not read password file '%s' for basicAuth.".formatted(passwordFile),
                            e);
                }
            }
        }

        @Override
        public final String toString() {
            return "basicAuth(username: %s, clientPassword: %s)".formatted(visibleValueToString(username),
                    passwordToString(password));
        }

        public Stream<? extends FeasibilitySettingsError> validate(String storeId) {
            Builder<FeasibilitySettingsError> errors = Stream.builder();

            if (username == null || username.isBlank()) {
                errors.add(
                        new FeasibilitySettingsError(
                                "Store '%s' has no username set for basicAuth.".formatted(storeId)));
            }
            if ((password == null || password.isEmpty())) {
                errors.add(
                        new FeasibilitySettingsError(
                                "Store '%s' has no password set for basicAuth.".formatted(storeId)));
            }
            if (password != null && !password.isEmpty() && passwordFile != null && !passwordFile.isBlank()) {
                errors.add(new FeasibilitySettingsError(
                        "Store '%s' has password '***' and passwordFile '%s' set for basicAuth. Only one of these is allowed."
                                .formatted(storeId, passwordFile)));
            }
            return errors.build();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static record GeneralSettings(Duration requestTaskTimeout) {

        public GeneralSettings {
            requestTaskTimeout = requestTaskTimeout == null ? DEFAULT_REQUEST_TASK_TIMEOUT : requestTaskTimeout;
        }

        @Override
        public final String toString() {
            return "general settings: [request-task-timeout: %s]".formatted(requestTaskTimeout);
        }

        public Stream<FeasibilitySettingsError> validate() {
            Builder<FeasibilitySettingsError> errors = Stream.builder();

            if (requestTaskTimeout.isNegative() || requestTaskTimeout.isZero()) {
                errors.add(new FeasibilitySettingsError(
                        "Request task timeout must be greater than 0. Parsed value: %s".formatted(requestTaskTimeout)));
            }
            return errors.build();
        }
    }
}
