package org.microsoft;

import org.microsoft.github.service.GitHubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

/**
 * Main entry point for the OSS Summary application.
 * This application generates comprehensive reports for GitHub repositories,
 * including commit analysis, pull request summaries, and issue tracking.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            logger.info("Starting OSS Summary application");
            ConfigLoader configLoader = new ConfigLoader("src/main/resources/config.properties");

            String githubToken = System.getenv("GITHUB_TOKEN");
            if (githubToken == null || githubToken.isEmpty()) {
                githubToken = configLoader.getGithubToken();
                logger.debug("Using GitHub token from configuration file");
            } else {
                logger.debug("Using GitHub token from environment variable");
            }

            if (githubToken == null || githubToken.isEmpty()) {
                logger.error("GitHub token not found in environment or configuration");
                System.exit(1);
            }

            GitHubService githubService = new GitHubService(githubToken);

            AzureFoundryAgentClient agentClient = new AzureFoundryAgentClient(
                configLoader.getAzureAgentEndpoint(),
                configLoader.getAzureAgentApiKey(),
                configLoader.getAzureAgentId()
            );

            SummaryGeneratorOrchestrator app = new SummaryGeneratorOrchestrator(configLoader, githubService, agentClient);
            app.run(args);

            logger.info("OSS Summary application completed successfully");
        } catch (IOException e) {
            logger.error("Error initializing application: {}", e.getMessage(), e);
            System.exit(1);
        } catch (Exception e) {
            logger.error("Unexpected error occurred: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}