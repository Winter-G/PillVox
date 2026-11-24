package com.wynter.pillvox.domain;

import com.wynter.pillvox.data.Medicine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * MATCHING: turns "what the camera saw" into "which medicine is this?".
 * Plain Java (no Android code), so you can unit-test it on your laptop and report results in your paper.
 *
 *   score = 0.6 * imprintSimilarity + 0.2 * shapeMatch + 0.2 * colourMatch      (0.0 .. 1.0)
 * If the best score is below THRESHOLD, best() returns null = "not sure". We NEVER guess.
 */
public final class PillMatcher {
    private PillMatcher() { }

    /** Tune on your own test images. Higher = safer but asks for more re-scans. */
    public static final double THRESHOLD = 0.75;

    public static class Candidate {
        public final Medicine medicine; public final double score;
        Candidate(Medicine m, double s) { medicine = m; score = s; }
    }

    /** "AB 5", "ab-5" and "AB5" all become "AB5". */
    public static String normalise(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toUpperCase().toCharArray()) if (Character.isLetterOrDigit(c)) b.append(c);
        return b.toString();
    }

    /** Levenshtein edit distance: the fewest single-character edits that turn a into b. */
    public static int editDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++)
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        return dp[a.length()][b.length()];
    }

    /** 1.0 = identical, 0.0 = totally different. Example: similarity("AB5", "A85") = 0.67 (one OCR slip). */
    public static double similarity(String a, String b) {
        String x = normalise(a), y = normalise(b);
        int longest = Math.max(x.length(), y.length());
        return longest == 0 ? 0.0 : 1.0 - (double) editDistance(x, y) / longest;
    }

    /** All medicines, best match first. */
    public static List<Candidate> rank(Observed o, List<Medicine> meds) {
        List<Candidate> out = new ArrayList<>();
        for (Medicine m : meds) {
            double score = 0.6 * similarity(o.imprint, m.imprint)
                    + 0.2 * (o.shape.equalsIgnoreCase(m.shape) ? 1 : 0)
                    + 0.2 * (o.colour.equalsIgnoreCase(m.colour) ? 1 : 0);
            out.add(new Candidate(m, score));
        }
        Collections.sort(out, (a, b) -> Double.compare(b.score, a.score));
        return out;
    }

    /** The best match, or null if not confident enough. */
    public static Candidate best(Observed o, List<Medicine> meds) {
        List<Candidate> r = rank(o, meds);
        return (!r.isEmpty() && r.get(0).score >= THRESHOLD) ? r.get(0) : null;
    }
}
