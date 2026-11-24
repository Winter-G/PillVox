package com.wynter.pillvox.domain;

import com.wynter.pillvox.data.DoseLog;
import com.wynter.pillvox.data.PillDao;
import java.time.LocalTime;

/**
 * DOSE RULES: the safety logic of the reminder module, kept apart from the screens.
 *   1. A dose is only TAKEN if the user confirmed it. No answer = not taken.
 *   2. A second confirmation for the same dose on the same day is refused (duplicate block).
 */
public final class DoseRules {
    private DoseRules() { }

    public enum DoseStatus { PENDING, DUE, MISSED, TAKEN }
    public enum DoseResult { RECORDED, ALREADY_TAKEN }

    /**
     * Example: statusOf(13, 0, false, 13:20) -> DUE;  statusOf(13, 0, false, 15:00) -> MISSED
     * (shown as "unconfirmed", never as "taken").
     */
    public static DoseStatus statusOf(int hour, int minute, boolean taken, LocalTime now) {
        if (taken) return DoseStatus.TAKEN;
        int minutesLate = now.toSecondOfDay() / 60 - (hour * 60 + minute);
        if (minutesLate < 0) return DoseStatus.PENDING;   // not time yet
        if (minutesLate < 60) return DoseStatus.DUE;      // the window is open for one hour
        return DoseStatus.MISSED;                         // too late and still unconfirmed
    }

    /** Example: DoseResult r = DoseRules.markTaken(dao, scheduleId, LocalDate.now().toString()); (background thread!) */
    public static DoseResult markTaken(PillDao dao, long scheduleId, String day) {
        if (dao.takenCount(scheduleId, day) > 0) return DoseResult.ALREADY_TAKEN;   // duplicate-dose block
        dao.addLog(new DoseLog(scheduleId, day, "TAKEN", System.currentTimeMillis()));
        return DoseResult.RECORDED;
    }
}