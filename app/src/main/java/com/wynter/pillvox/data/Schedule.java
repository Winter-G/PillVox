package com.wynter.pillvox.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/** "Take medicine X every day at hour:minute". Example: new Schedule(1, 13, 0) = medicine 1 at 13:00. */
@Entity(tableName = "schedules")
public class Schedule {
    @PrimaryKey(autoGenerate = true) public long id;
    public long medicineId;
    public int hour, minute;

    public Schedule() { }

    @Ignore
    public Schedule(long medicineId, int hour, int minute) {
        this.medicineId = medicineId; this.hour = hour; this.minute = minute;
    }
}
