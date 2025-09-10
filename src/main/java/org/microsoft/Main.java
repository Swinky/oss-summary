package org.microsoft;

import java.io.IOException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        try {
            ConfigLoader configLoader = new ConfigLoader("src/main/resources/config.properties");

            String githubToken = System.getenv("GITHUB_TOKEN");
            if (githubToken == null || githubToken.isEmpty()) {
                githubToken = configLoader.getGithubToken();
            }

            GitHubDataFetcher fetcher = new GitHubDataFetcher(githubToken);

            AzureFoundryAgentClient agentClient = new AzureFoundryAgentClient(
                configLoader.getAzureAgentEndpoint(),
                configLoader.getAzureAgentApiKey(),
                configLoader.getAzureAgentId()
            );

            SummaryGeneratorOrchestrator app = new SummaryGeneratorOrchestrator(configLoader, fetcher, agentClient);
            app.run(args);

        } catch (IOException e) {
            System.err.println("Error initializing application: " + e.getMessage());
            e.printStackTrace();
        }
    }
}