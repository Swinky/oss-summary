package org.microsoft;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ConfigLoader {
    private int summaryPeriod;
    private String endDate;
    private List<String> repositories;
    private String githubToken;
    private String azureAgentEndpoint;
    private String azureAgentApiKey;
    private String azureAgentId;
    private String outputDir;

    public ConfigLoader(String configPath) throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream(configPath));
        this.summaryPeriod = Integer.parseInt(props.getProperty("summary.period", "7"));
        this.endDate = props.getProperty("summary.endDate");
        String repos = props.getProperty("summary.repositories", "");
        this.repositories = Arrays.asList(repos.split(","));
        this.githubToken = props.getProperty("github.token", "");
        this.azureAgentEndpoint = props.getProperty("azure.agent.endpoint", "");
        this.azureAgentApiKey = props.getProperty("azure.agent.apiKey", "");
        this.azureAgentId = props.getProperty("azure.agent.id", "");
        this.outputDir = props.getProperty("output.dir", "output");
    }

    public int getSummaryPeriod() { return summaryPeriod; }
    public String getEndDate() { return endDate; }
    public List<String> getRepositories() { return repositories; }
    public String getGithubToken() { return githubToken; }
    public String getAzureAgentEndpoint() { return azureAgentEndpoint; }
    public String getAzureAgentApiKey() { return azureAgentApiKey; }
    public String getAzureAgentId() { return azureAgentId; }
    public String getOutputDir() { return outputDir; }
}
