package org.microsoft;

import java.util.Arrays;
import java.util.List;

public class UserInput {
    private String endDate;
    private int summaryPeriod;
    private List<String> repositories;

    public UserInput(String[] args) {
        // Simple CLI parsing
        for (String arg : args) {
            if (arg.startsWith("--date=")) {
                endDate = arg.substring("--date=".length());
            } else if (arg.startsWith("--period=")) {
                summaryPeriod = Integer.parseInt(arg.substring("--period=".length()));
            } else if (arg.startsWith("--repos=")) {
                repositories = Arrays.asList(arg.substring("--repos=".length()).split(","));
            }
        }
    }

    public String getEndDate() { return endDate; }
    public int getSummaryPeriod() { return summaryPeriod; }
    public List<String> getRepositories() { return repositories; }
}
