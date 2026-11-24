package com.wynter.pillvox.ml;

import android.graphics.Bitmap;
import com.wynter.pillvox.data.PillDao;
import com.wynter.pillvox.domain.PillMatcher;

/**
 * frame -> PillAnalyzer (detect, read imprint, quality/damage checks) -> PillMatcher (compare with the
 * local DB) -> ScanOutcome (safe result for the UI).
 * Example: ScanOutcome o = new ScanPipeline(new FakeAnalyzer(), dao).scan(null);   // background thread
 */
public class ScanPipeline {
    private final PillAnalyzer analyzer; private final PillDao dao;
    public ScanPipeline(PillAnalyzer analyzer, PillDao dao) { this.analyzer = analyzer; this.dao = dao; }

    public ScanOutcome scan(Bitmap frame) {
        PillAnalyzer.FrameAnalysis a = analyzer.analyse(frame);
        if (!a.pillFound) return ScanOutcome.notSure(Issue.OFF_CENTRE);
        if (a.issue == Issue.DAMAGED) return ScanOutcome.damaged();   // only "may be damaged", never "safe/unsafe"
        if (a.issue != Issue.NONE || a.observed == null) return ScanOutcome.notSure(a.issue);
        PillMatcher.Candidate best = PillMatcher.best(a.observed, dao.medicines());   // null = below threshold
        if (best == null) return ScanOutcome.notSure(Issue.IMPRINT_HIDDEN);
        return ScanOutcome.match(best.medicine, (int) (best.score * 100));
    }
}
