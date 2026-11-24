package com.wynter.pillvox.ml;

import com.wynter.pillvox.data.Medicine;

/**
 * The only three things the UI can be told. There is deliberately NO "low-confidence guess" outcome,
 * so a guess can never be shown as a confirmed identification.
 */
public final class ScanOutcome {
    public enum Type { MATCH, NOT_SURE, DAMAGED }

    public final Type type;
    public final Medicine medicine;   // only for MATCH
    public final int confidence;      // percent, only for MATCH
    public final Issue issue;         // only for NOT_SURE

    private ScanOutcome(Type t, Medicine m, int c, Issue i) { type = t; medicine = m; confidence = c; issue = i; }

    public static ScanOutcome match(Medicine m, int pct) { return new ScanOutcome(Type.MATCH, m, pct, Issue.NONE); }
    public static ScanOutcome notSure(Issue i) { return new ScanOutcome(Type.NOT_SURE, null, 0, i); }
    public static ScanOutcome damaged() { return new ScanOutcome(Type.DAMAGED, null, 0, Issue.DAMAGED); }
}
