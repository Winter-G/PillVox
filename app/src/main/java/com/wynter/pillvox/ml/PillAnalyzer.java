package com.wynter.pillvox.ml;

import android.graphics.Bitmap;
import com.wynter.pillvox.domain.Observed;

/**
 * The "eyes" of the app. An INTERFACE on purpose: today FakeAnalyzer plugs in so the whole app works.
 * Later you write TflitePillAnalyzer (YOLOv8n INT8 detector + text recognition + blur/brightness/damage
 * checks) and change ONE line in MainActivity. Nothing else in the app has to change.
 */
public interface PillAnalyzer {
    class FrameAnalysis {
        public final boolean pillFound; public final Issue issue; public final Observed observed;
        public FrameAnalysis(boolean pillFound, Issue issue, Observed observed) {
            this.pillFound = pillFound; this.issue = issue; this.observed = observed;
        }
    }

    /** frame may be null for the fake version. Always call from a background thread. */
    FrameAnalysis analyse(Bitmap frame);
}