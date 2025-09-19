package org.microsoft;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles command-line argument parsing for the OSS Summary application.
 * Supports both --key=value and --key value formats for flexibility.
 */
public class UserInput {
    private static final Logger logger = LoggerFactory.getLogger(UserInput.class);

    private String endDate;
    private int summaryPeriod;
    private List<String> repositories;

    public UserInput(String[] args) {
        logger.debug("Parsing command-line arguments: {}", Arrays.toString(args));
        parseArguments(args);
        validateInput();
    }

    private void parseArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            try {
                if (arg.startsWith("--date=") || arg.startsWith("--endDate=")) {
                    endDate = arg.substring(arg.indexOf('=') + 1).trim();
                } else if (arg.equals("--date") || arg.equals("--endDate")) {
                    endDate = getNextArgument(args, i, "date");
                    if (endDate != null) i++; // Skip next argument as it was consumed
                } else if (arg.startsWith("--period=")) {
                    String periodStr = arg.substring("--period=".length()).trim();
                    summaryPeriod = parseIntArgument(periodStr, "period");
                } else if (arg.equals("--period")) {
                    String periodStr = getNextArgument(args, i, "period");
                    if (periodStr != null) {
                        summaryPeriod = parseIntArgument(periodStr, "period");
                        i++; // Skip next argument as it was consumed
                    }
                } else if (arg.startsWith("--repos=") || arg.startsWith("--repositories=")) {
                    String reposStr = arg.substring(arg.indexOf('=') + 1).trim();
                    repositories = parseRepositoryList(reposStr);
                } else if (arg.equals("--repos") || arg.equals("--repositories")) {
                    String reposStr = getNextArgument(args, i, "repositories");
                    if (reposStr != null) {
                        repositories = parseRepositoryList(reposStr);
                        i++; // Skip next argument as it was consumed
                    }
                } else if (arg.startsWith("--")) {
                    logger.warn("Unknown command-line option: {}", arg);
                }
            } catch (Exception e) {
                logger.error("Error parsing argument '{}': {}", arg, e.getMessage());
            }
        }
    }

    private String getNextArgument(String[] args, int currentIndex, String argumentName) {
        if (currentIndex + 1 < args.length && !args[currentIndex + 1].startsWith("--")) {
            return args[currentIndex + 1].trim();
        } else {
            logger.warn("Missing value for --{} argument", argumentName);
            return null;
        }
    }

    private int parseIntArgument(String value, String argumentName) {
        try {
            int result = Integer.parseInt(value);
            if (result <= 0) {
                logger.warn("Invalid {} value: {}. Must be positive.", argumentName, value);
                return 0;
            }
            return result;
        } catch (NumberFormatException e) {
            logger.warn("Invalid {} value: '{}'. Must be a number.", argumentName, value);
            return 0;
        }
    }

    private List<String> parseRepositoryList(String reposStr) {
        if (reposStr.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(reposStr.split(","))
                .map(String::trim)
                .filter(repo -> !repo.isEmpty())
                .collect(Collectors.toList());
    }

    private void validateInput() {
        if (endDate != null && !isValidDateFormat(endDate)) {
            logger.warn("Invalid date format: '{}'. Expected format: yyyy-MM-dd", endDate);
        }
        if (repositories != null && !repositories.isEmpty()) {
            logger.info("Command-line repositories specified: {}", repositories);
        }
        if (summaryPeriod > 0) {
            logger.info("Command-line summary period specified: {} days", summaryPeriod);
        }
    }

    private boolean isValidDateFormat(String date) {
        return date.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    // Getters
    public String getEndDate() { return endDate; }
    public int getSummaryPeriod() { return summaryPeriod; }
    public List<String> getRepositories() { return repositories; }
}
