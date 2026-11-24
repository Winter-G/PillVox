package com.wynter.pillvox.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

/**
 * DAO = the list of questions we can ask the database (Room writes the SQL plumbing for us).
 * IMPORTANT: Room refuses to run on the main (UI) thread, so always call these from a background
 * thread. MainActivity does this with an ExecutorService.
 */
@Dao
public interface PillDao {
    @Query("SELECT * FROM medicines") List<Medicine> medicines();
    @Query("SELECT * FROM medicines WHERE id = :id") Medicine medicine(long id);
    @Insert long addMedicine(Medicine m);                       // returns the new row id

    @Query("SELECT * FROM schedules") List<Schedule> allSchedules();
    @Query("SELECT * FROM schedules WHERE id = :id") Schedule schedule(long id);
    @Insert long addSchedule(Schedule s);

    @Query("SELECT * FROM dose_log WHERE day = :day") List<DoseLog> logsFor(String day);
    @Query("SELECT COUNT(*) FROM dose_log WHERE scheduleId = :sid AND day = :day AND status = 'TAKEN'")
    int takenCount(long sid, String day);
    @Insert void addLog(DoseLog log);
}

