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
    public String getSummaryFromAgent(RepositoryData repoData, String startDate, String endDate, int period, String msteam) {
        try {
            // Debug: Print input parameters
            System.out.println("[DEBUG] getSummaryFromAgent - Start Date: " + startDate);
            System.out.println("[DEBUG] getSummaryFromAgent - End Date: " + endDate);
            System.out.println("[DEBUG] getSummaryFromAgent - Period: " + period);
            System.out.println("[DEBUG] getSummaryFromAgent - MS Team Members: " + msteam);

            // 1. Build the detailed prompt for the agent
            String prompt = buildSummaryPrompt(repoData, startDate, endDate, period, msteam);

            // Debug: Print the generated prompt
            System.out.println("[DEBUG] getSummaryFromAgent - Generated Prompt:");
            System.out.println("=== PROMPT START ===");
            System.out.println(prompt);
            System.out.println("=== PROMPT END ===");

            // 2. Serialize the repository data to JSON
            String repoDataJson = objectMapper.writeValueAsString(repoData);

            // Debug: Print repository data
            System.out.println("[DEBUG] getSummaryFromAgent - Repository Data (JSON):");
            System.out.println("=== REPO DATA START ===");
            System.out.println(repoDataJson);
            System.out.println("=== REPO DATA END ===");

            // 3. Combine prompt and data into a single message
            String fullMessage = prompt + "\n\nRepository Data (JSON):\n" + repoDataJson;

            // Debug: Print full message length for token estimation
            System.out.println("[DEBUG] getSummaryFromAgent - Full Message Length: " + fullMessage.length() + " characters");

            // 4. Prepare chat messages
            List<ChatRequestMessage> chatMessages = Arrays.asList(
                new ChatRequestSystemMessage("You are an expert github repo commits classifer and summarizer."),
                new ChatRequestUserMessage(fullMessage)
            );
            ChatCompletionsOptions options = new ChatCompletionsOptions(chatMessages);
            options.setMaxTokens(16384);
            options.setTemperature(0.3);
            options.setTopP(1d);
            options.setFrequencyPenalty(0d);
            options.setPresencePenalty(0d);

            System.out.println("[DEBUG] getSummaryFromAgent - Sending request to Azure OpenAI...");

            // 5. Get chat completions
            ChatCompletions completions = client.getChatCompletions(deploymentName, options);
            StringBuilder result = new StringBuilder();
            for (ChatChoice choice : completions.getChoices()) {
                ChatResponseMessage message = choice.getMessage();
                result.append(message.getContent()).append("\n");
            }

            String finalResult = result.toString().trim();

            // Debug: Print the result
            System.out.println("[DEBUG] getSummaryFromAgent - Result from Azure OpenAI:");
            System.out.println("=== RESULT START ===");
            System.out.println(finalResult);
            System.out.println("=== RESULT END ===");
            System.out.println("[DEBUG] getSummaryFromAgent - Result Length: " + finalResult.length() + " characters");

            return finalResult;
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to get summary from Azure OpenAI: " + e.getMessage());
            e.printStackTrace(); // Add stack trace for better debugging
            return "[ERROR] Exception: " + e.getMessage();
        }
    }

    /**
     * Sends a specific prompt to the agent with token limit control.
     * This method is used for focused AI tasks like categorization and summary generation.
     */
    public String getSummaryFromAgent(RepositoryData repoData, String startDate, String endDate, int period, String msteam, String customPrompt, int maxTokens) {
        try {
            System.out.println("[DEBUG] getSummaryFromAgent (custom) - Custom Prompt Length: " + customPrompt.length());
            System.out.println("[DEBUG] getSummaryFromAgent (custom) - Max Tokens: " + maxTokens);

            // Debug: Print the custom prompt being sent
            System.out.println("[DEBUG] getSummaryFromAgent (custom) - Custom Prompt:");
            System.out.println("=== CUSTOM PROMPT START ===");
            System.out.println(customPrompt);
            System.out.println("=== CUSTOM PROMPT END ===");

            // Prepare chat messages with custom prompt
            List<ChatRequestMessage> chatMessages = Arrays.asList(
                new ChatRequestSystemMessage("You are an expert github repo commits classifier and summarizer."),
                new ChatRequestUserMessage(customPrompt)
            );

            ChatCompletionsOptions options = new ChatCompletionsOptions(chatMessages);
            options.setMaxTokens(maxTokens);
            options.setTemperature(0.3); // Lower temperature for more consistent results
            options.setTopP(1.0);
            options.setFrequencyPenalty(0.0);
            options.setPresencePenalty(0.0);

            System.out.println("[DEBUG] getSummaryFromAgent (custom) - Sending request to Azure OpenAI...");
            System.out.println("[DEBUG] getSummaryFromAgent (custom) - Request Parameters: MaxTokens=" + maxTokens + ", Temperature=0.3, TopP=1.0");

            ChatCompletions completions = client.getChatCompletions(deploymentName, options);
            StringBuilder result = new StringBuilder();
            for (ChatChoice choice : completions.getChoices()) {
                ChatResponseMessage message = choice.getMessage();
                result.append(message.getContent()).append("\n");
            }

            String finalResult = result.toString().trim();

            // Enhanced response logging with more details
            System.out.println("[DEBUG] getSummaryFromAgent (custom) - Request completed successfully");
            System.out.println("[DEBUG] getSummaryFromAgent (custom) - Response Length: " + finalResult.length() + " characters");
            System.out.println("[DEBUG] getSummaryFromAgent (custom) - Response Tokens (estimated): " + (finalResult.length() / 4));

            // Debug: Print the result from custom prompt
            System.out.println("[DEBUG] getSummaryFromAgent (custom) - AI Response for Prompt:");
            System.out.println("=== AI RESPONSE START ===");
            System.out.println(finalResult);
            System.out.println("=== AI RESPONSE END ===");

            return finalResult;
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to get summary from Azure OpenAI: " + e.getMessage());
            e.printStackTrace();
            return "[ERROR] Exception: " + e.getMessage();
        }
    }

    /**
     * Builds the prompt for the Azure AI Agent to generate the OSS summary.
     * Reads the prompt template from prompts/<repoName>.txt if available, otherwise uses a default.
     */
    public String buildSummaryPrompt(RepositoryData repoData, String startDate, String endDate, int period, String msteam) {
        String repoName = repoData.getRepoName();
        String template = loadPromptTemplate(repoName);
        return template
            .replace("{startDate}", startDate)
            .replace("{endDate}", endDate)
            .replace("{period}", String.valueOf(period))
            .replace("{msteamMembers}", msteam);
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
                return "Summarize the following repository data for the period {startDate} to {endDate} ({period} days). " +
                       "Microsoft team members for reference: {msteamMembers}";
            }
        }
    }
}
