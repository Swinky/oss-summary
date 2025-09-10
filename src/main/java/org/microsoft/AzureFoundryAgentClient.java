package org.microsoft;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.ai.openai.models.ChatResponseMessage;
import com.azure.core.credential.AzureKeyCredential;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.microsoft.github.data.RepositoryData;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Client for connecting to Azure OpenAI and generating summaries.
 */
public class AzureFoundryAgentClient {
    private final OpenAIClient client;
    private final String deploymentName;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AzureFoundryAgentClient(String endpoint, String apiKey, String deploymentName) {
        this.client = new OpenAIClientBuilder()
            .credential(new AzureKeyCredential(apiKey))
            .endpoint(endpoint)
            .buildClient();
        this.deploymentName = deploymentName;
    }

    /**
     * Sends repository data and a detailed prompt to the agent and returns the summary string.
     */
    public String getSummaryFromAgent(RepositoryData repoData, String startDate, String endDate, int period) {
        try {
            // 1. Build the detailed prompt for the agent
            String prompt = buildSummaryPrompt(repoData, startDate, endDate, period);

            // 2. Serialize the repository data to JSON
            String repoDataJson = objectMapper.writeValueAsString(repoData);

            // 3. Combine prompt and data into a single message
            String fullMessage = prompt + "\n\nRepository Data (JSON):\n" + repoDataJson;

            // 4. Prepare chat messages
            List<ChatRequestMessage> chatMessages = Arrays.asList(
                new ChatRequestSystemMessage("You are an expert open source project summarizer."),
                new ChatRequestUserMessage(fullMessage)
            );
            ChatCompletionsOptions options = new ChatCompletionsOptions(chatMessages);
            options.setMaxTokens(2048);
            options.setTemperature(1d);
            options.setTopP(1d);
            options.setFrequencyPenalty(0d);
            options.setPresencePenalty(0d);

            // 5. Get chat completions
            ChatCompletions completions = client.getChatCompletions(deploymentName, options);
            StringBuilder result = new StringBuilder();
            for (ChatChoice choice : completions.getChoices()) {
                ChatResponseMessage message = choice.getMessage();
                result.append(message.getContent()).append("\n");
            }
            return result.toString().trim();
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to get summary from Azure OpenAI: " + e.getMessage());
            return "[ERROR] Exception: " + e.getMessage();
        }
    }

    /**
     * Builds the prompt for the Azure AI Agent to generate the OSS summary.
     * Reads the prompt template from prompts/<repoName>.txt if available, otherwise uses a default.
     */
    public String buildSummaryPrompt(RepositoryData repoData, String startDate, String endDate, int period) {
        String repoName = repoData.getRepoName();
        String template = loadPromptTemplate(repoName);
        return template
            .replace("{startDate}", startDate)
            .replace("{endDate}", endDate)
            .replace("{period}", String.valueOf(period));
    }

    /**
     * Loads the prompt template from resources/prompt files based on repo name, or project name, or returns a default if not found.
     */
    private String loadPromptTemplate(String repoName) {
        // Try full repo name first
        String filePathFull = "src/main/resources/prompts/" + repoName + ".txt";
        // Extract project name (last part after '/')
        String projectName = repoName.contains("/") ? repoName.substring(repoName.lastIndexOf('/') + 1) : repoName;
        String filePathProject = "src/main/resources/prompts/" + projectName + ".txt";
        try {
            return new String(Files.readAllBytes(Paths.get(filePathFull)), StandardCharsets.UTF_8);
        } catch (Exception e1) {
            try {
                return new String(Files.readAllBytes(Paths.get(filePathProject)), StandardCharsets.UTF_8);
            } catch (Exception e2) {
                System.err.println("[WARN] Could not load prompt template for repo: " + repoName + " or project: " + projectName + ". Using default.");
                return "Summarize the following repository data for the period {startDate} to {endDate} ({period} days):";
            }
        }
    }
}
