package org.microsoft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Configuration loader for the OSS Summary application.
 * Loads configuration from properties file with validation and sensible defaults.
 */
public class ConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

    private static final int DEFAULT_SUMMARY_PERIOD = 7;
    private static final String DEFAULT_OUTPUT_DIR = "output";

    private final int summaryPeriod;
    private final String endDate;
    private final List<String> repositories;
    private final String githubToken;
    private final String azureAgentEndpoint;
    private final String azureAgentApiKey;
    private final String azureAgentId;
    private final String outputDir;
    private final List<String> msteam;

    public ConfigLoader(String configPath) throws IOException {
        logger.debug("Loading configuration from: {}", configPath);

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configPath)) {
            props.load(fis);
        }

        this.summaryPeriod = parseIntProperty(props, "summary.period", DEFAULT_SUMMARY_PERIOD);
        this.endDate = props.getProperty("summary.endDate");
        this.repositories = parseListProperty(props, "summary.repositories");
        this.githubToken = props.getProperty("github.token", "").trim();
        this.azureAgentEndpoint = props.getProperty("azure.agent.endpoint", "").trim();
        this.azureAgentApiKey = props.getProperty("azure.agent.apiKey", "").trim();
        this.azureAgentId = props.getProperty("azure.agent.id", "").trim();
        this.outputDir = props.getProperty("output.dir", DEFAULT_OUTPUT_DIR).trim();
        this.msteam = parseListProperty(props, "msteam");

        validateConfiguration();
        logger.info("Configuration loaded successfully");
    }

    private int parseIntProperty(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warn("Invalid integer value for property '{}': '{}'. Using default: {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    private List<String> parseListProperty(Properties props, String key) {
        String value = props.getProperty(key, "");
        if (value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private void validateConfiguration() {
        if (endDate == null || endDate.trim().isEmpty()) {
            logger.warn("End date not configured");
        }
        if (repositories.isEmpty()) {
            logger.warn("No repositories configured");
        }
        if (githubToken.isEmpty()) {
            logger.warn("GitHub token not configured - will need to be provided via environment variable");
        }
        if (azureAgentEndpoint.isEmpty() || azureAgentApiKey.isEmpty() || azureAgentId.isEmpty()) {
            logger.warn("Azure AI agent configuration incomplete");
        }
    }

    // Getters
    public int getSummaryPeriod() { return summaryPeriod; }
    public String getEndDate() { return endDate; }
    public List<String> getRepositories() { return repositories; }
    public String getGithubToken() { return githubToken; }
    public String getAzureAgentEndpoint() { return azureAgentEndpoint; }
    public String getAzureAgentApiKey() { return azureAgentApiKey; }
    public String getAzureAgentId() { return azureAgentId; }
    public String getOutputDir() { return outputDir; }
    public List<String> getMsteam() { return msteam; }
}
