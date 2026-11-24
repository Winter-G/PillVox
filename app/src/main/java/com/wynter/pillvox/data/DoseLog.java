package com.wynter.pillvox.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/** One row each time a dose is CONFIRMED. A missed dose has no row: we never assume it was taken. */
@Entity(tableName = "dose_log")
public class DoseLog {
    @PrimaryKey(autoGenerate = true) public long id;
    public long scheduleId;
    public String day;       // "2026-11-21", lets us ask "was this dose taken TODAY?"
    public String status;    // "TAKEN"
    public long atMillis;

    public DoseLog() { }

    @Ignore
    public DoseLog(long scheduleId, String day, String status, long atMillis) {
        this.scheduleId = scheduleId; this.day = day; this.status = status; this.atMillis = atMillis;
    }
}