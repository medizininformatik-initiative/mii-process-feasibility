package de.medizininformatik_initiative.process.feasibility.spring.config;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.medizininformatik_initiative.process.feasibility.FeasibilitySettings;
import de.medizininformatik_initiative.process.feasibility.FeasibilitySettingsError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class BaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(BaseConfig.class);

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.configuration.file:#{null}}")
    private String configurationFile;

    @Bean
    YAMLMapper buildSettingsMapper() {
        return YAMLMapper.builder()
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .addModule(new JavaTimeModule())
                .build();
    }

    @Bean
    FeasibilitySettings storeSettings(YAMLMapper mapper) {
        if (configurationFile == null || configurationFile.isBlank()) {
            return new FeasibilitySettings(Map.of(), Map.of());
        } else {
            try {
                logger.debug("Parsing configuration file '{}'.", configurationFile);
                FeasibilitySettings settings = mapper.readValue(new FileInputStream(configurationFile),
                        FeasibilitySettings.class);
                List<FeasibilitySettingsError> errors = settings.validate();

                if (errors.isEmpty()) {
                    return settings;
                } else {
                    throw new IllegalArgumentException(
                            "Configuration file '%s' contains errors: %s".formatted(configurationFile, errors));
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        "Configuration file '%s' could not be loaded.".formatted(configurationFile),
                        e);
            }
        }
    }

    @Bean
    Map<String, Set<String>> networkStores(FeasibilitySettings settings) {
        return settings.networks()
                .entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> Set.copyOf(e.getValue().storeIds())));
    }
}
