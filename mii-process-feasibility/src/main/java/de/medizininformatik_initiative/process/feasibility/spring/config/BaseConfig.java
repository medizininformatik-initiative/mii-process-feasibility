package de.medizininformatik_initiative.process.feasibility.spring.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.medizininformatik_initiative.process.feasibility.FeasibilitySettings;
import dev.dsf.bpe.v1.documentation.ProcessDocumentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.FEASIBILITY_EXECUTE_PROCESS_ID;

@Configuration
public class BaseConfig {

    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{\\w+\\}");

    private static final Logger logger = LoggerFactory.getLogger(BaseConfig.class);

    @ProcessDocumentation(description = "Path to a YAML file containing the configuration for the process",
            processNames = { FEASIBILITY_EXECUTE_PROCESS_ID },
            required = true)
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.configuration.file:#{null}}")
    String configurationFile;

    @ProcessDocumentation(description = """
            The configuration is a YAML formatted text containing the configuration for the FHIR store(s) which will be used for
            executing feasibility queries. It is separated into 2 main parts:

              * the section `general` is for basic plugin settings independent to stores/networks.
              * the section `stores` configures the access to the FHIR store(s) including the evaluation strategy to be used, the base
                URL and authentication options.
              * the section `networks` sets the stores, whether results should be obfuscated or not and the request rate limit for
                each parent organization the local organization is part of and has the role *DIC*.

            > [!IMPORTANT]
            > To use the configuration file, it must be accessible by the BPE process where the feasibility process plugin is
            > deployed. If you're using Docker, ensure the file is mounted inside the BPE Docker container. Configure the path to
            > this file for the feasibility process plugin using the environment variable specified in the **Environment Variables**
            > section.

            The following code block shows the configuration structure with all available options including descriptions and the
            relation to the now unsupported environment variables used in versions before 1.1.0.0:

            ```yaml
            general:

              # The maximum allowed time duration between creation of the initial request task and the start
              # of the request process after which the task execution is skipped, given in ISO 8601 format.
              # Only relevant for DSF instances with role *HRP*.
              requestTaskTimeout: PT5M # env: TASK_REQUEST_TIMEOUT

            stores:

              # A self chosen unique id for the store configuration referenced in the section 'networks' below.
              my-store-1:

                # Defines whether the feasibility shall be evaluated using `cql` or `ccdl` (previously called `structured-query`).
                # Using the latter requires a FLARE instance.
                evaluationStrategy: ccdl  # env: EVALUATION_STRATEGY

                # Base URL to a FHIR server or proxy for feasibility evaluation.
                # This can also be the base URL of a reverse proxy if used.
                baseUrl: https://foo.store  # env: CLIENT_STORE_BASE_URL

                # Timeout for a blocking read / write network operation to a FHIR server without failing in ms.
                # Defaults to 300000 ms (= 5 minutes).
                requestTimeout: 20000  # env: CLIENT_STORE_TIMEOUT_SOCKET

                # Path to a PEM certificate file containing trusted CA certificates used for connecting to a FHIR server.
                # Necessary when using self-signed certificates.
                # NOTE: trusted certificates have to be provided as a single PEM file containing the concatenated certificates.
                #       Before version 1.0.1.0 it had to be provided in the PKCS#12 format (.p12).
                trustedCACertificates: /path/to/trusted-cas.pem  # env: CLIENT_STORE_TRUST_STORE_PATH

                # Username and password for basic authentication against a FHIR server client target.
                # 'password' and 'passwordFile' are mutual exclusive.
                basicAuth:
                  username: foo                                   # env: CLIENT_STORE_AUTH_BASIC_USERNAME
                  password: B4r                                   # env: CLIENT_STORE_AUTH_BASIC_PASSWORD
                  passwordFile: /path/to/basic/auth/password.txt  # env: CLIENT_STORE_AUTH_BASIC_PASSWORD

                # Bearer token used for authentication against a client target. Do not prefix this with `Bearer `!
                # 'token' and 'tokenFile' are mutual exclusive.
                bearerAuth:
                  token: S3cr3tT0k3n                         # env: CLIENT_STORE_AUTH_BEARER_TOKEN
                  tokenFile: /path/to/bearer/auth/token.txt  # env: CLIENT_STORE_AUTH_BEARER_TOKEN

                # Authentication against a OpenID Connect provider to gain access token for FHIR server client target.
                # 'clientPassword' and 'clientPasswordFile' are mutual exclusive.
                oAuth:
                  issuerUrl: https://issuer.example/                # env: CLIENT_STORE_AUTH_OAUTH_ISSUER_URL
                  clientId: foo                                     # env: CLIENT_STORE_AUTH_OAUTH_CLIENT_ID
                  clientPassword: bar                               # env: CLIENT_STORE_AUTH_OAUTH_CLIENT_PASSWORD
                  clientPasswordFile: /path/to/client/password.txt  # env: CLIENT_STORE_AUTH_OAUTH_CLIENT_PASSWORD
                  # Forward proxy to be used to connect to the OpenID Connect provider.
                  # 'username' and 'password' are optional.
                  # 'password' and 'passwordFile' are mutual exclusive.
                  proxy:
                    host: proxy.foo                                  # env: CLIENT_STORE_AUTH_OAUTH_PROXY_HOST
                    port: 1234                                       # env: CLIENT_STORE_AUTH_OAUTH_PROXY_PORT
                    username: foo                                    # env: CLIENT_STORE_AUTH_OAUTH_PROXY_USERNAME
                    password: B4r                                    # env: CLIENT_STORE_AUTH_OAUTH_PROXY_PASSWORD
                    passwordFile: /path/to/oauth/proxy/password.txt  # env: CLIENT_STORE_AUTH_OAUTH_PROXY_PASSWORD
                # Forward proxy to connect to FHIR server.
                # 'username', 'password' and 'passwordFile' are optional.
                # 'password' and 'passwordFile' are mutual exclusive.
                proxy:
                  host: proxy.bar                            # env: CLIENT_STORE_PROXY_HOST
                  port: 4321                                 # env: CLIENT_STORE_PROXY_PORT
                  username: foo                              # env: CLIENT_STORE_PROXY_USERNAME
                  password: B4r                              # env: CLIENT_STORE_PROXY_PASSWORD
                  passwordFile: /path/to/proxy/password.txt  # env: CLIENT_STORE_PROXY_PASSWORD

                # Path to a client certificate PEM file used for authenticating against a FHIR server or proxy.
                # NOTE: The client certificate and the private key are now needed to be provided as PEM files.
                #       Before version 1.0.1.0 it had to be provided in the PKCS#12 format (.p12)
                clientCertificate: /path/to/client-cert.pem                # env: CLIENT_STORE_KEY_STORE_PATH
                privateKey: /path/to/private-key.pem                       # env: CLIENT_STORE_KEY_STORE_PATH
                privateKeyPassword: fooB4r                                 # env: CLIENT_STORE_KEY_STORE_PASSWORD
                privateKeyPasswordFile: /path/to/private/key/password.txt  # env: CLIENT_STORE_KEY_STORE_PASSWORD

            networks:
              # Each parent organization is identified by the same organization identifier as used in the allow list of the DSF.
              medizininformatik-initiative.de:

                # Defines whether the feasibility evaluation result shall be obfuscated.
                # Defaults to 'true'.
                obfuscate: true  # env: EVALUATION_OBFUSCATE

                # Sets the hard limit for the maximum allowed number of requests during the configured rate limit interval after no
                # further requests will be processed.
                # The duration is required to be given in the ISO 8601 format, e.g. "PT1H30M10S" (see
                # https://en.wikipedia.org/wiki/ISO_8601#Durations).
                # Defaults to 999 requests per hour.
                rateLimit:
                  count: 500      # env: RATE_LIMIT_COUNT
                  interval: PT2H  # env: RATE_LIMIT_INTERVAL_DURATION

                # Sets the FHIR store(s) to be used for executing feasibility queries. Results of multiple stores are summed up to
                # a single count.
                # Each id corresponds to an id given to a store configuration in the section 'stores' above.
                stores:
                - my-store-1
            ```

            > [!IMPORTANT]
            > In the `stores` section, each store configuration can use only **one** of the following authentication methods:
            > `basicAuth`, `bearerAuth`, or `oAuth`. A client certificate can either be combined with any of these methods or used
            > on its own.

            Below is an example configuration for 2 parent organizations and 2 FHIR stores. The stores use TLS encrypted
            communication with a self-signed server certificate. One store requires OpenID Connect authentication and CCDL queries,
            while the other uses a client certificate and CQL queries. Note that one of the stores and the OpenID Connect provider
            are only accessible via a forward proxy with basic authentication:



            #### Environment Variable Substitution

            The YAML configuration may contain custom environment variables which will be replaced if an environment variable of the
            same name is found. If no environment variable of the same name is found, no substitution will be done.
            The format for enviromnent variables inside the YAML configuration to be recognized as such is by wrapping the variable
            name with `${}`, e. g. by setting a value for the environment variable `BASE_URL` all occurences of the text
            `${BASE_URL}` in the configuration YAML will be substituted with the value. The following configuration snippet shows
            the usage:

            ```yaml
            stores:
              my-store-1:
                baseUrl: ${BASE_URL}
                evaluationStrategy: cql
            ```

            Nested environment variables are supported. For example, by setting the environment variable `STORE_ID_1` to `foo` and
            `BASE_URL_foo` to `http://foo.bar`, you can use nested variable substitution. The following snippet demonstrates this:

            ```yaml
            stores:
              ${STORE_ID_1}:
                baseUrl: ${BASE_URL_${STORE_ID_1}} # First replaced by '${BASE_URL_foo}' and finally by 'http://foo.bar'
            ```

            > [!NOTE]
            > It is recommended to add an unique prefix to your custom environment variable names to prevent conflicts with existing
            > system and application environment variables.
            """, processNames = {
            FEASIBILITY_EXECUTE_PROCESS_ID }, example = """
                    ``yaml
                    stores:
                      my-production-store:
                        baseUrl: https://store.my-organization.de
                        evaluationStrategy: ccdl
                        requestTimeout: 20000
                        trustedCACertificates: /opt/bpe/trusted-cas.pem
                        oAuth:
                          issuerUrl: https://issuer.internal/realm/foo
                          clientId: foo
                          clientPassword: F00B4r
                          proxy:
                            host: proxy.internal
                            port: 1234
                            username: foo
                            passwordFile: /opt/bpe/proxy.password

                      my-extra-store:
                        baseUrl: https://non-public-store.internal/fhir
                        evaluationStrategy: cql
                        requestTimeout: 5000
                        trustedCACertificates: /opt/bpe/trusted-cas.pem
                        clientCertificate: /opt/bpe/client-cert.pem
                        privateKey: /opt/bpe/private-key.pem
                        privateKeyPasswordFile: /opt/bpe/private-key.password
                        proxy:
                          host: proxy.internal
                          port: 1234
                          username: foo
                          passwordFile: /opt/bpe/proxy.password

                    networks:
                      medizininformatik-initiative.de:
                        obfuscate: true
                        rateLimit:
                          count: 100
                          interval: PT1H
                        stores:
                        - my-production-store

                      my.parent-organization.example.com:
                        obfuscate: false
                        rateLimit:
                          count: 1000
                          interval: PT1S
                        stores:
                        - my-production-store
                        - my-extra-store
                    ``""",
            required = true)
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.configuration:#{null}}")
    String configuration;

    @Autowired Environment environment;

    @Bean
    FeasibilitySettings settings() {
        if ((configurationFile == null || configurationFile.isBlank())
                && (configuration == null || configuration.isBlank())) {
            return FeasibilitySettings.defaultSettings();
        } else {
            var config = configuration;
            if ((config == null || config.isBlank())) {
                try {
                    logger.debug("Loading configuration file '{}'.", configurationFile);
                    config = Files.readString(Paths.get(configurationFile));
                } catch (IOException e) {
                    throw new IllegalArgumentException(
                            "Configuration file '%s' could not be loaded.".formatted(configurationFile),
                            e);
                }
            }
            var substitutedConfig = substituteEnvironmentVariables(config);
            var settings = parseConfig(substitutedConfig, buildSettingsMapper());
            var errors = settings.validate();

            if (errors.isEmpty()) {
                return settings;
            } else {
                throw new IllegalArgumentException(
                        "Configuration contains errors: %s".formatted(errors));
            }
        }
    }

    private FeasibilitySettings parseConfig(String substitutedConfig, YAMLMapper mapper) {
        try {
            return mapper.readValue(substitutedConfig, FeasibilitySettings.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Configuration could not be parsed.", e);
        }
    }

    @Bean
    Map<String, Set<String>> networkStores(FeasibilitySettings settings) {
        return settings.networks()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> Set.copyOf(e.getValue().storeIds())));
    }

    private String substituteEnvironmentVariables(String config) {
        var envVars = ((AbstractEnvironment) environment).getPropertySources().stream()
                .filter(ps -> ps instanceof EnumerablePropertySource)
                .map(EnumerablePropertySource.class::cast)
                .flatMap(eps -> Arrays.stream(eps.getPropertyNames()))
                .collect(Collectors.toMap(n -> n, n -> getProperty(n)))
                .entrySet()
                .stream()
                .filter(e -> e.getValue().isPresent())
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().get()));
        var oldConfig = "";
        while (ENV_VAR_PATTERN.matcher(config).find() && !oldConfig.contentEquals(config)) {
            oldConfig = config;
            config = envVars.entrySet()
                    .stream()
                    .reduce(config, (c, e) -> c.replaceAll("\\$\\{%s\\}".formatted(e.getKey()), e.getValue()),
                    (s1, s2) -> s1);
        }
        return config;
    }

    private Optional<String> getProperty(String name) {
        try {
            return Optional.ofNullable(environment.getProperty(name));
        } catch (IllegalArgumentException e) {
            logger.debug("Could not get environment variable '%s'.".formatted(name), e);
            return Optional.empty();
        }
    }

    private YAMLMapper buildSettingsMapper() {
        return YAMLMapper.builder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .addModule(new JavaTimeModule())
                .build();
    }
}
