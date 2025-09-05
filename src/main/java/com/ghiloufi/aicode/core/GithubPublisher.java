
package com.ghiloufi.aicode.core;


import com.ghiloufi.aicode.domain.DiffBundle;
import com.ghiloufi.aicode.github.GithubClient;
import com.ghiloufi.aicode.model.ReviewResult;
import com.ghiloufi.aicode.util.UnifiedDiffMapper;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publie un commentaire résumé + des commentaires inline sur une Pull Request GitHub.
 */
public class GithubPublisher {

    private static final Logger log = LoggerFactory.getLogger(GithubPublisher.class);

    private final GithubClient gh;

    public GithubPublisher(GithubClient gh) {
        this.gh = gh;
    }

    private static String nvl(String val, String def) {
        return (val == null) ? def : val;
    }

    // -------------------------
    // Helpers
    // -------------------------

    private static int safeSize(List<?> list) {
        return (list == null) ? 0 : list.size();
    }

    /**
     * Publie le résumé et les commentaires inline issus du résultat de review.
     *
     * @param pr     numéro de la Pull Request
     * @param rr     résultat agrégé de la review (LLM + SAST)
     * @param bundle bundle contenant le diff unifié (pour calcul des positions dans le patch)
     */
    public void publish(int pr, ReviewResult rr, DiffBundle bundle) {
        if (rr == null) {
            log.warn("ReviewResult est null, rien à publier.");
            return;
        }

        // 1) Commentaire résumé (markdown propre)
        final String summaryText = (rr.summary == null || rr.summary.isBlank())
                ? "No summary"
                : rr.summary.trim();

        StringBuilder body = new StringBuilder();
        body.append("### 🤖 AI Review Summary").append("\n\n")
                .append(summaryText).append("\n\n")
                .append("**Findings:** ").append(safeSize(rr.issues)).append(" issue(s)").append("\n");

        gh.postIssueComment(pr, body.toString());

        // 2) Commentaires inline
        if (bundle == null || bundle.diff() == null) {
            log.warn("Diff bundle manquant : publication inline impossible. Les findings restent dans le résumé.");
            return;
        }

        UnifiedDiffMapper mapper = new UnifiedDiffMapper(bundle.diff());
        List<GithubClient.ReviewComment> comments = new ArrayList<>();

        if (rr.issues != null) {
            for (ReviewResult.Issue issue : rr.issues) {
                if (issue == null) continue;

                String file = nvl(issue.file, "");
                int startLine = issue.start_line;
                int position = mapper.positionFor(file, startLine);

                if (position > 0) {
                    String commentBody = buildInlineComment(issue);
                    comments.add(new GithubClient.ReviewComment(file, position, commentBody));
                } else {
                    log.warn("Could not map inline position for {}:{}; will include in summary only",
                            file, startLine);
                }
            }
        }

        if (!comments.isEmpty()) {
            gh.createReview(pr, comments);
        }
    }

    private String buildInlineComment(ReviewResult.Issue issue) {
        String severity = nvl(issue.severity, "info").toUpperCase();
        String title = nvl(issue.title, "");
        String rationale = nvl(issue.rationale, "");
        String suggestion = nvl(issue.suggestion, "").trim();

        StringBuilder sb = new StringBuilder();
        sb.append("**").append(severity).append("** ").append(title).append("\n\n");

        if (!rationale.isBlank()) {
            sb.append(rationale).append("\n\n");
        }

        if (!suggestion.isBlank()) {
            sb.append("**Suggestion:**\n").append(suggestion).append("\n");
        }

        if (issue.references != null && !issue.references.isEmpty()) {
            sb.append("\n**References:** ");
            sb.append(String.join(", ", issue.references));
        }

        return sb.toString().trim();
    }
}
