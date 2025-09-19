package org.microsoft.analysis;

import org.microsoft.github.data.Commit;
import org.microsoft.github.data.RepositoryData;
import java.util.List;

/**
 * Service interface for AI-powered analysis of repository data.
 */
public interface AiAnalysisService {

    /**
     * Categorizes commits into Bug Fixes, Features, Improvements, and Others.
     *
     * @param commits List of commits to categorize
     * @return CommitCategorization with commits grouped by type
     */
    CommitCategorization categorizeCommits(List<Commit> commits);

    /**
     * Generates a natural language summary of repository activity.
     *
     * @param data Repository data
     * @param categorization Commit categorization results
     * @param startDate Report start date
     * @param endDate Report end date
     * @return Brief summary (2-3 sentences) of repository activity
     */
    String generateSummary(RepositoryData data, CommitCategorization categorization,
                          String startDate, String endDate);
}
