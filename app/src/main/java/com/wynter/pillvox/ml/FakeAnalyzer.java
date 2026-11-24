package com.wynter.pillvox.ml;

import android.graphics.Bitmap;
import com.wynter.pillvox.domain.Observed;

/** Placeholder so you can test screens, voice and haptics today. Each scan returns the next scenario. */
public class FakeAnalyzer implements PillAnalyzer {
    private int step = 0;
    private final FrameAnalysis[] scenarios = {
            new FrameAnalysis(true, Issue.NONE, new Observed("AB 5", "round", "white")), // matches the demo medicine
            new FrameAnalysis(true, Issue.FINGER, null),
            new FrameAnalysis(true, Issue.IMPRINT_HIDDEN, null),
            new FrameAnalysis(true, Issue.DAMAGED, null)
    };

    @Override public FrameAnalysis analyse(Bitmap frame) { return scenarios[step++ % scenarios.length]; }
}
