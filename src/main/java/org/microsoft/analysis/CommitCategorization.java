package org.microsoft.analysis;

import org.microsoft.github.data.Commit;
import java.util.List;

/**
 * Represents the categorization of commits into different types.
 */
public class CommitCategorization {
    private final List<Commit> bugFixes;
    private final List<Commit> features;
    private final List<Commit> improvements;
    private final List<Commit> others;

    public CommitCategorization(List<Commit> bugFixes, List<Commit> features,
                               List<Commit> improvements, List<Commit> others) {
        this.bugFixes = List.copyOf(bugFixes != null ? bugFixes : List.of());
        this.features = List.copyOf(features != null ? features : List.of());
        this.improvements = List.copyOf(improvements != null ? improvements : List.of());
        this.others = List.copyOf(others != null ? others : List.of());
    }

    public List<Commit> getBugFixes() {
        return bugFixes;
    }

    public List<Commit> getFeatures() {
        return features;
    }

    public List<Commit> getImprovements() {
        return improvements;
    }

    public List<Commit> getOthers() {
        return others;
    }

    public int getTotalCount() {
        return bugFixes.size() + features.size() + improvements.size() + others.size();
    }

    @Override
    public String toString() {
        return String.format("CommitCategorization{bugFixes=%d, features=%d, improvements=%d, others=%d}",
                bugFixes.size(), features.size(), improvements.size(), others.size());
    }
}
